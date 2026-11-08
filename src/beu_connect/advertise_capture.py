"""Record BlueZ-observed BF990 LE advertising properties without connecting."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import sys
import time

from .app import ADAPTER, DEVICE, PROPS, BlueZ, positive


def safe_bytes(value):
    if isinstance(value, dict):
        return {str(k): safe_bytes(v) for k, v in value.items()}
    if isinstance(value, (bytes, bytearray, list, tuple)):
        try:
            return bytes(value).hex(" ")
        except (TypeError, ValueError):
            return [safe_bytes(v) for v in value]
    return str(value)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--name", default="BF990")
    parser.add_argument("--seconds", type=positive, default=60)
    parser.add_argument("--timeout", type=positive, default=20)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args(argv)
    target = args.output or Path(__file__).resolve().parents[2] / "database/captures" / (
        datetime.now(timezone.utc).strftime("advertising-%Y%m%dT%H%M%S-%fZ.jsonl"))
    target.parent.mkdir(parents=True, exist_ok=True)
    from dbus.mainloop.glib import DBusGMainLoop
    DBusGMainLoop(set_as_default=True)
    backend = BlueZ("hci0", args.timeout)
    adapter = backend.iface(backend.adapter, ADAPTER)
    started = time.monotonic()
    count = 0
    with target.open("x", encoding="utf-8") as output:
        def emit(event, **fields):
            output.write(json.dumps({"event": event, "at": datetime.now(timezone.utc).isoformat(),
                "elapsed_seconds": round(time.monotonic() - started, 3), **fields}) + "\n")
            output.flush()

        def record(path, properties):
            nonlocal count
            if properties.get("Name", properties.get("Alias", "")).casefold() != args.name.casefold():
                return
            entry = {key: safe_bytes(properties[key]) for key in
                     ("ServiceData", "ManufacturerData", "UUIDs", "RSSI", "TxPower")
                     if key in properties}
            if not entry:
                return
            entry["path"] = str(path)
            count += 1
            emit("advertisement_properties", **entry)

        try:
            objects = backend.objects()
            if backend.adapter not in objects or not objects[backend.adapter][ADAPTER].get("Powered"):
                raise RuntimeError("Bluetooth adapter is unavailable or powered off")
            for path, props in objects.items():
                if DEVICE in props and str(path).startswith(backend.adapter + "/"):
                    record(path, props[DEVICE])
            receiver = backend.bus.add_signal_receiver(
                lambda interface, changed, invalidated, path=None:
                    record(path, changed) if str(interface) == DEVICE else None,
                dbus_interface=PROPS, signal_name="PropertiesChanged",
                bus_name="org.bluez", path_keyword="path")
            adapter.SetDiscoveryFilter(backend.dbus.Dictionary({"Transport": "le"}, signature="sv"))
            adapter.StartDiscovery(timeout=args.timeout)
            emit("scan_started", name=args.name, seconds=args.seconds,
                 scope="BlueZ advertising properties only; no GATT connection or writes")
            print(f"SCANNING for {args.seconds:g} seconds; no connection or device writes.", flush=True)
            from gi.repository import GLib
            loop = GLib.MainLoop()
            GLib.timeout_add(max(1, int(args.seconds * 1000)), lambda: (loop.quit(), False)[1])
            loop.run()
            receiver.remove()
            emit("scan_finished", property_events=count)
        except KeyboardInterrupt:
            emit("interrupted", property_events=count)
            return 130
        except Exception as exc:
            emit("error", message=str(exc))
            print(f"Advertising scan failed: {exc}", file=sys.stderr, flush=True)
            return 1
        finally:
            try:
                adapter.StopDiscovery(timeout=args.timeout)
            except Exception:
                pass
    print(f"Advertising capture saved locally: {target}; property events: {count}", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
