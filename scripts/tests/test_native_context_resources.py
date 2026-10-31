"""Finite, synthetic resource checks; these tests start no Docker or JVM process."""
import importlib.util
import signal
from pathlib import Path
import tempfile
import unittest
import copy
import json
import xml.etree.ElementTree as ET
from unittest.mock import Mock, patch

MODULE = Path(__file__).resolve().parents[1] / "native_context_resources.py"
spec = importlib.util.spec_from_file_location("native_context_resources", MODULE)
resources = importlib.util.module_from_spec(spec)
spec.loader.exec_module(resources)
MIB = 1024 * 1024


class MemoryTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.proc = self.root / "proc"
        self.cg = self.root / "cgroup"
        (self.proc / "self").mkdir(parents=True)
        (self.proc / "pressure").mkdir()
        (self.cg / "job").mkdir(parents=True)
        (self.cg / "cgroup.controllers").write_text("memory cpu\n")
        (self.proc / "self/cgroup").write_text("0::/job\n")
        self.memory(8192, 7168)
        (self.proc / "pressure/memory").write_text(
            "some avg10=0.00 avg60=0.00 avg300=0.00 total=0\n"
            "full avg10=0.00 avg60=0.00 avg300=0.00 total=0\n")
        for p in [self.cg, self.cg / "job"]:
            (p / "memory.max").write_text("max\n")
            (p / "memory.current").write_text(str(1024 * MIB))
            (p / "memory.events").write_text(
                "low 0\nhigh 0\nmax 0\noom 0\noom_kill 0\noom_group_kill 0\n")

    def memory(self, total, available, swap_used=0):
        (self.proc / "meminfo").write_text(
            f"MemTotal: {total * 1024} kB\nMemAvailable: {available * 1024} kB\n"
            f"SwapTotal: 1048576 kB\nSwapFree: {(1024 - swap_used) * 1024} kB\n")

    def sample(self):
        return resources.linux_memory(self.proc, self.cg)

    def test_real_linux_shapes_accept_a_budget_with_host_and_cgroup_reserves(self):
        (self.cg / "job/memory.max").write_text(str(6144 * MIB))
        sample = self.sample()
        resources.require_memory(sample, 3184, starting=True)
        self.assertEqual(2, len(sample["cgroups"]))
        self.assertEqual(7168 * MIB, sample["availableBytes"])

    def test_start_requires_forty_percent_even_when_small_budget_fits(self):
        self.memory(8192, 3000)
        with self.assertRaises(resources.ResourceRefusal):
            resources.require_memory(self.sample(), 100, starting=True)

    def test_full_budget_must_fit_while_retaining_twenty_five_percent(self):
        self.memory(8192, 5000)
        with self.assertRaises(resources.ResourceRefusal):
            resources.require_memory(self.sample(), 3184, starting=True)

    def test_smaller_ancestor_limit_is_checked_even_when_host_is_free(self):
        (self.cg / "memory.max").write_text(str(4096 * MIB))
        with self.assertRaises(resources.ResourceRefusal):
            resources.require_memory(self.sample(), 3184, starting=True)

    def test_monitor_keeps_floor_without_reserving_the_whole_budget_again(self):
        self.memory(8192, 2500)
        resources.require_memory(self.sample(), 3184, starting=False)
        self.memory(8192, 2000)
        with self.assertRaises(resources.ResourceRefusal):
            resources.require_memory(self.sample(), 3184, starting=False)

    def test_swap_or_stalled_memory_or_oom_cannot_be_hidden_by_free_percentage(self):
        self.memory(8192, 7168, swap_used=1)
        with self.assertRaises(resources.ResourceRefusal):
            resources.require_memory(self.sample(), 3184, starting=True)
        self.memory(8192, 7168)
        (self.proc / "pressure/memory").write_text(
            "some avg10=0.00 avg60=0.00 avg300=0.00 total=0\n"
            "full avg10=0.01 avg60=0.00 avg300=0.00 total=3\n")
        with self.assertRaises(resources.ResourceRefusal):
            resources.require_memory(self.sample(), 3184, starting=True)
        (self.proc / "pressure/memory").write_text(
            "some avg10=0.00 avg60=0.00 avg300=0.00 total=0\n"
            "full avg10=0.00 avg60=0.00 avg300=0.00 total=3\n")
        (self.cg / "job/memory.events").write_text("high 0\nmax 0\noom 1\noom_kill 1\n")
        with self.assertRaises(resources.ResourceRefusal):
            resources.require_memory(self.sample(), 3184, starting=False)

    def test_missing_pressure_or_current_or_events_is_a_refusal(self):
        for p in [self.proc / "pressure/memory", self.cg / "job/memory.current",
                  self.cg / "job/memory.events"]:
            original = p.read_bytes()
            p.unlink()
            with self.assertRaises(resources.ResourceRefusal):
                self.sample()
            p.write_bytes(original)

    def test_cgroup_traversal_or_missing_v2_is_a_refusal(self):
        (self.proc / "self/cgroup").write_text("0::/../../elsewhere\n")
        with self.assertRaises(resources.ResourceRefusal):
            self.sample()
        (self.proc / "self/cgroup").write_text("5:memory:/job\n")
        with self.assertRaises(resources.ResourceRefusal):
            self.sample()

    def test_kernel_root_without_nonroot_limit_files_requires_the_actual_root_mount(self):
        for name in ("memory.max", "memory.current", "memory.events"):
            (self.cg / name).unlink()
        (self.proc / "self/mountinfo").write_text(
            f"31 24 0:28 / {self.cg.resolve()} rw - cgroup2 cgroup rw\n")
        resources.require_memory(self.sample(), 3184, starting=True)
        (self.proc / "self/mountinfo").write_text(
            f"31 24 0:28 /restricted {self.cg.resolve()} rw - cgroup2 cgroup rw\n")
        with self.assertRaises(resources.ResourceRefusal):
            self.sample()

    def lease_samples(self, totals):
        initial = self.sample()
        lease = resources.MemoryLease(3184, self.root / "observations.jsonl")
        samples = [{**initial, "fullPsiTotal": total} for total in totals]
        return lease, samples

    def test_full_stall_allowance_is_one_millisecond_for_the_entire_lease(self):
        lease, samples = self.lease_samples([0, 1, 1000, 1001])
        with patch.object(resources, "machine_memory", side_effect=samples):
            lease.check(starting=True)
            lease.check()
            lease.check()
            with self.assertRaises(resources.ResourceRefusal):
                lease.check()
        audit = [json.loads(line) for line in lease.observations.read_text().splitlines()]
        self.assertEqual([0, 1, 1000, 1001], [row["fullPsiLeaseDeltaUs"] for row in audit])
        self.assertTrue(all(row["fullPsiLeaseBaselineUs"] == 0 for row in audit))
        self.assertTrue(all(row["fullPsiLeaseCeilingUs"] == 1000 for row in audit))

    def test_full_stall_allowance_uses_nonzero_initial_baseline_not_absolute_total(self):
        lease, samples = self.lease_samples([9000000, 9000001, 9001000])
        with patch.object(resources, "machine_memory", side_effect=samples):
            lease.check(starting=True)
            lease.check()
            lease.check()
        last = json.loads(lease.observations.read_text().splitlines()[-1])
        self.assertEqual(9000000, last["fullPsiLeaseBaselineUs"])
        self.assertEqual(1000, last["fullPsiLeaseDeltaUs"])

    def test_small_intervals_cannot_reset_the_total_stall_allowance(self):
        lease, samples = self.lease_samples([100, 700, 1200])
        with patch.object(resources, "machine_memory", side_effect=samples):
            lease.check(starting=True)
            lease.check()
            with self.assertRaises(resources.ResourceRefusal):
                lease.check()

    def test_counter_reset_refuses_even_when_still_above_initial_baseline(self):
        lease, samples = self.lease_samples([100, 600, 500])
        with patch.object(resources, "machine_memory", side_effect=samples):
            lease.check(starting=True)
            lease.check()
            with self.assertRaises(resources.ResourceRefusal):
                lease.check()

    def test_missing_or_noninteger_or_negative_full_total_is_not_zero(self):
        for value in [None, True, False, -1, 1.0, "1"]:
            with self.subTest(value=value):
                lease, samples = self.lease_samples([value])
                with patch.object(resources, "machine_memory", return_value=samples[0]):
                    with self.assertRaises(resources.ResourceRefusal):
                        lease.check(starting=True)
        lease, samples = self.lease_samples([0])
        del samples[0]["fullPsiTotal"]
        with patch.object(resources, "machine_memory", return_value=samples[0]):
            with self.assertRaises(resources.ResourceRefusal):
                lease.check(starting=True)

    def test_positive_average_still_refuses_within_cumulative_allowance(self):
        lease, samples = self.lease_samples([100, 101])
        samples[1]["fullPsiAvg10"] = 0.01
        with patch.object(resources, "machine_memory", side_effect=samples):
            lease.check(starting=True)
            with self.assertRaises(resources.ResourceRefusal):
                lease.check()


