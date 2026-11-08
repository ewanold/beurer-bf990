"""Create and configure one local Beurer user through standard Bluetooth UDS."""
import argparse
from datetime import date, datetime, timezone
import hashlib
import json
from pathlib import Path
import secrets
import sys
import time

from .app import BlueZ, CHAR, DEVICE, PROPS, positive, select_device, uuid
from .capture import inventory

ROOT = Path(__file__).resolve().parents[2]


def load_profile(path):
    try:
        profile = json.loads(path.read_text())
        born = date.fromisoformat(profile["date_of_birth"])
        height = profile["height_cm"]
        sex = profile["sex"]
        if not date(1900, 1, 1) <= born < date.today():
            raise ValueError()
        if type(height) not in (int, float) or not 50 <= height <= 250 or int(height) != height:
            raise ValueError()
        if sex not in ("male", "female"):
            raise ValueError()
        return {"date_of_birth": born.isoformat(), "height_cm": int(height), "sex": sex}
    except (ValueError, TypeError, KeyError) as exc:
        raise ValueError("Invalid profile: use an actual ISO birth date, whole-centimetre height (50-250), and male/female") from None


def profile_payloads(profile, height_length):
    born = date.fromisoformat(profile["date_of_birth"])
    if height_length not in (1, 2):
        raise ValueError("Unknown Height characteristic length; refusing to guess encoding")
    return {
        "2a85": born.year.to_bytes(2, "little") + bytes([born.month, born.day]),
        "2a8c": bytes([0 if profile["sex"] == "male" else 1]),
        "2a8e": profile["height_cm"].to_bytes(height_length, "little"),
    }


def fingerprint(profile):
    return hashlib.sha256(json.dumps(profile, sort_keys=True).encode()).hexdigest()


def check_state(state, address, profile):
    if state["address"].upper() != address.upper():
        raise RuntimeError("Saved user belongs to another Bluetooth device")
    if state["profile_fingerprint"] != fingerprint(profile):
        raise RuntimeError("Profile differs from saved setup state; inspect before changing an existing user")
    if state["phase"] != "prepared" and "user_index" not in state:
        raise RuntimeError("Previous registration has an unresolved outcome; refusing another new-user request")


def response_value(raw, opcode):
    if len(raw) < 3 or raw[:2] != bytes([0x20, opcode]):
        raise ValueError("Malformed or mismatched UDS response")
    if raw[2] != 1:
        raise RuntimeError(f"UDS operation 0x{opcode:02x} rejected with result 0x{raw[2]:02x}")
    if opcode == 1:
        if len(raw) != 4 or raw[3] == 255:
            raise ValueError("Registration response lacks a valid assigned user index")
        return raw[3]
    return None


def save_state(path, state):
    state["updated_at"] = datetime.now(timezone.utc).isoformat()
    tmp = path.with_name(path.name + ".tmp")
    with tmp.open("w", encoding="utf-8") as f:
        json.dump(state, f, indent=2)
        f.write("\n")
        f.flush()
        import os
        os.fsync(f.fileno())
    tmp.replace(path)


