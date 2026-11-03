import importlib.util
import unittest
from datetime import date
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "verify-trivy-policy.py"

spec = importlib.util.spec_from_file_location("trivy_policy", SCRIPT)
trivy_policy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(trivy_policy)


def make_report(fixed_version=""):
    return {
        "Results": [
            {
                "Class": "os-pkgs",
                "Type": "debian",
                "Vulnerabilities": [
                    {
                        "VulnerabilityID": "CVE-2026-76642",
                        "PkgName": "libuuid1",
                        "InstalledVersion": "2.41.5-0+deb13u1",
                        "FixedVersion": fixed_version,
                        "Severity": "HIGH",
                    }
                ],
            }
        ]
    }


def make_exception(**overrides):
    exception = {
        "cve": "CVE-2026-76642",
        "package": "libuuid1",
        "installed_version": "2.41.5-0+deb13u1",
        "severity": "HIGH",
        "reason": "No fix available",
        "owner": "security-team",
        "reference": "SEC-123",
        "expires": "2026-12-03",
    }
    exception.update(overrides)
    return exception


def make_policy(exceptions=None):
    return {
        "version": 1,
        "policy": {
            "blocking_severities": ["HIGH", "CRITICAL"],
        },
        "exceptions": exceptions or [],
    }


class TrivyPolicyTests(unittest.TestCase):

    TODAY = date(2026, 11, 3)

    def test_unfixed_high_vulnerability_is_blocked(self):
        violations = trivy_policy.validate(
            make_report(), make_policy(), self.TODAY
        )
        self.assertEqual(len(violations), 1)

    def test_matching_valid_exception_is_accepted(self):
        violations = trivy_policy.validate(
            make_report(),
            make_policy([make_exception()]),
            self.TODAY,
        )
        self.assertEqual(violations, [])

    def test_expired_exception_is_rejected(self):
        exception = make_exception(expires="2026-11-02")

        violations = trivy_policy.validate(
            make_report(), make_policy([exception]), self.TODAY
        )
        self.assertEqual(len(violations), 1)

    def test_available_fix_invalidates_exception(self):
        violations = trivy_policy.validate(
            make_report("2.41.5-0+deb13u2"),
            make_policy([make_exception()]),
            self.TODAY,
        )
        self.assertEqual(len(violations), 1)

    def test_medium_vulnerability_is_not_blocked(self):
        report = make_report()
        report["Results"][0]["Vulnerabilities"][0]["Severity"] = "MEDIUM"

        violations = trivy_policy.validate(
            report, make_policy(), self.TODAY
        )
        self.assertEqual(violations, [])

    def test_exception_for_another_version_does_not_apply(self):
        exception = make_exception(
            installed_version="2.41.5-0+deb13u2"
        )

        violations = trivy_policy.validate(
            make_report(), make_policy([exception]), self.TODAY
        )
        self.assertEqual(len(violations), 1)

    def test_exception_without_reason_is_rejected(self):
        exception = make_exception()
        del exception["reason"]

        with self.assertRaisesRegex(ValueError, "missing fields"):
            trivy_policy.validate_policy(make_policy([exception]))

    def test_exception_with_invalid_date_is_rejected(self):
        exception = make_exception(expires="not-a-date")

        with self.assertRaisesRegex(ValueError, "invalid expiry date"):
            trivy_policy.validate_policy(make_policy([exception]))

    def test_duplicate_exceptions_are_rejected(self):
        exception = make_exception()

        with self.assertRaisesRegex(ValueError, "duplicate exception"):
            trivy_policy.validate_policy(
                make_policy([exception, exception])
            )

    def test_empty_exception_reason_is_rejected(self):
        exception = make_exception(reason=" ")

        with self.assertRaisesRegex(ValueError, "invalid or empty"):
            trivy_policy.validate_policy(make_policy([exception]))

    def test_critical_vulnerability_is_blocked(self):
        report = make_report()
        report["Results"][0]["Vulnerabilities"][0]["Severity"] = "CRITICAL"

        violations = trivy_policy.validate(
            report, make_policy(), self.TODAY
        )
        self.assertEqual(len(violations), 1)

    def test_invalid_report_is_not_treated_as_clean(self):
        with self.assertRaisesRegex(ValueError, "Invalid Trivy report"):
            trivy_policy.validate(
                {"Results": None}, make_policy(), self.TODAY
            )

    def test_high_exception_does_not_cover_critical(self):
        exception = make_exception(severity="HIGH")
        report = make_report()
        report["Results"][0]["Vulnerabilities"][0]["Severity"] = "CRITICAL"

        violations = trivy_policy.validate(
            report, make_policy([exception]), self.TODAY
        )
        self.assertEqual(len(violations), 1)

    def test_exception_severity_must_be_blocking(self):
        exception = make_exception(severity="MEDIUM")

        with self.assertRaisesRegex(ValueError, "invalid severity"):
            trivy_policy.validate_policy(make_policy([exception]))


if __name__ == "__main__":
    unittest.main(verbosity=2)