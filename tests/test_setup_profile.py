import json
from pathlib import Path
from types import SimpleNamespace
import unittest
from beu_connect.setup_profile import check_state, configure, fingerprint, load_profile, profile_payloads, response_value, start_measurement

PROFILE = {"date_of_birth": "1980-02-29", "height_cm": 175, "sex": "female"}


class FakeSession:
    def __init__(self):
        self.fields = {"2a9a": b"\xff", "2a8e": b"\xa5\x00", "2a85": b"\x00" * 4,
                       "2a8c": b"\x00", "2a99": b"\x00" * 4}
        self.calls = []
        self.timeout_registration = False

    def read(self, service, short):
        return self.fields[short]

    def control(self, opcode, payload):
        self.calls.append(opcode)
        if opcode == 1:
            if self.timeout_registration:
                raise TimeoutError("No response")
            return 2
        if opcode == 2:
            self.fields["2a9a"] = bytes([payload[0]])

    def verified_write(self, short, payload):
        self.calls.append(short)
        self.fields[short] = payload


class SetupTests(unittest.TestCase):
    def state(self):
        return {"address": "AA:BB", "phase": "prepared", "consent_code": 1234,
                "profile_fingerprint": fingerprint(PROFILE)}

    def test_input_validation_and_private_error(self):
        obj = SimpleNamespace(read_text=lambda: json.dumps(PROFILE))
        self.assertEqual(load_profile(obj), PROFILE)
        for field, value in [("height_cm", True), ("height_cm", 175.5), ("sex", "other"),
                             ("date_of_birth", "1981-02-29"), ("date_of_birth", "2999-01-01")]:
            with self.subTest(field=field, value=value):
                obj = SimpleNamespace(read_text=lambda: json.dumps(dict(PROFILE, **{field: value})))
                with self.assertRaises(ValueError):
                    load_profile(obj)

    def test_payloads_follow_live_height_width(self):
        values = profile_payloads(PROFILE, 2)
        self.assertEqual(values["2a85"], bytes.fromhex("bc07021d"))
        self.assertEqual(values["2a8e"], bytes.fromhex("af00"))
        self.assertEqual(values["2a8c"], b"\x01")
        self.assertEqual(profile_payloads(PROFILE, 1)["2a8e"], b"\xaf")
        with self.assertRaises(ValueError):
            profile_payloads(PROFILE, 3)

    def test_response_validation(self):
        self.assertEqual(response_value(bytes.fromhex("20010102"), 1), 2)
        for payload in ["2001", "20020102", "200101ff", "200101"]:
            with self.assertRaises(ValueError):
                response_value(bytes.fromhex(payload), 1)
        with self.assertRaises(RuntimeError):
            response_value(bytes.fromhex("200104"), 1)

    def test_pending_state_prevents_another_registration(self):
        state = self.state()
        state["phase"] = "registration_pending"
        with self.assertRaises(RuntimeError):
            check_state(state, "AA:BB", PROFILE)
        fake = FakeSession()
        with self.assertRaises(RuntimeError):
            configure(fake, PROFILE, state, lambda s: None, lambda *a, **k: None)
        self.assertEqual(fake.calls, [])

    def test_device_and_input_binding(self):
        with self.assertRaises(RuntimeError):
            check_state(self.state(), "CC:DD", PROFILE)
        with self.assertRaises(RuntimeError):
            check_state(self.state(), "AA:BB", dict(PROFILE, height_cm=176))

    def test_timeout_keeps_intent_and_does_not_write_profile(self):
        state = self.state()
        fake = FakeSession()
        fake.timeout_registration = True
        saved = []
        with self.assertRaises(TimeoutError):
            configure(fake, PROFILE, state, lambda s: saved.append(dict(s)), lambda *a, **k: None)
        self.assertEqual(saved[0]["phase"], "registration_pending")
        self.assertEqual(fake.calls, [1])
        self.assertNotIn("user_index", state)

    def test_success_persists_index_before_profile_and_reuses_it(self):
        state = self.state()
        saved = []
        fake = FakeSession()
        configure(fake, PROFILE, state, lambda s: saved.append(dict(s)), lambda *a, **k: None)
        self.assertEqual(state["phase"], "configured")
        self.assertEqual(saved[1]["user_index"], 2)
        self.assertEqual(saved[1]["phase"], "registered")
        self.assertEqual(fake.calls, [1, 2, "2a85", "2a8c", "2a8e", "2a99"])
        fake.calls.clear()
        configure(fake, PROFILE, state, lambda s: None, lambda *a, **k: None)
        self.assertNotIn(1, fake.calls)
        self.assertEqual(fake.calls, [2])

    def test_unexpected_assigned_index_is_saved_before_stopping(self):
        fake = FakeSession()
        original_control = fake.control
        fake.control = lambda opcode, payload: 3 if opcode == 1 else original_control(opcode, payload)
        state = self.state()
        saved = []
        with self.assertRaisesRegex(RuntimeError, "differs"):
            configure(fake, PROFILE, state, lambda s: saved.append(dict(s)), lambda *a, **k: None, expected_index=2)
        self.assertEqual(state["user_index"], 3)
        self.assertEqual(saved[-1]["phase"], "registered")
        self.assertEqual(fake.calls, [])

    def test_existing_selected_user_blocks_creation(self):
        fake = FakeSession()
        fake.fields["2a9a"] = b"\x03"
        with self.assertRaises(RuntimeError):
            configure(fake, PROFILE, self.state(), lambda s: None, lambda *a, **k: None)
        self.assertEqual(fake.calls, [])

    def test_unexpected_field_width_stops_before_profile_writes(self):
        fake = FakeSession()
        fake.fields["2a8e"] = b"\x00" * 3
        with self.assertRaises(ValueError):
            configure(fake, PROFILE, self.state(), lambda s: None, lambda *a, **k: None)
        self.assertEqual(fake.calls, [1, 2])

    def test_revision_wrap_does_not_use_unknown_sentinel(self):
        fake = FakeSession()
        fake.fields["2a99"] = bytes.fromhex("feffffff")
        configure(fake, PROFILE, self.state(), lambda s: None, lambda *a, **k: None)
        self.assertEqual(fake.fields["2a99"], b"\x00" * 4)

    def test_measurement_trigger_requires_expected_properties(self):
        fake = SimpleNamespace(char=lambda *a: {"flags": ["read"]})
        with self.assertRaises(RuntimeError):
            start_measurement(fake, lambda *a, **k: None)

    def test_explicit_trigger_subscribes_then_writes_related_model_payload(self):
        from beu_connect.app import uuid
        calls = []
        characteristic = {"service_uuid": uuid("ffff"), "uuid": uuid("fff4"), "flags": ["notify", "write-without-response"]}
        fake = SimpleNamespace(char=lambda *a: characteristic,
            chars={"test": characteristic},
            subscribe=lambda *a: calls.append(("subscribe", a)),
            write=lambda *a: calls.append(("write", a)))
        start_measurement(fake, lambda *a, **k: None)
        self.assertEqual(calls, [("subscribe", ("ffff", "fff4")), ("write", ("ffff", "fff4", b"\x00"))])
