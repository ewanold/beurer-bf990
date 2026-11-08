import unittest
from beu_connect.app import probe, select_device, uuid

DEVICE = {"name": "BF990", "address": "00:11:22:33:44:55", "path": "/scale"}


class FakeBlueZ:
    def __init__(self):
        self.values = {uuid("2a29"): b"Beurer", uuid("2a24"): b"BF990",
                       uuid("2a26"): b"V02.13", uuid("2a19"): b"\x64",
                       uuid("2a9e"): bytes.fromhex("9f000000"),
                       uuid("2a9b"): bytes.fromhex("cf100000")}
        self.live = True
        self.fail_connect = False

    def connect(self, path):
        if self.fail_connect:
            raise TimeoutError("Scale asleep")

    def inventory(self, path):
        return [uuid("181d"), uuid("181b")], {k: k for k in self.values}

    def read(self, path):
        value = self.values[path]
        if isinstance(value, Exception):
            raise value
        return value

    def connected(self, path):
        return self.live


class ProofTests(unittest.TestCase):
    def test_live_reads_prove_connection_but_not_bf600_compatibility(self):
        report = {}
        probe(FakeBlueZ(), DEVICE, report)
        self.assertTrue(report["connection_proven"])
        self.assertTrue(report["scale_profile_verified"])
        self.assertFalse(report["measurements_downloaded"])

    def test_advertisements_and_cached_services_alone_are_not_proof(self):
        backend = FakeBlueZ()
        backend.values = {k: RuntimeError("Read failed") for k in backend.values}
        report = {}
        probe(backend, DEVICE, report)
        self.assertFalse(report["connection_proven"])
        self.assertFalse(report["scale_profile_verified"])
        self.assertEqual(len(report["read_errors"]), 6)

    def test_partial_reads_are_not_full_profile_verification(self):
        backend = FakeBlueZ()
        backend.values.pop(uuid("2a9b"))
        report = {}
        probe(backend, DEVICE, report)
        self.assertTrue(report["connection_proven"])
        self.assertFalse(report["scale_profile_verified"])

    def test_malformed_features_are_not_verified(self):
        backend = FakeBlueZ()
        backend.values[uuid("2a9e")] = b"\x01"
        report = {}
        probe(backend, DEVICE, report)
        self.assertFalse(report["weight_service_verified"])

    def test_disconnect_during_reads_is_not_connection_success(self):
        backend = FakeBlueZ()
        backend.live = False
        report = {}
        probe(backend, DEVICE, report)
        self.assertFalse(report["connection_proven"])
        self.assertFalse(report["scale_profile_verified"])

    def test_connection_timeout_propagates(self):
        backend = FakeBlueZ()
        backend.fail_connect = True
        report = {"connection_proven": False}
        with self.assertRaises(TimeoutError):
            probe(backend, DEVICE, report)
        self.assertFalse(report["connection_proven"])

    def test_multiple_scales_require_selection(self):
        with self.assertRaisesRegex(RuntimeError, "Multiple"):
            select_device([DEVICE, dict(DEVICE, name="BF600")])

    def test_exact_name_and_case_insensitive_address_selection(self):
        devices = [DEVICE, dict(DEVICE, name="BF600", address="AA:BB:CC:DD:EE:FF")]
        self.assertEqual(select_device(devices, name="bf600")["name"], "BF600")
        self.assertEqual(select_device(devices, address="aa:bb:cc:dd:ee:ff")["name"], "BF600")

    def test_no_scale_does_not_select_unrelated_device(self):
        with self.assertRaisesRegex(RuntimeError, "No matching"):
            select_device([dict(DEVICE, name="Phone")])


if __name__ == "__main__":
    unittest.main()
