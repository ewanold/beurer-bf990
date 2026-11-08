from datetime import datetime
from types import SimpleNamespace
import unittest
from beu_connect.app import uuid
from beu_connect.decode import decode_weight, decode_body, decode_events, compare_observations, read_observations


def weight_frame():
    # Synthetic: 80 kg, BMI 25, height 1.75 m, 2026-11-07 12:00:30, user 1.
    return (b"\x0e" + (16000).to_bytes(2, "little") + (2026).to_bytes(2, "little") +
            bytes([11, 7, 12, 0, 30, 1]) + (250).to_bytes(2, "little") + (1750).to_bytes(2, "little"))


def body_frame():
    return (0x0398).to_bytes(2, "little") + b"".join(v.to_bytes(2, "little") for v in [200, 777, 400, 1234, 9600, 4321])


def events():
    start = datetime(2026, 11, 7, 12, 0, tzinfo=datetime.now().astimezone().tzinfo).isoformat()
    return [{"event": "listening", "at": start, "seconds": 180},
            {"event": "measurement", "at": start, "characteristic_uuid": uuid("2a9d"), "raw_hex": weight_frame().hex()},
            {"event": "measurement", "at": start, "characteristic_uuid": uuid("2a9c"), "raw_hex": body_frame().hex()}]


class DecoderTests(unittest.TestCase):
    def test_synthetic_weight_units_timestamp_and_raw(self):
        r = decode_weight(weight_frame())
        self.assertAlmostEqual(r["weight_kg"], 80)
        self.assertAlmostEqual(r["bmi"], 25)
        self.assertAlmostEqual(r["height_m"], 1.75)
        self.assertEqual(r["measured_local_time"], "2026-11-07T12:00:30")
        self.assertEqual(bytes.fromhex(r["weight_raw_hex"]), weight_frame())

    def test_body_percentages_water_conversion_and_unknown_hr(self):
        r = decode_body(body_frame(), 80)
        self.assertAlmostEqual(r["body_fat_percent"], 20)
        self.assertAlmostEqual(r["muscle_percent"], 40)
        self.assertAlmostEqual(r["body_water_percent"], 60)
        self.assertAlmostEqual(r["body_water_mass_kg"], 48)
        self.assertIsNone(r["heart_rate_bpm"])
        self.assertEqual(r["unvalidated_raw_fields"]["bmr"], 777)

    def test_zero_body_payload_stays_missing(self):
        r = decode_body(bytes.fromhex("9803") + b"\x00" * 12, 80)
        for key in ["body_fat_percent", "muscle_percent", "body_water_percent", "heart_rate_bpm"]:
            self.assertIsNone(r[key])
        self.assertEqual(r["body_data_status"], "all_zero_unavailable")

    def test_unknown_flags_and_wrong_lengths_rejected(self):
        for raw in [weight_frame()[:-1], b"\x0f" + weight_frame()[1:], weight_frame() + b"\x00"]:
            with self.assertRaises(ValueError): decode_weight(raw)
        for raw in [body_frame()[:-1], bytes.fromhex("9813") + body_frame()[2:]]:
            with self.assertRaises(ValueError): decode_body(raw, 80)
        with self.assertRaises(ValueError): decode_body(body_frame(), 0)

    def test_invalid_date_rejected_unknown_date_retained_missing(self):
        b = bytearray(weight_frame()); b[5] = 13
        with self.assertRaises(ValueError): decode_weight(bytes(b))
        b[5] = 0
        self.assertIsNone(decode_weight(bytes(b))["measured_local_time"])

    def test_pairs_current_frames_and_preserves_private_bytes(self):
        e = events() + [{"event": "private_value_changed", "raw_hex": "00 01", "characteristic_uuid": uuid("fff8")}]
        r = decode_events(e)
        self.assertEqual(len(r["records"]), 1)
        self.assertEqual(r["records"][0]["capture_time_class"], "current")
        self.assertIn("body_raw_hex", r["records"][0])
        self.assertEqual(r["private_events_retained_undecoded"][0]["raw_hex"], "00 01")
        self.assertEqual(r["warnings"], [])

    def test_orphan_duplicate_and_after_disconnect_body_not_paired(self):
        e = events()
        self.assertEqual(len(decode_events([e[2]])["warnings"]), 1)
        self.assertEqual(len(decode_events(e + [e[2]])["warnings"]), 1)
        e.insert(2, {"event": "connection_changed", "connected": False})
        self.assertEqual(len(decode_events(e)["warnings"]), 1)
        self.assertNotIn("body_raw_hex", decode_events(e)["records"][0])

    def test_ambiguous_observations_are_not_given_a_unique_cycle(self):
        r = decode_events(events())["records"][0]
        values = {k: r[k] for k in ["weight_kg", "bmi", "body_fat_percent", "body_water_percent", "muscle_percent"]}
        c = compare_observations([r, dict(r)], [{"cycle": 1, "values": values}])["cycles"][0]
        self.assertTrue(c["all_five_metrics_agree"])
        self.assertFalse(c["unique_cycle_assignment"])
        self.assertEqual(c["matching_current_record_indices"], [0, 1])
        changed = dict(values, weight_kg=81)
        c = compare_observations([r], [{"cycle": 1, "values": changed}])["cycles"][0]
        self.assertFalse(c["all_five_metrics_agree"])

    def test_missing_body_does_not_match_or_fake_metrics(self):
        r = decode_events(events()[:2])["records"][0]
        c = compare_observations([r], [{"cycle": 1, "values": {"body_fat_percent": 20}}])["cycles"][0]
        self.assertFalse(c["all_five_metrics_agree"])

    def test_worksheet_parser_decimal_comma_and_hr_text(self):
        text = """| Weight and unit | 80,0 kg | 80.0 kg |
| BMI | 25,0 | 25.0 |
| Body fat (%) | 20,0 | 20.0 |
| Body water (%) | 60,0 | 60.0 |
| Muscle: exact label/value/unit | 40,0 % | 40.0 % |
| Heart rate (bpm) | no HR | missed |
"""
        result = read_observations(SimpleNamespace(read_text=lambda: text))
        self.assertEqual(result[0]["values"]["weight_kg"], 80)
        self.assertEqual(result[0]["heart_rate_observation"], "no HR")
        compact = read_observations(SimpleNamespace(read_text=lambda: text.replace("80,0 kg", "80,0kg")))
        self.assertEqual(compact[0]["values"]["weight_kg"], 80)
        with self.assertRaises(ValueError):
            read_observations(SimpleNamespace(read_text=lambda: text.replace("80,0 kg", "176 lb")))
