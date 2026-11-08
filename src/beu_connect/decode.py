"""Offline decoder/comparison for the BF990 packet layouts verified locally."""
import argparse
from datetime import datetime, timedelta
from decimal import Decimal
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
METRICS = {
    "weight_kg": "Weight and unit", "bmi": "BMI", "body_fat_percent": "Body fat (%)",
    "body_water_percent": "Body water (%)", "muscle_percent": "Muscle: exact label/value/unit",
}


def u16(raw, offset):
    return int.from_bytes(raw[offset:offset + 2], "little")


def decode_weight(raw):
    if len(raw) != 15 or raw[0] != 0x0e:
        raise ValueError("Unsupported weight layout; expected observed metric flags 0E and 15 bytes")
    weight = u16(raw, 1) * 0.005
    if not 0 < weight <= 180:
        raise ValueError("Invalid BF990 weight value")
    parts = [u16(raw, 3), *raw[5:10]]
    try:
        stamp = None if any(v == 0 for v in parts[:3]) else datetime(*parts).isoformat()
    except ValueError:
        raise ValueError("Invalid measurement timestamp") from None
    return {"weight_kg": weight, "bmi": u16(raw, 11) * 0.1,
            "height_m": u16(raw, 13) * 0.001, "user_index": raw[10],
            "measured_local_time": stamp, "weight_raw_hex": raw.hex(" ")}


def decode_body(raw, weight_kg):
    if len(raw) != 14 or u16(raw, 0) != 0x0398:
        raise ValueError("Unsupported body layout; expected observed flags 0398 and 14 bytes")
    if weight_kg is None or weight_kg <= 0:
        raise ValueError("A valid paired weight is required for water percentage")
    fields = [u16(raw, i) for i in range(2, 14, 2)]
    missing = all(v == 0 for v in fields)
    return {"body_fat_percent": None if missing else fields[0] * 0.1,
            "muscle_percent": None if missing else fields[2] * 0.1,
            "body_water_mass_kg": None if missing else fields[4] * 0.005,
            "body_water_percent": None if missing else fields[4] * 0.005 / weight_kg * 100,
            "body_data_status": "all_zero_unavailable" if missing else "present",
            "heart_rate_bpm": None, "heart_rate_status": "not_identified_in_decoded_fields",
            "unvalidated_raw_fields": {"bmr": fields[1], "soft_lean_mass": fields[3], "impedance": fields[5]},
            "body_raw_hex": raw.hex(" ")}


def decode_events(events):
    records, warnings, private = [], [], []
    pending = None
    start_event = next((e for e in events if e.get("event") == "listening"), None)
    start = datetime.fromisoformat(start_event["at"]) if start_event else None
    seconds = start_event.get("seconds", 0) if start_event else 0
    for line, event in enumerate(events, 1):
        kind = event.get("event")
        if kind == "private_value_changed":
            private.append(event)
        if kind in ("connection_changed", "error", "interrupted", "finished"):
            pending = None
        if kind != "measurement":
            continue
        short = event.get("characteristic_uuid", "")[4:8]
        try:
            raw = bytes.fromhex(event["raw_hex"])
            if short == "2a9d":
                pending = None
                decoded = decode_weight(raw)
                decoded["received_at"] = event["at"]
                decoded["capture_time_class"] = "unknown"
                if start and decoded["measured_local_time"]:
                    # Scale Date Time has no zone; explicitly assume host-local zone.
                    t = datetime.fromisoformat(decoded["measured_local_time"]).replace(
                        tzinfo=datetime.now().astimezone().tzinfo)
                    decoded["capture_time_class"] = ("stored" if t < start else
                        "current" if t <= start + timedelta(seconds=seconds) else "after_window")
                decoded["heart_rate_bpm"] = None
                decoded["heart_rate_status"] = "not_identified_in_decoded_fields"
                records.append(decoded)
                pending = decoded
            elif short == "2a9c":
                if pending is None or "body_raw_hex" in pending:
                    raise ValueError("Body packet has no unique preceding weight in this session")
                pending.update(decode_body(raw, pending["weight_kg"]))
        except (KeyError, ValueError) as exc:
            warnings.append({"event_number": line, "error": str(exc)})
            pending = None
    return {"schema": 1, "decoder_scope": "Observed BF990 metric layouts only; no private or HR decoding",
            "timezone_assumption": "Scale timestamps use the decoding host local time zone",
            "records": records, "private_events_retained_undecoded": private, "warnings": warnings}


