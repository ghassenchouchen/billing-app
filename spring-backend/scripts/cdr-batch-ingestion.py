#!/usr/bin/env python3
"""
CDR Batch Script

Reads a CSV file containing Call Detail Records (CDRs),
parses them, and sends them to the Usage Service REST API
in configurable batch sizes.


Usage:
  python cdr-batch-ingestion.py <csv_file> [options]

Examples:
  python cdr-batch-ingestion.py cdrs-20260212.csv
  python cdr-batch-ingestion.py cdrs.csv --api-url http://localhost:8084 --batch-size 50
"""

import argparse
import csv
import json
import sys
import uuid
import os
from datetime import datetime
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

DEFAULT_API_URL = "http://localhost:8084"
DEFAULT_BATCH_SIZE = 100
DEFAULT_CDR_SOURCE = "batch-script"
DEFAULT_USAGE_TYPE = "VOICE"
DEFAULT_UNIT = "seconds"


def parse_row(row: dict, cdr_source: str, row_num: int) -> dict | None:
    """Convert one CSV row into a CdrIngestionRequest JSON payload."""
    try:
        external_id = row.get("external_id", "").strip()
        abonnement_id = row.get("abonnement_id", "").strip()
        service_id = row.get("service_id", "").strip()
        quantity = row.get("quantity", "").strip()
        date_usage = row.get("date_usage", "").strip()

        if not all([external_id, abonnement_id, service_id, quantity, date_usage]):
            print(f"  [SKIP] Row {row_num}: missing required field(s)")
            return None

        # Normalise the date to ISO format 
        timestamp = normalise_date(date_usage)
        if timestamp is None:
            print(f"  [SKIP] Row {row_num}: unparseable date '{date_usage}'")
            return None

        return {
            "sessionId": external_id,
            "subscriptionId": int(abonnement_id),
            "serviceId": int(service_id),
            "usageType": row.get("usage_type", DEFAULT_USAGE_TYPE).strip() or DEFAULT_USAGE_TYPE,
            "quantity": float(quantity),
            "unit": row.get("unit", DEFAULT_UNIT).strip() or DEFAULT_UNIT,
            "timestamp": timestamp,
            "cdrSource": row.get("cdr_source", cdr_source).strip() or cdr_source,
            "calledNumber": row.get("called_number", "").strip() or None,
            "callingNumber": row.get("calling_number", "").strip() or None,
            "cellId": row.get("cell_id", "").strip() or None,
            "rawCdrData": json.dumps(row),
            "correlationId": None,
        }
    except (ValueError, KeyError) as exc:
        print(f"  [SKIP] Row {row_num}: {exc}")
        return None


SUPPORTED_DATE_FORMATS = [
    "%Y-%m-%dT%H:%M:%S",
    "%Y-%m-%d %H:%M:%S",
    "%Y-%m-%d",
    "%d/%m/%Y %H:%M:%S",
    "%d/%m/%Y",
]


def normalise_date(raw: str) -> str | None:
    """Try several date formats and return ISO string or None."""
    for fmt in SUPPORTED_DATE_FORMATS:
        try:
            dt = datetime.strptime(raw, fmt)
            return dt.strftime("%Y-%m-%dT%H:%M:%S")
        except ValueError:
            continue
    return None


