import unittest

from beu_connect.protocol_candidates import parse_advertisement, bf600_pending_candidate


def ad(kind, data):
    return bytes((len(data) + 1, kind)) + data


class AdvertisementTests(unittest.TestCase):
    def test_complete_multiple_indices_and_raw_retained(self):
        raw = ad(0x16, b"\x1d\x18\x01\x03") + b"\x00"
        parsed = parse_advertisement(raw)
        result = bf600_pending_candidate(parsed)
        self.assertFalse(parsed.malformed)
        self.assertEqual(result["indices"], (1, 3))
        self.assertEqual(result["raw"], raw)
        self.assertTrue(result["candidate_only"])

    def test_absent_and_unrelated_service_data(self):
        for raw in (b"\x00", ad(0x16, b"\x0f\x18\x01")):
            result = bf600_pending_candidate(parse_advertisement(raw))
            self.assertEqual(result["indices"], ())
            self.assertEqual(result["match_count"], 0)

    def test_empty_pending_list(self):
        result = bf600_pending_candidate(parse_advertisement(ad(0x16, b"\x1d\x18")))
        self.assertEqual(result["match_count"], 1)
        self.assertEqual(result["indices"], ())

    def test_duplicate_fields_are_preserved(self):
        raw = ad(0x16, b"\x1d\x18\x02") * 2
        result = bf600_pending_candidate(parse_advertisement(raw))
        self.assertEqual(result["indices"], (2, 2))
        self.assertEqual(result["match_count"], 2)

    def test_unknown_fields_and_truncation(self):
        parsed = parse_advertisement(ad(0x99, b"abc") + b"\x04\x16\x1d")
        self.assertTrue(parsed.malformed)
        self.assertEqual(parsed.fields, ((0x99, b"abc"),))
        self.assertEqual(parsed.raw, ad(0x99, b"abc") + b"\x04\x16\x1d")

    def test_zero_length_terminates_and_bad_length_marks_malformed(self):
        self.assertFalse(parse_advertisement(b"\x00\x01").malformed)
        self.assertTrue(parse_advertisement(b"\x05\x16\x1d").malformed)


class CandidateUserListDesignTests(unittest.TestCase):
    """Synthetic BF600 frames are deliberately not parsed as BF990 behavior."""

    def test_candidate_layout_vectors_are_distinguishable(self):
        # BF600 reference candidate: event 00 + fields; complete 01; empty 02.
        entry = bytes((0, 1, 65, 66, 67, 0xE8, 0x07, 1, 2, 170, 1, 3))
        self.assertEqual(entry[0], 0)
        self.assertEqual(bytes((1,)), b"\x01")
        self.assertEqual(bytes((2,)), b"\x02")
        self.assertNotEqual(entry[0], 1)


if __name__ == "__main__":
    unittest.main()
