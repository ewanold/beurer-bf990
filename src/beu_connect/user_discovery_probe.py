"""Read user-data fields before and after known-user authorization on BF990."""
import json
import sys
from datetime import datetime, timezone
from .app import BlueZ, select_device, uuid
from .setup_profile import ROOT, Session, check_state, load_profile
from .multi_user_test import PROFILES, verify_user


def probe_fields(session, emit, phase):
    for field in ("2a9a", "2a85", "2a8e", "2a8c"):
        try:
            raw = session.read("181c", field)
            emit("field_read", phase=phase, characteristic=field, raw_hex=raw.hex())
            print(f"{phase}: {field} read succeeded ({len(raw)} bytes).", flush=True)
        except Exception as exc:
            emit("field_read_failed", phase=phase, characteristic=field, error=type(exc).__name__)
            print(f"{phase}: {field} read failed ({type(exc).__name__}).", flush=True)


def main():
    root = ROOT / "database"
    paths = [root / "profile-state.json", root / "multi-user-test-2-state.json"]
    locks = [p.with_name(p.name + ".lock") for p in paths]
    acquired=[]; backend=session=None
    try:
        for lock in locks:
            lock.mkdir(); acquired.append(lock)
        original, test = [json.loads(p.read_text()) for p in paths]
        original_profile=load_profile(root / "profile-input.json")
        check_state(original, original["address"], original_profile)
        check_state(test, original["address"], PROFILES[2])
        if original.get("phase") != "configured" or original.get("user_index") != 1 or test.get("phase") != "configured" or test.get("user_index") != 2:
            raise RuntimeError("Saved original or disposable test user is not configured")
        from dbus.mainloop.glib import DBusGMainLoop
        DBusGMainLoop(set_as_default=True)
        backend=BlueZ("hci0", 15)
        print("Connecting for read-only user-data probe; keep the scale awake.",flush=True)
        device=select_device(backend.discover(3),name="BF990")
        if device["connected"]: raise RuntimeError("Scale is already connected elsewhere")
        check_state(original, device["address"], original_profile)
        backend.connect(device["path"])
        path=root / "captures" / datetime.now(timezone.utc).strftime("user-discovery-%Y%m%dT%H%M%S-%fZ.jsonl")
        path.parent.mkdir(parents=True,exist_ok=True)
        with path.open("x") as output:
            def emit(event,**fields):
                output.write(json.dumps({"event":event,"at":datetime.now(timezone.utc).isoformat(),**fields})+"\n");output.flush()
            session=Session(backend,device,emit)
            if session.read("180a","2a24").rstrip(b"\x00") != b"BF990": raise RuntimeError("Live model verification failed")
            chars=[{"uuid":c["uuid"],"flags":c["flags"]} for c in session.chars.values() if c["service_uuid"]==uuid("181c")]
            emit("user_service_inventory",characteristics=chars)
            for c in chars: print(f"User characteristic {c['uuid'][:8]}: {', '.join(c['flags'])}",flush=True)
            probe_fields(session,emit,"before authorization")
            session.subscribe("181c","2a9f")
            try:
                verify_user(session,test,PROFILES[2],2)
                probe_fields(session,emit,"user 2 authorized")
            finally:
                verify_user(session,original,original_profile,1)
                print("Original user 1 selected again.",flush=True)
        print("Probe complete. Private capture:",path,flush=True)
        return 0
    except Exception as exc:
        print(f"Probe stopped: {exc}",file=sys.stderr,flush=True)
        return 1
    finally:
        if session: session.close()
        if backend: backend.close()
        for lock in reversed(acquired): lock.rmdir()

if __name__ == "__main__": raise SystemExit(main())