def read_observations(path):
    rows = {}
    for line in path.read_text().splitlines():
        if line.startswith("|"):
            parts = [p.strip() for p in line.split("|")[1:-1]]
            if len(parts) == 3:
                rows[parts[0]] = parts[1:]
    result = []
    for cycle in range(2):
        values = {}
        for key, label in METRICS.items():
            cell = rows.get(label, ["", ""])[cycle]
            tokens = re.findall(r"[-+]?\d+(?:[.,]\d+)?", cell)
            if len(tokens) != 1:
                raise ValueError(f"Cycle {cycle + 1}, {label}: expected exactly one numeric value")
            if key == "weight_kg" and not re.search(r"(?<![A-Za-z])kg(?![A-Za-z])", cell, re.I):
                raise ValueError("Observation comparison currently requires kg")
            if key == "muscle_percent" and "%" not in cell:
                raise ValueError("Muscle observation must explicitly use percent")
            values[key] = float(Decimal(tokens[0].replace(",", ".")))
        result.append({"cycle": cycle + 1, "values": values,
                       "heart_rate_observation": rows.get("Heart rate (bpm)", ["", ""])[cycle]})
    return result


def compare_observations(records, observations):
    candidates = [(i, r) for i, r in enumerate(records) if r["capture_time_class"] == "current"]
    comparisons = []
    for observation in observations:
        matches, details = [], []
        for i, record in candidates:
            checks = {k: record.get(k) is not None and
                      abs(record[k] - v) <= 0.050000001 for k, v in observation["values"].items()}
            details.append({"record_index": i, "metric_matches_within_display_rounding": checks})
            if all(checks.values()):
                matches.append(i)
        comparisons.append({"cycle": observation["cycle"], "matching_current_record_indices": matches,
                            "all_five_metrics_agree": bool(matches),
                            "unique_cycle_assignment": len(matches) == 1, "comparisons": details})
    return {"tolerance": "0.05 kg / BMI unit / percentage point; equality at display precision",
            "cycle_assignment_note": "Matching values alone do not uniquely associate similar cycles; check times",
            "cycles": comparisons}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("capture", type=Path)
    parser.add_argument("--observations", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args(argv)
    try:
        events = [json.loads(line) for line in args.capture.read_text().splitlines() if line.strip()]
        report = decode_events(events)
        report["source_capture"] = str(args.capture)
        if args.observations:
            observations = read_observations(args.observations)
            report["local_observations"] = observations
            report["observation_comparison"] = compare_observations(report["records"], observations)
        output = args.output or ROOT / "database/analysis" / (
            args.capture.stem + "-" + datetime.now().strftime("%Y%m%dT%H%M%S-%f") + ".json")
        output.parent.mkdir(parents=True, exist_ok=True)
        with output.open("x") as f:
            json.dump(report, f, indent=2)
            f.write("\n")
    except (OSError, ValueError, KeyError):
        print("Offline analysis failed: check input format/layout or choose an unused output filename. No health values printed.")
        return 1
    print(f"Decoded {len(report['records'])} weight records; warnings: {len(report['warnings'])}.")
    if args.observations:
        checks = report["observation_comparison"]["cycles"]
        print("Display comparisons agreeing:", sum(c["all_five_metrics_agree"] for c in checks), "of", len(checks))
        print("Unique cycle assignments:", sum(c["unique_cycle_assignment"] for c in checks))
    print(f"Private local report: {output}")
    return 2 if report["warnings"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
