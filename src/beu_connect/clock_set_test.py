"""One explicitly requested Current Time write and immediate readback."""
from datetime import datetime, timezone
import json
import os
import sys
from .app import BlueZ, CHAR, select_device, uuid
from .capture import inventory
from .setup_profile import ROOT


def encode_local(value):
    return value.year.to_bytes(2, "little") + bytes((value.month, value.day, value.hour, value.minute, value.second, value.isoweekday(), 0, 1))


def parse_time(raw):
    if len(raw) != 10:
        raise ValueError("Unexpected Current Time length")
    return datetime(int.from_bytes(raw[:2], "little"), *raw[2:7])


def main():
    backend = None
    target = ROOT / "database/captures" / datetime.now(timezone.utc).strftime("clock-set-%Y%m%dT%H%M%S-%fZ.jsonl")
    try:
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("x") as output:
            def record(event, **data):
                output.write(json.dumps({"event": event, "at": datetime.now(timezone.utc).isoformat(), **data}) + "\n")
                output.flush()
                os.fsync(output.fileno())
            backend = BlueZ("hci0", 35)
            print("Connecting to test clock correction; please wake the scale...", flush=True)
            device = select_device(backend.discover(3), name="BF990")
            backend.connect(device["path"])
            chars = {(c["service_uuid"], c["uuid"]): c for c in inventory(backend.objects(), device["path"])}
            if backend.read(chars[(uuid("180a"), uuid("2a24"))]["path"]).rstrip(b"\0") != b"BF990":
                raise RuntimeError("Live model verification failed")
            clock = chars[(uuid("1805"), uuid("2a2b"))]
            if not {"read", "write"}.issubset(set(clock["flags"])):
                raise RuntimeError("Clock does not support read and acknowledged write")
            before = backend.read(clock["path"])
            old_time = parse_time(before)
            host = datetime.now().astimezone()
            record("before", raw_hex=before.hex(" "), host_local=host.isoformat(), difference_seconds=(old_time-host.replace(tzinfo=None)).total_seconds())
            now = datetime.now().astimezone()
            payload = encode_local(now)
            record("write_pending", raw_hex=payload.hex(" "), host_local=now.isoformat())
            print("Writing local time once; the previous clock value is saved.", flush=True)
            d = backend.dbus
            backend.iface(clock["path"], CHAR).WriteValue(d.Array(payload, signature="y"), d.Dictionary({"type": "request"}, signature="sv"), timeout=backend.timeout)
            record("write_returned")
            raw = backend.read(clock["path"])
            actual = parse_time(raw)
            host = datetime.now().astimezone()
            delta = (actual - host.replace(tzinfo=None)).total_seconds()
            record("readback", raw_hex=raw.hex(" "), host_local=host.isoformat(), difference_seconds=delta)
            print("Readback difference from local time:", round(delta, 1), "seconds.", flush=True)
            print("Private report:", target, flush=True)
            if abs(delta) > 5: raise RuntimeError("Clock readback differs by more than 5 seconds; inspect before retry")
        return 0
    except Exception as exc:
        print("Clock test stopped:", exc, file=sys.stderr, flush=True)
        print("Private report:", target, flush=True)
        return 1
    finally:
        if backend: backend.close()

if __name__ == "__main__": raise SystemExit(main())
