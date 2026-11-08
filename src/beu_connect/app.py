"""Read-only Beurer connection probe using the native Linux BlueZ D-Bus API."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import sys
import time

DEVICE = "org.bluez.Device1"
ADAPTER = "org.bluez.Adapter1"
SERVICE = "org.bluez.GattService1"
CHAR = "org.bluez.GattCharacteristic1"
PROPS = "org.freedesktop.DBus.Properties"


def uuid(short):
    return f"0000{short.lower()}-0000-1000-8000-00805f9b34fb"


READS = {
    "manufacturer": ("2a29", "text"),
    "model": ("2a24", "text"),
    "firmware": ("2a26", "text"),
    "battery_percent": ("2a19", "byte"),
    "weight_feature": ("2a9e", "hex"),
    "body_composition_feature": ("2a9b", "hex"),
}


class BlueZ:
    def __init__(self, adapter, timeout):
        try:
            import dbus
        except ImportError as exc:
            raise RuntimeError("Use /usr/bin/python3 with python3-dbus installed; see README.md") from exc
        self.dbus = dbus
        self.bus = dbus.SystemBus()
        self.adapter = f"/org/bluez/{adapter}"
        self.timeout = timeout
        self.owned_connection = None

    def iface(self, path, name):
        return self.dbus.Interface(self.bus.get_object("org.bluez", path), name)

    def objects(self):
        return self.iface("/", "org.freedesktop.DBus.ObjectManager").GetManagedObjects(timeout=self.timeout)

    def discover(self, seconds):
        objects = self.objects()
        if self.adapter not in objects:
            raise RuntimeError(f"Bluetooth adapter {self.adapter} is unavailable")
        if not objects[self.adapter][ADAPTER].get("Powered"):
            raise RuntimeError("Bluetooth is powered off. Enable it in system settings.")
        adapter = self.iface(self.adapter, ADAPTER)
        # StartDiscovery creates this client's own reference-counted discovery session.
        adapter.SetDiscoveryFilter(self.dbus.Dictionary({"Transport": "le"}, signature="sv"))
        adapter.StartDiscovery(timeout=self.timeout)
        try:
            time.sleep(seconds)
            objects = self.objects()
        finally:
            adapter.StopDiscovery(timeout=self.timeout)
        return [dict(path=str(path), address=str(props[DEVICE]["Address"]),
                     name=str(props[DEVICE].get("Name", props[DEVICE].get("Alias", ""))),
                     connected=bool(props[DEVICE].get("Connected", False)))
                for path, props in objects.items()
                if DEVICE in props and str(path).startswith(self.adapter + "/")]

    def connect(self, path):
        props = self.iface(path, PROPS)
        if not props.Get(DEVICE, "Connected"):
            self.owned_connection = path
            self.iface(path, DEVICE).Connect(timeout=self.timeout)
        deadline = time.monotonic() + self.timeout
        while time.monotonic() < deadline:
            if props.Get(DEVICE, "Connected") and props.Get(DEVICE, "ServicesResolved"):
                return
            time.sleep(0.2)
        raise TimeoutError("Connection did not resolve GATT services before timeout")

    def inventory(self, path):
        objects = self.objects()
        services = {str(p): str(v[SERVICE]["UUID"]).lower() for p, v in objects.items()
                    if SERVICE in v and str(v[SERVICE].get("Device", "")) == path}
        chars = {str(v[CHAR]["UUID"]).lower(): str(p) for p, v in objects.items()
                 if CHAR in v and str(v[CHAR].get("Service", "")) in services}
        return sorted(services.values()), chars

    def read(self, path):
        return bytes(self.iface(path, CHAR).ReadValue(
            self.dbus.Dictionary({}, signature="sv"), timeout=self.timeout))

    def connected(self, path):
        props = self.iface(path, PROPS)
        return bool(props.Get(DEVICE, "Connected") and props.Get(DEVICE, "ServicesResolved"))

    def close(self):
        if self.owned_connection:
            self.iface(self.owned_connection, DEVICE).Disconnect(timeout=self.timeout)


def select_device(devices, address=None, name=None):
    if address:
        candidates = [d for d in devices if d["address"].upper() == address.upper()]
    elif name:
        candidates = [d for d in devices if d["name"].casefold() == name.casefold()]
    else:
        candidates = [d for d in devices if re.fullmatch(r"BF[0-9]+", d["name"], re.I)]
    if not candidates:
        raise RuntimeError("No matching scale found. Wake it, close the phone app, and retry. Cached devices may be asleep.")
    if len(candidates) != 1:
        raise RuntimeError("Multiple scales found; choose one explicitly with --address or --name")
    return candidates[0]


def probe(backend, device, report):
    report["device"] = {k: device[k] for k in ("name", "address")}
    backend.connect(device["path"])
    report["gatt_connected"] = True
    services, chars = backend.inventory(device["path"])
    report["services"] = services
    report["characteristics"] = sorted(chars)
    reads, errors = {}, {}
    report["reads"], report["read_errors"] = reads, errors
    for label, (short, kind) in READS.items():
        if uuid(short) not in chars:
            errors[label] = "Characteristic absent"
            continue
        try:
            raw = backend.read(chars[uuid(short)])
            if not raw:
                raise ValueError("Empty response")
            if kind == "byte" and (len(raw) != 1 or raw[0] > 100):
                raise ValueError("Invalid battery level")
            if kind == "hex" and len(raw) != 4:
                raise ValueError("Feature value must contain four bytes")
            reads[label] = {"raw_hex": raw.hex(" "), "value":
                raw.decode("utf-8").rstrip("\x00") if kind == "text" else
                raw[0] if kind == "byte" else raw.hex(" ")}
        except Exception as exc:
            errors[label] = str(exc)
    report["connection_proven"] = bool(reads) and backend.connected(device["path"])
    report["weight_service_verified"] = uuid("181d") in services and "weight_feature" in reads
    report["body_composition_service_verified"] = uuid("181b") in services and "body_composition_feature" in reads
    report["scale_profile_verified"] = (report["connection_proven"] and
        report["weight_service_verified"] and report["body_composition_service_verified"])
    report["measurements_downloaded"] = False
    report["verification_scope"] = "Live GATT reads only; no measurement download or proprietary command verification"


def positive(value):
    number = float(value)
    if not 0 < number <= 300:
        raise argparse.ArgumentTypeError("Choose a duration greater than 0 and at most 300 seconds")
    return number


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    selector = parser.add_mutually_exclusive_group()
    selector.add_argument("--address", help="Explicit Bluetooth address")
    selector.add_argument("--name", help="Exact advertised name, e.g. BF600 or BF990")
    parser.add_argument("--adapter", default="hci0")
    parser.add_argument("--scan-seconds", type=positive, default=10)
    parser.add_argument("--timeout", type=positive, default=25)
    parser.add_argument("--scan-only", action="store_true")
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parents[2] / "database/connection-latest.json")
    args = parser.parse_args(argv)
    report = {"started_at": datetime.now(timezone.utc).isoformat(),
              "connection_proven": False, "scale_profile_verified": False}
    backend = None
    code = 1
    try:
        backend = BlueZ(args.adapter, args.timeout)
        print("Scanning Bluetooth LE devices...", flush=True)
        devices = backend.discover(args.scan_seconds)
        # Do not persist unrelated nearby device identities.
        for d in devices:
            if re.fullmatch(r"BF[0-9]+", d["name"], re.I):
                print(f"Scale candidate: {d['name']} ({d['address']}); cached connection: {d['connected']}")
        if args.scan_only:
            report["scan_only"] = True
            code = 0
        else:
            device = select_device(devices, args.address, args.name)
            print(f"Reading {device['name']}...", flush=True)
            probe(backend, device, report)
            code = 0 if report["scale_profile_verified"] else 2
            print("CONNECTION PROVEN" if report["connection_proven"] else "CONNECTION NOT PROVEN")
            for label, result in report["reads"].items():
                print(f"  {label}: {result['value']}")
    except KeyboardInterrupt:
        report["error"] = "Interrupted"
        code = 130
    except Exception as exc:
        report["error"] = str(exc)
        print(f"Probe failed: {exc}", file=sys.stderr)
    finally:
        if backend:
            try:
                backend.close()
            except Exception as exc:
                report["cleanup_error"] = str(exc)
                code = code or 1
        report["finished_at"] = datetime.now(timezone.utc).isoformat()
        try:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
            print(f"Report: {args.output}")
        except OSError as exc:
            print(f"Cannot save report: {exc}", file=sys.stderr)
            code = code or 1
    return code
