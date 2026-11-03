#!/usr/bin/env python3
"""Validate a Trivy report against the repository security policy."""

import argparse
import json
import sys
from datetime import date
from pathlib import Path


BLOCKABLE_SEVERITIES = {"HIGH", "CRITICAL"}

REQUIRED_EXCEPTION_FIELDS = {
    "cve",
    "package",
    "installed_version",
    "severity",
    "reason",
    "owner",
    "reference",
    "expires",
}


def load_json(path):
    with Path(path).open(encoding="utf-8") as file:
        return json.load(file)


def validate_policy(policy):
    if not isinstance(policy, dict):
        raise ValueError("Invalid policy: expected an object")

    if policy.get("version") != 1:
        raise ValueError("Unsupported policy version")

    rules = policy.get("policy")

    if not isinstance(rules, dict):
        raise ValueError("Missing or invalid policy section")

    severities = rules.get("blocking_severities")

    if (
        not isinstance(severities, list)
        or not severities
        or len(severities) != len(set(severities))
        or any(
            not isinstance(severity, str)
            or severity not in BLOCKABLE_SEVERITIES
            for severity in severities
        )
    ):
        raise ValueError("Invalid blocking_severities")

    exceptions = policy.get("exceptions")

    if not isinstance(exceptions, list):
        raise ValueError("Exceptions must be a list")

    seen = set()

    for index, exception in enumerate(exceptions):
        if not isinstance(exception, dict):
            raise ValueError(f"Exception {index}: must be an object")

        missing = REQUIRED_EXCEPTION_FIELDS - exception.keys()

        if missing:
            raise ValueError(
                f"Exception {index}: missing fields {sorted(missing)}"
            )

        for field in REQUIRED_EXCEPTION_FIELDS:
            value = exception[field]

            if not isinstance(value, str) or not value.strip():
                raise ValueError(
                    f"Exception {index}: invalid or empty field '{field}'"
                )

        if exception["severity"] not in BLOCKABLE_SEVERITIES:
            raise ValueError(f"Exception {index}: invalid severity")

        if exception["severity"] not in severities:
            raise ValueError(
                f"Exception {index}: severity is not configured as blocking"
            )

        try:
            expiry = date.fromisoformat(exception["expires"])
        except ValueError as error:
            raise ValueError(
                f"Exception {index}: invalid expiry date"
            ) from error

        if expiry.isoformat() != exception["expires"]:
            raise ValueError(
                f"Exception {index}: expiry must use YYYY-MM-DD format"
            )

        key = (
            exception["cve"],
            exception["package"],
            exception["installed_version"],
            exception["severity"],
        )

        if key in seen:
            raise ValueError(f"Exception {index}: duplicate exception {key}")

        seen.add(key)


def validate(report, policy, today=None):
    if not isinstance(report, dict):
        raise ValueError("Invalid Trivy report: expected an object")

    results = report.get("Results")

    if not isinstance(results, list):
        raise ValueError("Invalid Trivy report: Results must be a list")

    today = today or date.today()
    blocking = set(policy["policy"]["blocking_severities"])
    exceptions = policy["exceptions"]
    violations = []

    for result in results:
        if not isinstance(result, dict):
            raise ValueError(
                "Invalid Trivy report: result must be an object"
            )

        vulnerabilities = result.get("Vulnerabilities")

        if vulnerabilities is None:
            continue

        if not isinstance(vulnerabilities, list):
            raise ValueError(
                "Invalid Trivy report: Vulnerabilities must be a list"
            )

        for vulnerability in vulnerabilities:
            if not isinstance(vulnerability, dict):
                raise ValueError(
                    "Invalid Trivy report: vulnerability must be an object"
                )

            severity = vulnerability.get("Severity")

            if not isinstance(severity, str):
                raise ValueError(
                    "Invalid Trivy report: missing or invalid Severity"
                )

            severity = severity.upper()

            if severity not in blocking:
                continue

            cve = vulnerability.get("VulnerabilityID")
            package = vulnerability.get("PkgName")
            version = vulnerability.get("InstalledVersion")
            fixed_version = vulnerability.get("FixedVersion") or ""

            if not all(
                isinstance(value, str) and value.strip()
                for value in (cve, package, version)
            ):
                raise ValueError(
                    "Invalid Trivy report: incomplete vulnerability metadata"
                )

            if not isinstance(fixed_version, str):
                raise ValueError(
                    "Invalid Trivy report: invalid FixedVersion"
                )

            accepted = False

            for exception in exceptions:
                matches = (
                    exception["cve"] == cve
                    and exception["package"] == package
                    and exception["installed_version"] == version
                    and exception["severity"] == severity
                )

                if not matches:
                    continue

                expiry = date.fromisoformat(exception["expires"])

                if expiry >= today and not fixed_version.strip():
                    accepted = True
                    break

            if not accepted:
                violations.append({
                    "cve": cve,
                    "package": package,
                    "version": version,
                    "severity": severity,
                    "fixed_version": fixed_version,
                })

    return violations


def main():
    parser = argparse.ArgumentParser(
        description="Check Trivy vulnerabilities against the repository policy."
    )

    parser.add_argument("--report", required=True, type=Path)

    parser.add_argument(
        "--policy",
        type=Path,
        default=Path(".github/security/trivy-policy.json"),
    )

    args = parser.parse_args()

    try:
        report = load_json(args.report)
        policy = load_json(args.policy)

        validate_policy(policy)

        violations = validate(report, policy)

        if violations:
            print(
                f"FAIL: {len(violations)} blocking vulnerability(ies):"
            )

            for item in violations:
                print(
                    f"- {item['severity']} {item['cve']} "
                    f"{item['package']}@{item['version']} "
                    f"(fixed: {item['fixed_version'] or 'unknown'})"
                )

            return 1

        print("PASS: no blocking vulnerabilities.")
        return 0

    except (OSError, json.JSONDecodeError, KeyError, ValueError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())