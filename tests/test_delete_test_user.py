import unittest
from beu_connect.delete_test_user import delete_and_restore
from beu_connect.setup_profile import profile_payloads

PROFILE = {"date_of_birth": "1990-01-02", "height_cm": 170, "sex": "female"}
ORIGINAL = {"phase": "configured", "user_index": 1, "consent_code": 101}
TEST_USER = {"phase": "configured", "user_index": 2, "consent_code": 202}

class FakeSession:
    def __init__(self):
        self.index = 255
        self.calls = []
        self.fields = profile_payloads(PROFILE, 2)
        self.fail_delete = False
    def control(self, op, payload=b""):
        self.calls.append(op)
        if op == 2: self.index = payload[0]
        if op == 3:
            if self.fail_delete: raise TimeoutError("unknown outcome")
            self.index = 255
    def read(self, service, short):
        return bytes([self.index]) if short == "2a9a" else self.fields[short]

class DeleteTests(unittest.TestCase):
    def test_deletes_only_after_verification_and_restores_original(self):
        f = FakeSession(); state = dict(TEST_USER); saved = []
        delete_and_restore(f, ORIGINAL, state, PROFILE, lambda s: saved.append(s["phase"]))
        self.assertEqual(f.calls, [2, 2, 3, 2])
        self.assertEqual(saved, ["deletion_pending", "deletion_acknowledged"])
        self.assertEqual(f.index, 1)
    def test_unknown_delete_outcome_is_saved_and_original_restored(self):
        f = FakeSession(); f.fail_delete = True; state = dict(TEST_USER); saved = []
        with self.assertRaises(TimeoutError):
            delete_and_restore(f, ORIGINAL, state, PROFILE, lambda s: saved.append(s["phase"]))
        self.assertEqual(saved, ["deletion_pending"])
        self.assertEqual(f.index, 1)
        with self.assertRaises(RuntimeError):
            delete_and_restore(f, ORIGINAL, state, PROFILE, lambda s: None)
        self.assertEqual(f.calls.count(3), 1)
    def test_profile_mismatch_prevents_deletion(self):
        f = FakeSession(); f.fields["2a85"] = b"bad!"
        with self.assertRaises(RuntimeError):
            delete_and_restore(f, ORIGINAL, dict(TEST_USER), PROFILE, lambda s: None)
        self.assertNotIn(3, f.calls)
    def test_wrong_index_prevents_any_operation(self):
        f = FakeSession()
        with self.assertRaises(RuntimeError):
            delete_and_restore(f, ORIGINAL, dict(TEST_USER, user_index=3), PROFILE, lambda s: None)
        self.assertEqual(f.calls, [])