class Session:
    def __init__(self, backend, device, emit):
        from gi.repository import GLib
        self.GLib = GLib
        self.backend, self.device, self.emit = backend, device, emit
        chars = inventory(backend.objects(), device["path"])
        self.chars = {(c["service_uuid"], c["uuid"]): c for c in chars}
        self.by_path = {c["path"]: c for c in chars}
        self.pending = None
        self.responses = []
        self.subscriptions = []
        self.loop = None
        self.live = True
        self.measurements = 0
        self.receiver = backend.bus.add_signal_receiver(
            self.changed, dbus_interface=PROPS, signal_name="PropertiesChanged",
            bus_name="org.bluez", path_keyword="path")

    def char(self, service, short):
        return self.chars[(uuid(service), uuid(short))]

    def changed(self, interface, values, invalidated, path=None):
        path = str(path)
        if str(interface) == DEVICE and path == self.device["path"] and "Connected" in values:
            self.live = bool(values["Connected"])
            if not self.live and self.loop:
                self.loop.quit()
        if str(interface) != CHAR or "Value" not in values:
            return
        raw = bytes(values["Value"])
        if path == self.char("181c", "2a9f")["path"]:
            self.emit("uds_response", raw_hex=raw.hex(" "))
            if self.pending is not None and len(raw) >= 2 and raw[:2] == bytes([0x20, self.pending]):
                self.responses.append(raw)
                if self.loop:
                    self.loop.quit()
        else:
            c = self.by_path.get(path)
            if c and c["service_uuid"] == uuid("ffff"):
                self.emit("private_value_changed", characteristic_uuid=c["uuid"], raw_hex=raw.hex(" "))
                print(f"Private event received ({c['uuid'][:8]}, {len(raw)} bytes)", flush=True)
            for service, short in (("181d", "2a9d"), ("181b", "2a9c")):
                if path == self.char(service, short)["path"]:
                    self.measurements += 1
                    self.emit("measurement", service_uuid=uuid(service),
                              characteristic_uuid=uuid(short), raw_hex=raw.hex(" "))
                    print(f"Measurement packet received ({short}, {len(raw)} bytes)", flush=True)

    def subscribe(self, service, short):
        c = self.char(service, short)
        self.backend.iface(c["path"], CHAR).StartNotify(timeout=self.backend.timeout)
        self.subscriptions.append(c["path"])

    def write(self, service, short, payload):
        c = self.char(service, short)
        flags = c["flags"]
        kind = "request" if "write" in flags else "command" if "write-without-response" in flags else None
        if kind is None:
            raise RuntimeError("Characteristic is not writable")
        d = self.backend.dbus
        self.backend.iface(c["path"], CHAR).WriteValue(
            d.Array(payload, signature="y"), d.Dictionary({"type": kind}, signature="sv"),
            timeout=self.backend.timeout)

    def read(self, service, short):
        return self.backend.read(self.char(service, short)["path"])

    def wait(self, seconds):
        self.loop = self.GLib.MainLoop()
        expired = [False]
        def expire():
            expired[0] = True
            self.loop.quit()
            return False
        timer = self.GLib.timeout_add(max(1, int(seconds * 1000)), expire)
        try:
            self.loop.run()
        finally:
            if not expired[0]:
                self.GLib.source_remove(timer)
            self.loop = None

    def control(self, opcode, payload=b""):
        self.pending, self.responses = opcode, []
        try:
            self.emit("uds_request", opcode=opcode)  # No consent-code payload in logs.
            self.write("181c", "2a9f", bytes([opcode]) + payload)
            self.wait(self.backend.timeout)
            if not self.live:
                raise RuntimeError("Scale disconnected during UDS procedure")
            if not self.responses:
                raise TimeoutError("No matching UDS indication; operation outcome may be unknown")
            raw = self.responses[-1]
            return response_value(raw, opcode)
        finally:
            self.pending = None

    def verified_write(self, short, payload):
        self.write("181c", short, payload)
        for _ in range(10):
            if self.read("181c", short) == payload:
                self.emit("profile_field_verified", characteristic_uuid=uuid(short))
                return
            time.sleep(0.2)
        raise RuntimeError(f"Profile readback did not match for {short}")

    def close(self):
        self.receiver.remove()
        for path in reversed(self.subscriptions):
            try:
                self.backend.iface(path, CHAR).StopNotify(timeout=5)
            except Exception:
                self.emit("subscription_cleanup_warning")


def configure(session, profile, state, persist, emit):
    if "user_index" not in state:
        if state["phase"] != "prepared":
            raise RuntimeError("Unresolved registration; refusing duplicate creation")
        current = session.read("181c", "2a9a")
        if current != b"\xff":
            raise RuntimeError("A user is already selected, or index format is unknown; inspect before creating another")
        state["phase"] = "registration_pending"
        persist(state)  # Save consent code and intent BEFORE transmitting.
        index = session.control(1, state["consent_code"].to_bytes(2, "little"))
        state.update(user_index=index, phase="registered")
        persist(state)  # Recover using the same user from this point onward.
        print("New user registration confirmed by the scale.", flush=True)
    session.control(2, bytes([state["user_index"]]) + state["consent_code"].to_bytes(2, "little"))
    if session.read("181c", "2a9a") != bytes([state["user_index"]]):
        raise RuntimeError("Selected user does not match the newly registered user")
    print("User authorization confirmed.", flush=True)
    if state["phase"] == "configured":
        emit("existing_profile_reused")
        print("Reusing the configured profile; no profile fields rewritten.", flush=True)
        return
    # Verify device field widths using live reads before writing profile values.
    height = session.read("181c", "2a8e")
    if len(session.read("181c", "2a85")) != 4 or len(session.read("181c", "2a8c")) != 1:
        raise RuntimeError("Unexpected date-of-birth or gender field width")
    payloads = profile_payloads(profile, len(height))
    before = session.read("181c", "2a99")
    if len(before) != 4 or before == b"\xff" * 4:
        raise RuntimeError("Database increment unavailable after consent")
    state["phase"] = "profile_writes_pending"
    persist(state)
    for short, payload in payloads.items():
        session.verified_write(short, payload)
    # Use the latest revision because some scales increment it on every field write.
    current = session.read("181c", "2a99")
    if len(current) != 4 or current == b"\xff" * 4:
        raise RuntimeError("Database increment invalid after profile writes")
    revision = int.from_bytes(current, "little")
    session.verified_write("2a99", (0 if revision >= 0xfffffffe else revision + 1).to_bytes(4, "little"))
    state["phase"] = "configured"
    persist(state)
    emit("profile_configured", height_field_bytes=len(height), activity_level="unchanged; vendor mapping unverified")
    print("Profile fields written and read back successfully. Activity level unchanged.", flush=True)



