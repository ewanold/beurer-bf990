"""Read BF990 current time without changing the scale."""
from datetime import datetime, timezone
import json
import sys
from .app import BlueZ, select_device, uuid
from .capture import inventory
from .setup_profile import ROOT


def main():
    backend = None
    try:
        backend = BlueZ("hci0", 35)
        print("Connecting for a read-only clock check; please wake the scale...", flush=True)
        device = select_device(backend.discover(3), name="BF990")
        backend.connect(device["path"])
        chars = {(c["service_uuid"], c["uuid"]): c for c in inventory(backend.objects(), device["path"])}
        model = chars[(uuid("180a"), uuid("2a24"))]
        if backend.read(model["path"]).rstrip(b"\0") != b"BF990": raise RuntimeError("Live model verification failed")
        char = chars.get((uuid("1805"), uuid("2a2b")))
        if not char: raise RuntimeError("Current Time characteristic absent")
        print("Current Time properties:", ", ".join(char["flags"]), flush=True)
        if "read" not in char["flags"]: raise RuntimeError("Current Time is not readable")
        before = datetime.now().astimezone()
        raw = backend.read(char["path"])
        after = datetime.now().astimezone()
        report = {"at_utc": datetime.now(timezone.utc).isoformat(), "flags": list(char["flags"]), "raw_hex": raw.hex(" "), "host_local_before": before.isoformat(), "host_local_after": after.isoformat()}
        if len(raw) == 10:
            try:
                value = datetime(int.from_bytes(raw[:2], "little"), *raw[2:7])
                delta = (value - after.replace(tzinfo=None)).total_seconds()
                report.update(clock_local=value.isoformat(), difference_seconds=delta, weekday=raw[7], fraction=raw[8], adjustment_reason=raw[9])
                print("Clock value:", value.isoformat(sep=" "), flush=True)
                print("Scale minus host local time (seconds):", round(delta, 1), flush=True)
            except ValueError:
                print("Clock bytes do not encode a valid complete date/time.", flush=True)
        else: print("Unexpected clock length:", len(raw), flush=True)
        target = ROOT / "database/captures" / datetime.now(timezone.utc).strftime("clock-%Y%m%dT%H%M%S-%fZ.json")
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("x") as f: json.dump(report, f, indent=2); f.write("\n")
        print("Private clock report:", target, flush=True)
        return 0
    except Exception as exc:
        print("Clock check failed:", exc, file=sys.stderr, flush=True); return 1
    finally:
        if backend: backend.close()

if __name__ == "__main__": raise SystemExit(main())
