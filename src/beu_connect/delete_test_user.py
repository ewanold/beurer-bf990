"""Delete only the configured disposable user 2 and reauthorize user 1."""
import json
from datetime import datetime, timezone
from pathlib import Path
import sys

from .app import BlueZ, select_device
from .setup_profile import ROOT, Session, check_state, load_profile, profile_payloads, save_state


def delete_and_restore(session, original, test, profile, persist):
    if original.get("phase") != "configured" or original.get("user_index") != 1:
        raise RuntimeError("Original user 1 state is not configured")
    if test.get("phase") != "configured" or test.get("user_index") != 2:
        raise RuntimeError("Disposable user 2 state is not configured, or deletion outcome is unresolved")
    def select(state):
        session.control(2, bytes([state["user_index"]]) + state["consent_code"].to_bytes(2, "little"))
        if session.read("181c", "2a9a") != bytes([state["user_index"]]):
            raise RuntimeError("Selected user index did not match")
    select(original)
    print("Original user 1 authorization verified.", flush=True)
    select(test)
    for short, expected in profile_payloads(profile, 2).items():
        if session.read("181c", short) != expected:
            raise RuntimeError("Disposable profile readback mismatch; refusing deletion")
    if session.read("181c", "2a9a") != b"\x02":
        raise RuntimeError("Disposable user no longer selected; refusing deletion")
    test["phase"] = "deletion_pending"
    persist(test)
    try:
        session.control(3)
        test["phase"] = "deletion_acknowledged"
        persist(test)
        if session.read("181c", "2a9a") != b"\xff":
            raise RuntimeError("Deletion acknowledged but selected index did not clear")
        print("Deletion acknowledged and selection cleared; durable deletion still requires verification.", flush=True)
    finally:
        select(original)
        print("Original user 1 authorization restored.", flush=True)


def main():
    original_path = ROOT / "database/profile-state.json"
    test_path = ROOT / "database/test-user-2-state.json"
    original_profile = ROOT / "database/profile-input.json"
    test_profile = ROOT / "database/test-user-2-profile.json"
    locks = [original_path.with_name(original_path.name + ".lock"), test_path.with_name(test_path.name + ".lock")]
    acquired = []
    backend = session = None
    try:
        for lock in locks:
            lock.mkdir()
            acquired.append(lock)
        original = json.loads(original_path.read_text())
        test = json.loads(test_path.read_text())
        profile = load_profile(test_profile)
        check_state(original, original["address"], load_profile(original_profile))
        check_state(test, original["address"], profile)
        if original.get("phase") != "configured" or original.get("user_index") != 1 or test.get("phase") != "configured" or test.get("user_index") != 2:
            raise RuntimeError("Unexpected saved state; no Bluetooth operation made")
        from dbus.mainloop.glib import DBusGMainLoop
        DBusGMainLoop(set_as_default=True)
        backend = BlueZ("hci0", 35)
        print("Connecting for disposable-user deletion; please wake the scale...", flush=True)
        device = select_device(backend.discover(3), name="BF990")
        check_state(original, device["address"], load_profile(original_profile))
        check_state(test, device["address"], profile)
        backend.connect(device["path"])
        target = ROOT / "database/captures" / datetime.now(timezone.utc).strftime("delete-test-%Y%m%dT%H%M%S-%fZ.jsonl")
        with target.open("x") as output:
            def emit(event, **fields):
                output.write(json.dumps({"event": event, "at": datetime.now(timezone.utc).isoformat(), **fields}) + "\n")
                output.flush()
            session = Session(backend, device, emit)
            if session.read("180a", "2a24").rstrip(b"\x00") != b"BF990":
                raise RuntimeError("Live model verification failed")
            session.subscribe("181c", "2a9f")
            delete_and_restore(session, original, test, profile, lambda s: save_state(test_path, s))
            print("Private deletion log:", target, flush=True)
        return 0
    except Exception as exc:
        print(f"Deletion stopped: {exc}", file=sys.stderr, flush=True)
        return 1
    finally:
        if session:
            session.close()
        if backend:
            backend.close()
        for lock in reversed(acquired):
            lock.rmdir()

if __name__ == "__main__":
    raise SystemExit(main())
