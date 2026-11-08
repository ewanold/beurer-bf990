"""Capture scale GATT notifications locally without proprietary command writes."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import sys
import time

from .app import BlueZ, CHAR, DEVICE, PROPS, SERVICE, positive, select_device


def inventory(objects, device_path):
    services = {str(path): str(props[SERVICE]["UUID"])
                for path, props in objects.items()
                if SERVICE in props and str(props[SERVICE].get("Device", "")) == device_path}
    result = []
    for path, props in objects.items():
        if CHAR not in props:
            continue
        c = props[CHAR]
        service_path = str(c.get("Service", ""))
        if service_path in services:
            result.append({"path": str(path), "uuid": str(c["UUID"]),
                           "service_path": service_path, "service_uuid": services[service_path],
                           "flags": [str(flag) for flag in c.get("Flags", [])],
                           "descriptors": [
                               {"path": str(dp), "uuid": str(d["org.bluez.GattDescriptor1"]["UUID"])}
                               for dp, d in objects.items()
                               if "org.bluez.GattDescriptor1" in d and
                               str(d["org.bluez.GattDescriptor1"].get("Characteristic", "")) == str(path)]})
    return result


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--name", default="BF990")
    parser.add_argument("--seconds", type=positive, default=60)
    parser.add_argument("--timeout", type=positive, default=20)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args(argv)
    from dbus.mainloop.glib import DBusGMainLoop
    from gi.repository import GLib
    DBusGMainLoop(set_as_default=True)
    target = args.output or Path(__file__).resolve().parents[2] / "database/captures" / (
        datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S-%fZ") + ".jsonl")
    target.parent.mkdir(parents=True, exist_ok=True)
    backend = None
    subscriptions = []
    counts = {}
    code = 0
    started = time.monotonic()
    with target.open("x", encoding="utf-8") as output:
        def emit(kind, **fields):
            record = {"event": kind, "at": datetime.now(timezone.utc).isoformat(),
                      "elapsed_seconds": round(time.monotonic() - started, 3), **fields}
            output.write(json.dumps(record) + "\n")
            output.flush()

        emit("capture_started", requested_seconds=args.seconds,
             scope="Local GATT subscriptions only; no proprietary writes or cloud upload")
        try:
            backend = BlueZ("hci0", args.timeout)
            print("Looking for the scale; please wake it now...", flush=True)
            device = select_device(backend.discover(3), name=args.name)
            emit("device_selected", name=device["name"], address=device["address"])
            backend.connect(device["path"])
            chars = inventory(backend.objects(), device["path"])
            by_path = {c["path"]: c for c in chars}
            emit("gatt_inventory", characteristics=chars)
            loop = GLib.MainLoop()

            def changed(interface, values, invalidated, path=None):
                path = str(path)
                if str(interface) == CHAR and "Value" in values and path in by_path:
                    c = by_path[path]
                    raw = bytes(values["Value"])
                    counts[path] = counts.get(path, 0) + 1
                    emit("value_changed", characteristic_uuid=c["uuid"],
                         service_uuid=c["service_uuid"], path=path,
                         raw_hex=raw.hex(" "), length=len(raw))
                    print(f"Packet {sum(counts.values())}: {c['uuid'][:8]}, {len(raw)} bytes", flush=True)
                elif str(interface) == DEVICE and path == device["path"] and "Connected" in values:
                    connected = bool(values["Connected"])
                    emit("connection_changed", connected=connected)
                    if not connected:
                        print("Scale disconnected; ending this capture.", flush=True)
                        loop.quit()

            receiver = backend.bus.add_signal_receiver(
                changed, dbus_interface=PROPS, signal_name="PropertiesChanged",
                bus_name="org.bluez", path_keyword="path")
            try:
                for c in chars:
                    if not set(c["flags"]) & {"notify", "indicate"}:
                        continue
                    try:
                        backend.iface(c["path"], CHAR).StartNotify(timeout=args.timeout)
                        subscriptions.append(c["path"])
                        emit("subscribed", **c)
                    except Exception as exc:
                        emit("subscription_error", path=c["path"], uuid=c["uuid"], error=str(exc))
                        print(f"Subscription failed for {c['uuid'][:8]}: {exc}", flush=True)
                if not subscriptions:
                    raise RuntimeError("No subscriptions succeeded")
                emit("listening", seconds=args.seconds, subscriptions=len(subscriptions))
                print(f"LISTENING for {args.seconds:g} seconds on {len(subscriptions)} characteristics. Start your measurement now.", flush=True)
                def finish():
                    loop.quit()
                    return False
                GLib.timeout_add(max(1, int(args.seconds * 1000)), finish)
                loop.run()
            finally:
                receiver.remove()
        except KeyboardInterrupt:
            emit("interrupted")
            code = 130
        except Exception as exc:
            emit("error", message=str(exc))
            print(f"Capture failed: {exc}", file=sys.stderr, flush=True)
            code = 1
        finally:
            if backend:
                for path in subscriptions:
                    try:
                        backend.iface(path, CHAR).StopNotify(timeout=5)
                    except Exception as exc:
                        emit("cleanup_warning", path=path, error=str(exc))
                try:
                    backend.close()
                except Exception as exc:
                    emit("cleanup_warning", error=str(exc))
            emit("capture_finished", packet_count=sum(counts.values()),
                 characteristics_with_packets=len(counts), exit_code=code)
    print(f"Capture saved locally: {target}; packets: {sum(counts.values())}", flush=True)
    return code


if __name__ == "__main__":
    raise SystemExit(main())
