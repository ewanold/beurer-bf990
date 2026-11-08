"""Offline parsers for explicitly unverified BF600-derived protocol candidates.

These helpers parse bytes only. They make no claim about BF990 behavior.
"""

from dataclasses import dataclass


@dataclass(frozen=True)
class Advertisement:
    raw: bytes
    fields: tuple
    malformed: bool


def parse_advertisement(raw):
    """Parse BLE AD structures (length includes type, excludes length byte)."""
    raw = bytes(raw)
    fields = []
    offset = 0
    malformed = False
    while offset < len(raw):
        size = raw[offset]
        if size == 0:
            break
        end = offset + 1 + size
        if size < 1 or end > len(raw):
            malformed = True
            break
        fields.append((raw[offset + 1], raw[offset + 2:end]))
        offset = end
    return Advertisement(raw, tuple(fields), malformed)


def bf600_pending_candidate(advertisement):
    """Return candidate indices for 0x16 service data UUID 0x181D.

    The candidate convention is the BF600 document's UUID bytes 1d 18,
    followed by indices. Original structures remain available on the result.
    """
    matches = [data for kind, data in advertisement.fields
               if kind == 0x16 and data.startswith(b"\x1d\x18")]
    indices = tuple(i for payload in matches for i in payload[2:])
    return {"raw": advertisement.raw, "malformed": advertisement.malformed,
            "match_count": len(matches), "indices": indices,
            "candidate_only": True}