class CleanupTests(unittest.TestCase):
    def setUp(self):
        self.objects = {
            ("container", "owned"): {"Id": "owned-id", "Config": {"Labels": {"run": "ours"}},
                                     "State": {"OOMKilled": False}, "RestartCount": 0,
                                     "Mounts": [{"Type": "volume", "Name": "anonymous"}]},
            ("network", "net"): {"Labels": {"run": "ours"}},
            ("volume", "anonymous"): {},
        }
        self.calls = []

        def inspect(kind, name):
            return self.objects.get((kind, name))

        def command(*args):
            self.calls.append(args)
            if args[:3] == ("rm", "-f", "-v"):
                self.objects.pop(("container", args[3]))
                self.objects.pop(("volume", "anonymous"))
            elif args[:2] == ("network", "rm"):
                self.objects.pop(("network", args[2]))

        self.owned = resources.OwnedDocker("run", "ours", command=command, inspect=inspect)

    def test_uncertain_create_response_is_cleaned_by_preplanned_exact_name(self):
        self.owned.plan("network", "net")
        self.owned.plan("container", "owned")
        receipt = self.owned.cleanup()
        self.assertEqual([("rm", "-f", "-v", "owned"), ("network", "rm", "net")], self.calls)
        self.assertEqual(2, len(receipt))
        self.assertTrue(all(row["confirmedAbsent"] for row in receipt))

    def test_foreign_label_refuses_removal_but_other_owned_resources_are_cleaned(self):
        self.objects[("container", "owned")]["Config"]["Labels"]["run"] = "peer"
        self.owned.plan("network", "net")
        self.owned.plan("container", "owned")
        with self.assertRaises(resources.CleanupRefusal) as failed:
            self.owned.cleanup()
        self.assertEqual([("network", "rm", "net")], self.calls)
        self.assertTrue(failed.exception.errors)

    def test_confirmed_missing_preplanned_resources_are_safe(self):
        self.owned.plan("container", "never-created")
        self.assertTrue(self.owned.cleanup()[0]["confirmedAbsent"])
        self.assertEqual([], self.calls)

    def test_unknown_inspection_or_surviving_volume_never_claims_cleanup(self):
        self.owned.plan("container", "owned")
        self.owned.command = lambda *args: self.objects.pop(("container", "owned"))
        with self.assertRaises(resources.CleanupRefusal):
            self.owned.cleanup()
        self.owned.inspect = lambda *args: (_ for _ in ()).throw(RuntimeError("daemon unavailable"))
        with self.assertRaises(resources.CleanupRefusal):
            self.owned.cleanup()

    def test_owned_oom_is_removed_but_never_accepted_as_successful_resource_cleanup(self):
        self.objects[("container", "owned")]["State"]["OOMKilled"] = True
        self.owned.plan("container", "owned")
        with self.assertRaises(resources.CleanupRefusal):
            self.owned.cleanup()
        self.assertNotIn(("container", "owned"), self.objects)


