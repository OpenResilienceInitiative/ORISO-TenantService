import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    "native_memory_attribution", Path(__file__).parents[1] / "test-bootstrap-context-roundtrip.py")
proof = importlib.util.module_from_spec(spec)
spec.loader.exec_module(proof)


class OwnedNativeMemoryAttributionControls(unittest.TestCase):
    def test_owned_cgroup_snapshot_retains_only_numeric_memory_fields(self):
        identifier = "a" * 64
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            proc, cgroup = root / "proc", root / "cgroup"
            (proc / "123").mkdir(parents=True)
            (proc / "123" / "cgroup").write_text("0::/system.slice/docker-" + identifier + ".scope\n")
            memory = cgroup / "system.slice" / ("docker-" + identifier + ".scope")
            memory.mkdir(parents=True)
            for name, value in {"current": "7", "peak": "9", "max": "10",
                                "events": "low 0\nhigh 0\nmax 6\noom 0\noom_kill 0\n"}.items():
                (memory / ("memory." + name)).write_text(value)
            class Owned:
                label_key, run_id = "owned-run", "current"
                def inspect(self, kind, name):
                    return {"Id": identifier, "Config": {"Labels": {"owned-run": "current"}},
                            "State": {"Pid": 123}}
            snapshot = proof.owned_memory_snapshot(Owned(), "owned", proc, cgroup)
            self.assertEqual({"available": True, "currentBytes": 7, "peakBytes": 9,
                              "limitBytes": 10, "events": {"low": 0, "high": 0, "max": 6,
                                                             "oom": 0, "oom_kill": 0}}, snapshot)

    def test_foreign_ownership_is_refused_before_proc_or_cgroup_read(self):
        class Foreign:
            label_key, run_id = "owned-run", "current"
            def inspect(self, kind, name):
                return {"Config": {"Labels": {"owned-run": "other"}}}
        with patch.object(Path, "read_text", side_effect=AssertionError("No foreign reads")):
            self.assertEqual({"available": False, "failureType": "RuntimeError"},
                             proof.owned_memory_snapshot(Foreign(), "owned"))

    def test_wrong_cgroup_identity_cannot_be_reported_as_the_owned_peak(self):
        class Owned:
            label_key, run_id = "owned-run", "current"
            def inspect(self, kind, name):
                return {"Id": "a" * 64, "Config": {"Labels": {"owned-run": "current"}},
                        "State": {"Pid": 123}}
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "123").mkdir()
            (root / "123" / "cgroup").write_text("0::/system.slice/other.service\n")
            self.assertEqual({"available": False, "failureType": "RuntimeError"},
                             proof.owned_memory_snapshot(Owned(), "owned", root, root))


if __name__ == "__main__":
    unittest.main()
