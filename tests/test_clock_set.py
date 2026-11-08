from datetime import datetime
import unittest
from beu_connect.clock_set_test import encode_local, parse_time

class ClockSetTests(unittest.TestCase):
    def test_round_trip_and_current_time_fields(self):
        value = datetime(2024, 2, 29, 23, 59, 58)
        raw = encode_local(value)
        self.assertEqual(parse_time(raw), value)
        self.assertEqual(raw[7:], bytes([4, 0, 1]))
    def test_rejects_bad_length_or_invalid_date(self):
        with self.assertRaises(ValueError): parse_time(b"\x00")
        with self.assertRaises(ValueError): parse_time(bytes.fromhex("e8070d01120000000000"))