class ProcessCleanupTests(unittest.TestCase):
    def test_exited_leader_does_not_leave_its_owned_session_descendants(self):
        runner_spec = importlib.util.spec_from_file_location(
            "native_bootstrap_runner", MODULE.with_name("test-bootstrap-context-roundtrip.py"))
        runner = importlib.util.module_from_spec(runner_spec)
        runner_spec.loader.exec_module(runner)
        process = Mock(pid=41971)
        process.poll.return_value = 0
        process.wait.return_value = 0
        alive = [True]
        calls = []

        def group(pid, sig):
            self.assertEqual(41971, pid)
            if not alive[0]:
                raise ProcessLookupError()
            calls.append(sig)
            if sig == signal.SIGTERM:
                alive[0] = False

        with patch.object(runner.os, "killpg", side_effect=group):
            runner.stop(process)
        self.assertIn(signal.SIGTERM, calls)
        self.assertFalse(alive[0])


class CounterfactualTests(unittest.TestCase):
    def setUp(self):
        runner_spec = importlib.util.spec_from_file_location(
            "native_bootstrap_classifier", MODULE.with_name("test-bootstrap-context-roundtrip.py"))
        self.runner = importlib.util.module_from_spec(runner_spec)
        runner_spec.loader.exec_module(self.runner)
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        source = self.root / "src/test/java/com/vi/tenantservice/api/controller/BootstrapV2CallerReceiverIT.java"
        source.parent.mkdir(parents=True)
        source.write_text("assertThat(RECEIVER_STATUS.get()).isEqualTo(200);\n")
        self.method = "ownedTenantCreationUsesTheSharedV2ProofAtTheMaterializedDtoSendSeam"
        self.tree = ET.Element("testsuite")
        self.case = ET.SubElement(self.tree, "testcase", {
            "classname": "com.vi.tenantservice.api.controller.BootstrapV2CallerReceiverIT",
            "name": self.method})
        self.failure = ET.SubElement(self.case, "failure")
        self.failure.text = ("expected: 200\n but was: 403\n at BootstrapV2CallerReceiverIT."
                             + self.method + "(BootstrapV2CallerReceiverIT.java:1)")
        self.counts = {"tests": 1, "failures": 1, "errors": 0, "skipped": 0}
        state = {key: {"count": 0, "sha256": "a" * 64} for key in ("claims", "counter", "types")}
        self.receipt = {"observed": {"receiverStatus": 403, "receiverRequestObserved": True,
                                    "allocationStatus": "RESERVED", "tenantStatus": 500},
                        "receiverBefore": state, "receiverAfter": copy.deepcopy(state)}

    def accepts(self):
        return self.runner.intended_counterfactual(self.tree, self.counts, 1, self.receipt, self.root)

    def test_separate_receiver_frames_survive_without_logging_exception_values(self):
        log = self.root / "receiver.log"
        log.write_text("java.lang.IllegalStateException: synthetic-sensitive-marker\n"
                       "\tat de.caritas.cob.consultingtypeservice.config.security.TenantCreationContext.require(TenantCreationContext.java:91)\n"
                       "Caused by: org.springframework.web.client.HttpServerErrorException: synthetic-sensitive-marker\n")
        result = self.runner.safe_java_frames(log)
        self.assertEqual(91, result["applicationFrames"][0]["line"])
        self.assertIn("java.lang.IllegalStateException", result["exceptionTypes"])
        self.assertNotIn("synthetic-sensitive-marker", json.dumps(result))

    def test_exact_executed_failure_requires_the_actual_source_assertion(self):
        self.assertTrue(self.accepts())
        original = self.failure.text
        for message in ("an unrelated assertion", original.replace("java:1)", "java:2)"),
                        original.replace("403", "401")):
            with self.subTest(message=message):
                self.failure.text = message
                self.assertFalse(self.accepts())

    def test_no_other_test_outcome_can_be_credited_as_the_counterfactual(self):
        for key in ("tests", "failures", "errors", "skipped"):
            with self.subTest(key=key):
                original = self.counts[key]
                self.counts[key] += 1
                self.assertFalse(self.accepts())
                self.counts[key] = original
        self.case.set("name", "anotherTest")
        self.assertFalse(self.accepts())

    def test_no_write_and_reservation_compensation_are_required_not_assumed(self):
        original = copy.deepcopy(self.receipt)
        for key in ("claims", "counter", "types"):
            with self.subTest(collection=key):
                self.receipt = copy.deepcopy(original)
                self.receipt["receiverAfter"][key]["sha256"] = "b" * 64
                self.assertFalse(self.accepts())
        for key, value in (("allocationStatus", "ASSIGNED"), ("receiverRequestObserved", False),
                           ("receiverStatus", 200), ("tenantStatus", 200)):
            with self.subTest(observation=key):
                self.receipt = copy.deepcopy(original)
                self.receipt["observed"][key] = value
                self.assertFalse(self.accepts())


