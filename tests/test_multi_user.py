import unittest
from beu_connect.multi_user_test import verify_user, PROFILES
from beu_connect.setup_profile import profile_payloads

class Session:
    def __init__(self):
        self.calls = []
        self.fields = profile_payloads(PROFILES[2], 2)
        self.fields['2a9a'] = b'\x02'
    def control(self, opcode, payload):
        self.calls.append((opcode, payload[0]))
    def read(self, service, field):
        return self.fields[field]

class MultiUserTests(unittest.TestCase):
    def test_verifies_each_field_after_authorization(self):
        session = Session()
        verify_user(session, {'phase':'configured', 'user_index':2, 'consent_code':1234}, PROFILES[2], 2)
        self.assertEqual(session.calls, [(2, 2)])
    def test_refuses_wrong_state_before_any_operation(self):
        session = Session()
        with self.assertRaises(RuntimeError):
            verify_user(session, {'phase':'registration_pending', 'user_index':2, 'consent_code':1234}, PROFILES[2], 2)
        self.assertEqual(session.calls, [])
    def test_detects_wrong_birth_date_or_selection(self):
        for field in ('2a85', '2a9a'):
            session = Session()
            session.fields[field] = b'\x00'
            with self.assertRaises(RuntimeError):
                verify_user(session, {'phase':'configured', 'user_index':2, 'consent_code':1234}, PROFILES[2], 2)
