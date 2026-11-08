import unittest
from beu_connect.app import CHAR, SERVICE, uuid
from beu_connect.capture import inventory


class InventoryTests(unittest.TestCase):
    def test_preserves_duplicate_uuid_parent_services_and_descriptors(self):
        objects = {
            "/scale/a": {SERVICE: {"UUID": uuid("ffff"), "Device": "/scale"}},
            "/scale/b": {SERVICE: {"UUID": uuid("ffef"), "Device": "/scale"}},
            "/scale/a/c": {CHAR: {"UUID": uuid("fff1"), "Service": "/scale/a", "Flags": ["notify"]}},
            "/scale/b/c": {CHAR: {"UUID": uuid("fff1"), "Service": "/scale/b", "Flags": ["read"]}},
            "/scale/a/c/d": {"org.bluez.GattDescriptor1": {"UUID": uuid("2902"), "Characteristic": "/scale/a/c"}},
            "/other/s": {SERVICE: {"UUID": uuid("181d"), "Device": "/other"}},
            "/other/s/c": {CHAR: {"UUID": uuid("2a9d"), "Service": "/other/s", "Flags": ["indicate"]}},
        }
        result = inventory(objects, "/scale")
        self.assertEqual(len(result), 2)
        self.assertEqual({c["service_uuid"] for c in result}, {uuid("ffff"), uuid("ffef")})
        self.assertEqual(result[0]["flags"], ["notify"])
        self.assertEqual(result[0]["descriptors"][0]["uuid"], uuid("2902"))
        self.assertEqual(result[1]["descriptors"], [])

    def test_empty_inventory(self):
        self.assertEqual(inventory({}, "/scale"), [])