class FinalRuntimeAcceptanceTests(unittest.TestCase):
    def test_four_distinct_owned_process_groups_must_be_known_absent(self):
        wrapper_spec = importlib.util.spec_from_file_location(
            "native_process_wrapper", MODULE.parent / "ci/scenario-c-context-native.py")
        wrapper = importlib.util.module_from_spec(wrapper_spec)
        wrapper_spec.loader.exec_module(wrapper)
        groups = [{"processGroup": number, "confirmedAbsent": True} for number in (101, 102, 103)]
        server = {"processGroup": 104, "confirmedAbsent": True}
        self.assertTrue(wrapper.known_process_cleanup(groups, server))
        for invalid in ({}, {"processGroup": 103, "confirmedAbsent": True},
                        {"processGroup": 104, "confirmedAbsent": False},
                        {"processGroup": True, "confirmedAbsent": True},
                        {"processGroup": 0, "confirmedAbsent": True}):
            self.assertFalse(wrapper.known_process_cleanup(groups, invalid))
        self.assertFalse(wrapper.known_process_cleanup(groups[:-1], server))
        self.assertFalse(wrapper.known_process_cleanup(groups + groups[:1], server))

    def test_early_diagnostics_never_copy_provider_messages_or_source_values(self):
        wrapper_spec = importlib.util.spec_from_file_location(
            "native_early_diagnostic_wrapper", MODULE.parent / "ci/scenario-c-context-native.py")
        wrapper = importlib.util.module_from_spec(wrapper_spec)
        wrapper_spec.loader.exec_module(wrapper)
        with tempfile.TemporaryDirectory() as temporary:
            log = Path(temporary) / "early.log"
            log.write_text('  File "/public/scripts/_keycloak_container.py", line 21, in <module>\n'
                           '    private_value = "synthetic-sensitive-marker"\n'
                           'ModuleNotFoundError: No module named \'yaml\'\n')
            result = wrapper.safe_early_failure(log)
            self.assertEqual("ModuleNotFoundError", result["failureType"])
            self.assertEqual("PyYAML", result["missingDeclaredDependency"])
            self.assertEqual([{ "file": "_keycloak_container.py", "line": 21,
                                "function": "<module>"}], result["frames"])
            self.assertNotIn("synthetic-sensitive-marker", json.dumps(result))
            log.write_text("urllib.error.HTTPError: synthetic-sensitive-marker\n")
            self.assertNotIn("synthetic-sensitive-marker", json.dumps(wrapper.safe_early_failure(log)))

    def test_safe_cleanup_absence_cannot_substitute_for_known_container_oom_state(self):
        wrapper_spec = importlib.util.spec_from_file_location(
            "native_bootstrap_wrapper", MODULE.parent / "ci/scenario-c-context-native.py")
        wrapper = importlib.util.module_from_spec(wrapper_spec)
        wrapper_spec.loader.exec_module(wrapper)
        good = [{"kind": "container", "runtime": {"oomKilled": False, "restartCount": 0}},
                {"kind": "container", "runtime": {"oomKilled": False, "restartCount": 0}},
                {"kind": "network", "runtime": None}]
        self.assertTrue(wrapper.known_container_runtime(good))
        for invalid in (None, {}, {"oomKilled": True, "restartCount": 0},
                        {"oomKilled": False, "restartCount": 1},
                        {"oomKilled": False, "restartCount": False}):
            with self.subTest(runtime=invalid):
                changed = copy.deepcopy(good)
                changed[0]["runtime"] = invalid
                self.assertFalse(wrapper.known_container_runtime(changed))
        self.assertFalse(wrapper.known_container_runtime(good[1:]))


if __name__ == "__main__":
    unittest.main()
