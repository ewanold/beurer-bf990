"""Create two disposable BF990 profiles and verify known users individually.

This does not claim to enumerate scale users. A registration outcome is saved
before retries; the scale chooses each index, and unexpected assignments stop.
"""
import json
import secrets
import sys
from datetime import datetime, timezone

from .app import BlueZ, select_device
from .setup_profile import ROOT, Session, check_state, configure, fingerprint, load_profile, profile_payloads, save_state

PROFILES = {
    2: {"date_of_birth": "1991-02-03", "height_cm": 170, "sex": "female"},
    3: {"date_of_birth": "1992-03-04", "height_cm": 180, "sex": "male"},
}


def verify_user(session, state, profile, index):
    if state.get("phase") != "configured" or state.get("user_index") != index:
        raise RuntimeError(f"Saved state for user {index} is not configured as expected")
    session.control(2, bytes([index]) + state["consent_code"].to_bytes(2, "little"))
    if session.read("181c", "2a9a") != bytes([index]):
        raise RuntimeError(f"Selected user {index} did not match")
    height = session.read("181c", "2a8e")
    expected = profile_payloads(profile, len(height))
    for field, value in expected.items():
        actual = height if field == "2a8e" else session.read("181c", field)
        if actual != value:
            raise RuntimeError(f"Profile readback mismatch for user {index}, field {field}")
    print(f"User {index}: authorization, index and profile readback verified.", flush=True)


def main():
    database = ROOT / "database"
    original_path = database / "profile-state.json"
    paths = {i: database / f"multi-user-test-{i}-state.json" for i in PROFILES}
    locks = [p.with_name(p.name + ".lock") for p in [original_path, *paths.values()]]
    acquired = []
    try:
        for lock in locks:
            lock.mkdir()
            acquired.append(lock)
        original = json.loads(original_path.read_text())
        original_profile = load_profile(database / "profile-input.json")
        check_state(original, original["address"], original_profile)
        if original.get("phase") != "configured" or original.get("user_index") != 1:
            raise RuntimeError("Original user 1 state is not configured")
        states = {}
        for index, profile in PROFILES.items():
            path = paths[index]
            if path.exists():
                state = json.loads(path.read_text())
                check_state(state, original["address"], profile)
                if state.get("user_index", index) != index:
                    raise RuntimeError(f"User {index} has an unexpected assigned index; inspect saved state")
                states[index] = state
            else:
                states[index] = {"schema": 1, "address": original["address"], "phase": "prepared",
                                 "consent_code": secrets.randbelow(10000), "profile_fingerprint": fingerprint(profile)}
                save_state(path, states[index])
        from dbus.mainloop.glib import DBusGMainLoop
        DBusGMainLoop(set_as_default=True)
        capture = database / "captures" / datetime.now(timezone.utc).strftime("multi-user-test-%Y%m%dT%H%M%S-%fZ.jsonl")
        capture.parent.mkdir(parents=True, exist_ok=True)
        with capture.open("x") as output:
            def emit(event, **fields):
                output.write(json.dumps({"event": event, "at": datetime.now(timezone.utc).isoformat(), **fields}) + "\n")
                output.flush()
            for stage in (2, 3, "verify"):
                backend = BlueZ("hci0", 35)
                session = None
                try:
                    print(f"Connecting for stage {stage}; keep the scale awake.", flush=True)
                    device = select_device(backend.discover(3), name="BF990")
                    if device["connected"]:
                        raise RuntimeError("Scale already connected elsewhere; close the other connection and retry")
                    check_state(original, device["address"], original_profile)
                    backend.connect(device["path"])
                    session = Session(backend, device, emit)
                    if session.read("180a", "2a24").rstrip(b"\x00") != b"BF990":
                        raise RuntimeError("Live model verification failed")
                    session.subscribe("181c", "2a9f")
                    if stage == "verify":
                        for index in (2, 3):
                            verify_user(session, states[index], PROFILES[index], index)
                        verify_user(session, original, original_profile, 1)
                        print("Original user 1 selected again. No user-list command was issued.", flush=True)
                    else:
                        configure(session, PROFILES[stage], states[stage],
                                  lambda state, path=paths[stage]: save_state(path, state), emit, expected_index=stage)
                        verify_user(session, states[stage], PROFILES[stage], stage)
                finally:
                    if session:
                        session.close()
                    backend.close()
        print("Test complete. Profiles 2 and 3 remain on the scale. Private log:", capture, flush=True)
        return 0
    except Exception as exc:
        print(f"Test stopped: {exc}. Preserve saved test states; do not create replacements blindly.", file=sys.stderr, flush=True)
        return 1
    finally:
        for lock in reversed(acquired):
            lock.rmdir()


if __name__ == "__main__":
    raise SystemExit(main())