def post_json(url: str, payload, correlation_id: str, timeout: int = 30):
    """POST JSON and return (status_code, response_body)."""
    data = json.dumps(payload, default=str).encode("utf-8")
    req = Request(url, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")
    req.add_header("X-Correlation-Id", correlation_id)
    try:
        with urlopen(req, timeout=timeout) as resp:
            body = resp.read().decode("utf-8")
            return resp.status, json.loads(body) if body else {}
    except HTTPError as exc:
        body = exc.read().decode("utf-8") if exc.fp else ""
        return exc.code, json.loads(body) if body else {"error": str(exc)}
    except URLError as exc:
        return 0, {"error": str(exc.reason)}

# main logic

def ingest_csv(csv_path: str, api_url: str, batch_size: int,
               cdr_source: str, dry_run: bool = False):
    """Read CSV, batch-parse, and POST to Usage Service."""
    if not os.path.isfile(csv_path):
        print(f"ERROR: File not found: {csv_path}")
        sys.exit(1)

    correlation_id = str(uuid.uuid4())
    bulk_url = f"{api_url}/usage/cdr/bulk"

    print(f"═══════════════════════════════════════════════════════")
    print(f"  CDR Batch Ingestion")
    print(f"  File      : {csv_path}")
    print(f"  API URL   : {bulk_url}")
    print(f"  Batch size: {batch_size}")
    print(f"  CDR Source: {cdr_source}")
    print(f"  Corr. ID  : {correlation_id}")
    print(f"  Dry run   : {dry_run}")
    

    stats = {"parsed": 0, "skipped": 0, "sent": 0,
             "success": 0, "duplicate": 0, "rejected": 0, "failed": 0}

    batch: list[dict] = []

    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        row_num = 0

        for row in reader:
            row_num += 1
            payload = parse_row(row, cdr_source, row_num)
            if payload is None:
                stats["skipped"] += 1
                continue

            stats["parsed"] += 1
            batch.append(payload)

            if len(batch) >= batch_size:
                send_batch(bulk_url, batch, correlation_id, stats, dry_run)
                batch = []

        # flush remaining
        if batch:
            send_batch(bulk_url, batch, correlation_id, stats, dry_run)

    print()
    print(f"═══════════════════════════════════════════════════════")
    print(f"  RESULTS")
    print(f"  Rows parsed  : {stats['parsed']}")
    print(f"  Rows skipped : {stats['skipped']}")
    print(f"  Sent to API  : {stats['sent']}")
    print(f"   Success    : {stats['success']}")
    print(f"   Duplicate  : {stats['duplicate']}")
    print(f"   Rejected   : {stats['rejected']}")
    print(f"  Failed     : {stats['failed']}")

    if stats["failed"] > 0:
        sys.exit(1)


def send_batch(url: str, batch: list[dict], correlation_id: str,
               stats: dict, dry_run: bool):
    """Send a batch of CDRs to the bulk endpoint."""
    count = len(batch)
    stats["sent"] += count

    if dry_run:
        print(f"  [DRY-RUN] Would send {count} CDRs")
        stats["success"] += count
        return

    print(f"  Sending batch of {count} CDRs ... ", end="", flush=True)
    status, body = post_json(url, batch, correlation_id)

    if status == 200 and isinstance(body, list):
        for resp in body:
            if resp.get("duplicate"):
                stats["duplicate"] += 1
            elif resp.get("status") == "REJECTED":
                stats["rejected"] += 1
            elif resp.get("status") == "FAILED":
                stats["failed"] += 1
            else:
                stats["success"] += 1
        ok = sum(1 for r in body if not r.get("duplicate") and r.get("status") not in ("FAILED", "REJECTED"))
        dup = sum(1 for r in body if r.get("duplicate"))
        print(f"OK ({ok} success, {dup} dup)")
    elif status == 0:
        stats["failed"] += count
        print(f"CONNECTION ERROR: {body.get('error', 'unknown')}")
    else:
        stats["failed"] += count
        print(f"HTTP {status}: {json.dumps(body)[:200]}")

# CLI 

def main():
    parser = argparse.ArgumentParser(
        description="Ingest CDR CSV files into the Usage Service via REST API"
    )
    parser.add_argument("csv_file", help="Path to the CDR CSV file")
    parser.add_argument("--api-url", default=DEFAULT_API_URL,
                        help=f"Usage Service base URL (default: {DEFAULT_API_URL})")
    parser.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE,
                        help=f"Number of CDRs per bulk request (default: {DEFAULT_BATCH_SIZE})")
    parser.add_argument("--cdr-source", default=DEFAULT_CDR_SOURCE,
                        help=f"CDR source identifier (default: {DEFAULT_CDR_SOURCE})")
    parser.add_argument("--dry-run", action="store_true",
                        help="Parse CSV but do not actually send to API")
    args = parser.parse_args()

    ingest_csv(
        csv_path=args.csv_file,
        api_url=args.api_url,
        batch_size=args.batch_size,
        cdr_source=args.cdr_source,
        dry_run=args.dry_run,
    )


if __name__ == "__main__":
    main()