def start_measurement(session, emit):
    # Explicit opt-in trial: same service/characteristic/payload as related BF500.
    c = session.char("ffff", "fff4")
    if "notify" not in c["flags"] or not set(c["flags"]) & {"write", "write-without-response"}:
        raise RuntimeError("Measurement trigger properties do not match the documented related-model candidate")
    for candidate in session.chars.values():
        if candidate["service_uuid"] == uuid("ffff") and set(candidate["flags"]) & {"notify", "indicate"}:
            session.subscribe("ffff", candidate["uuid"][4:8])
    emit("measurement_start_trial", service_uuid=uuid("ffff"), characteristic_uuid=uuid("fff4"), raw_hex="00")
    session.write("ffff", "fff4", b"\x00")
    print("Sent the explicit measurement-start trial; waiting for device data.", flush=True)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", type=Path, default=ROOT / "database/profile-input.json")
    parser.add_argument("--state", type=Path, default=ROOT / "database/profile-state.json")
    parser.add_argument("--timeout", type=positive, default=20)
    parser.add_argument("--listen-seconds", type=positive, default=60)
    parser.add_argument("--start-measurement", action="store_true",
                        help="Explicit BF500-style FFFF/FFF4 measurement-start trial on BF990")
    args = parser.parse_args(argv)
    try:
        profile = load_profile(args.profile)
    except (OSError, ValueError):
        print("Profile input is invalid or unreadable; no Bluetooth changes made.", file=sys.stderr)
        return 1
    args.state.parent.mkdir(parents=True, exist_ok=True)
    lock = args.state.with_name(args.state.name + ".lock")
    try:
        lock.mkdir()
    except FileExistsError:
        print("Setup lock exists; check for another process or interrupted run before removing it.", file=sys.stderr)
        return 1
    backend = session = None
    code = 1
    target = args.state.parent / "captures" / (datetime.now(timezone.utc).strftime("setup-%Y%m%dT%H%M%S-%fZ.jsonl"))
    try:
        from dbus.mainloop.glib import DBusGMainLoop
        DBusGMainLoop(set_as_default=True)
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("x") as output:
            def emit(event, **fields):
                output.write(json.dumps({"event": event, "at": datetime.now(timezone.utc).isoformat(), **fields}) + "\n")
                output.flush()
            try:
                state = json.loads(args.state.read_text()) if args.state.exists() else None
                if state and state["phase"] != "prepared" and "user_index" not in state:
                    raise RuntimeError("Previous registration outcome unresolved; refusing duplicate creation")
                backend = BlueZ("hci0", args.timeout)
                print("Profile validated. Connecting to BF990; please wake the scale...", flush=True)
                device = select_device(backend.discover(3), name="BF990")
                backend.connect(device["path"])
                session = Session(backend, device, emit)
                if session.read("180a", "2a24").rstrip(b"\x00") != b"BF990":
                    raise RuntimeError("Live model verification failed")
                if state is None:
                    state = {"schema": 1, "address": device["address"], "phase": "prepared",
                             "consent_code": secrets.randbelow(10000),
                             "profile_fingerprint": fingerprint(profile)}
                    save_state(args.state, state)
                check_state(state, device["address"], profile)
                for service, short in (("181c", "2a9f"), ("181d", "2a9d"), ("181b", "2a9c")):
                    session.subscribe(service, short)
                configure(session, profile, state, lambda s: save_state(args.state, s), emit)
                if args.start_measurement:
                    start_measurement(session, emit)
                print(f"LISTENING for {args.listen_seconds:g} seconds. Perform a reference measurement now.", flush=True)
                emit("listening", seconds=args.listen_seconds)
                session.wait(args.listen_seconds)
                emit("listening_finished", measurement_packets=session.measurements)
                if not session.live:
                    raise RuntimeError("Disconnected during measurement window; profile may still be configured")
                code = 0
            except KeyboardInterrupt:
                emit("interrupted")
                code = 130
            except Exception as exc:
                # Do not include profile values or credential-bearing request bytes.
                print(f"Setup stopped: {exc}", file=sys.stderr, flush=True)
                emit("error", message=str(exc))
            finally:
                if session:
                    session.close()
                if backend:
                    try:
                        backend.close()
                    except Exception:
                        emit("connection_cleanup_warning")
                emit("finished", exit_code=code)
        print(f"Private setup log: {target}", flush=True)
    finally:
        lock.rmdir()
    return code


if __name__ == "__main__":
    raise SystemExit(main())
