"""Check whether deleted disposable credentials still authorize after reconnect."""
import json
import sys
from .app import BlueZ, select_device
from .setup_profile import ROOT, Session, check_state, load_profile


def main():
    original_path = ROOT / "database/profile-state.json"
    test_path = ROOT / "database/test-user-2-state.json"
    locks = [p.with_name(p.name + ".lock") for p in (original_path, test_path)]
    acquired = []
    backend = session = None
    try:
        for lock in locks:
            lock.mkdir(); acquired.append(lock)
        original, test = (json.loads(p.read_text()) for p in (original_path, test_path))
        if original.get("phase") != "configured" or original.get("user_index") != 1 or test.get("phase") != "deletion_acknowledged" or test.get("user_index") != 2:
            raise RuntimeError("Unexpected saved user states")
        from dbus.mainloop.glib import DBusGMainLoop
        DBusGMainLoop(set_as_default=True)
        backend = BlueZ("hci0", 35)
        print("Connecting to verify deleted credentials; please wake the scale...", flush=True)
        device = select_device(backend.discover(3), name="BF990")
        check_state(original, device["address"], load_profile(ROOT / "database/profile-input.json"))
        check_state(test, device["address"], load_profile(ROOT / "database/test-user-2-profile.json"))
        backend.connect(device["path"])
        session = Session(backend, device, lambda *args, **kwargs: None)
        if session.read("180a", "2a24").rstrip(b"\x00") != b"BF990": raise RuntimeError("Live model verification failed")
        session.subscribe("181c", "2a9f")
        try:
            try:
                session.control(2, bytes([2]) + test["consent_code"].to_bytes(2, "little"))
            except RuntimeError:
                if not session.responses or session.responses[-1][:3] != b"\x20\x02\x05": raise
                print("Deleted user 2 credentials rejected as unauthorized after reconnect.", flush=True)
            else:
                raise RuntimeError("Deleted user 2 credentials still authorize; deletion not verified")
        finally:
            session.control(2, bytes([1]) + original["consent_code"].to_bytes(2, "little"))
            if session.read("181c", "2a9a") != b"\x01": raise RuntimeError("Could not verify original user selection")
            print("Original user 1 authorization restored and read back.", flush=True)
        return 0
    except Exception as exc:
        print(f"Verification stopped: {exc}", file=sys.stderr, flush=True); return 1
    finally:
        if session: session.close()
        if backend: backend.close()
        for lock in reversed(acquired): lock.rmdir()

if __name__ == "__main__": raise SystemExit(main())
