#!/usr/bin/env python3
"""
Benchmark of the multi-criteria destination (md) search with the sets warmup, run-1 .. run-5.

One cycle runs the sets in this order, every set for each build before the next set:
  warmup-A, warmup-B, run-1-A, run-1-B, run-2-A, run-2-B, ... run-5-A, run-5-B
Every run is a new JVM that loads the graph and runs the set (200 local, 100 regional,
100 long_distance cases) once (`-n 1`, the SpeedTest default; `--samples` changes it). With more
than one sample the first ones warm up the JVM and only the last sample is saved. The warmup set is run like the others
but is not part of the report (it warms up the machine). Builds are interleaved so that slow drift
of the machine hits all builds alike. More cycles give more pairs for the comparison.

The graph must be built first (same graph for all builds; the serialization id must match the code
of each build). Expected results are not compared; only timing is used.

Usage:
  md_benchmark.py run --build LABEL=REPO_DIR [--build LABEL=REPO_DIR ...] [--cycles 1]
                      [--sets run-1,run-2,...] [--samples 1] [--out DIR]
  md_benchmark.py report [--out DIR]       (also done at the end of `run`)
  md_benchmark.py prepare | clean          (copy the config files into the set folders so one set
                                            can be run by hand with SpeedTest, remove them again)

  REPO_DIR is a checkout of OpenTripPlanner (any worktree) that can run SpeedTest with
  `java` (classpath from Maven, see classpath()). Results go to DIR (default ./bench-results):
    <label>__<cycle>__<set>.csv           per case: id, set, category, total ms
    timers/<label>__<cycle>__<set>.csv    per category and timer: calls, total seconds
    report.md                             the report

  The configuration files (build-config.json, otp-config.json, router-config.json,
  speed-test-config.json) are edited in this directory only. `run` copies them into the set folders
  before the first run and deletes them afterwards, also on failure (not when the process is
  killed: then run `clean`).

Report: for every category (and all) one table with every SpeedTest timer. For each build the total
time per set and the average per call; for two builds the paired difference of the totals (one
pair = same set, same cycle) with a 95% confidence interval. SpeedTest prints timer totals with
0.1 s resolution; timers with a total below 1 s per set are marked "~" and should not be trusted.
"""
import argparse, csv, glob, math, re, shutil, statistics as st, subprocess, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
SETS = [f"run-{i}" for i in range(1, 6)]
ALL_SET_DIRS = ["warmup"] + SETS  # config files are copied into all of them
CATEGORIES = ["local", "regional", "long_distance"]
CONFIG_FILES = ["build-config.json", "otp-config.json", "router-config.json",
                "speed-test-config.json"]
T95 = {1: 12.71, 2: 4.30, 3: 3.18, 4: 2.78, 5: 2.57, 6: 2.45, 7: 2.36, 8: 2.31, 9: 2.26,
       10: 2.23, 15: 2.13, 20: 2.09, 30: 2.04}

# The timers in the order of the report: (title, SpeedTest timer name)
MAIN_TIMERS = [
    ("Total (exact, per case)", None),
    ("Total", "Routing Total"),
    ("mc raptor", "Raptor Mc-DP Route"),
    ("reverse heuristic", "Raptor MinTravelDuration-Rev Route"),
    ("access & egress", "Routing AccessEgress"),
    ("precalculation", "Routing PreCalculation"),
    ("raptor total", "Routing Raptor"),
    ("itinerary creation", "Routing ItineraryCreation"),
    ("filtering", "Routing Filtering"),
    ("rendering", "Routing Rendering"),
    ("transit", "Routing Transit"),
    ("router", "Routing Router"),
    ("direct street", "Routing DirectStreet"),
    ("direct flex", "Routing DirectFlex"),
]
TIMER_LINE = re.compile(
    r"^((?:Raptor|Routing) .+?) \[(\w+)\]\s*\|\s*(\d+)\s+(\d+)\s+(\d+) ms\s+([\d']+)\s+([\d,.]+) (ms|s|min)\s*$")


def copy_configs(dirs):
    """Copy the configuration files from this directory (the root) into each directory, one level
    below the root. The graph path of speed-test-config.json is relative to the directory it is
    in, so it gets one extra "../"."""
    for d in dirs:
        for name in CONFIG_FILES:
            text = (HERE / name).read_text(encoding="utf-8")
            if name == "speed-test-config.json":
                text = text.replace('"graph": "../', '"graph": "../../')
            (Path(d) / name).write_text(text, encoding="utf-8")


def clean_configs(dirs):
    for d in dirs:
        for name in CONFIG_FILES:
            (Path(d) / name).unlink(missing_ok=True)


def parse_timers(lines):
    """Timers of the last md summary: {(category, timer): (calls, total seconds)}."""
    start = max(i for i, l in enumerate(lines) if "SUMMARY multi_criteria_destination" in l)
    out = {}
    for l in lines[start:]:
        if l.startswith("Test case ids"):
            break
        m = TIMER_LINE.match(l.rstrip())
        if m:
            total = float(m.group(7).replace(",", "."))
            total *= {"s": 1, "ms": 0.001, "min": 60}[m.group(8)]
            raw = m.group(6)  # SpeedTest prints large counts in thousands: 34' = about 34000 calls
            calls = int(raw[:-1]) * 1000 if raw.endswith("'") else int(raw)
            out[(m.group(2), m.group(1))] = (calls, total)
    return out


def git_info(repo):
    """Branch and commit of a checkout, from git (SpeedTest's own version line can be stale)."""
    def g(*args):
        r = subprocess.run(["git", "-C", str(repo), *args], capture_output=True, text=True)
        return r.stdout.strip() if r.returncode == 0 else "?"
    branch = g("branch", "--show-current") or "(detached)"
    return branch, g("log", "-1", "--format=%h %s")


def busy_processes(repos):
    """Maven or java processes that have a repo as working directory (a build there would
    delete classes under the benchmark and disturb the timing)."""
    ps = subprocess.run(["ps", "-axo", "pid=,comm="], capture_output=True, text=True).stdout
    found = []
    for line in ps.splitlines():
        pid, _, comm = line.strip().partition(" ")
        if not pid.isdigit() or not comm.strip().endswith(("java", "mvn", "mvnd")):
            continue
        out = subprocess.run(["lsof", "-a", "-p", pid, "-d", "cwd", "-Fn"],
                             capture_output=True, text=True).stdout
        cwd = next((l[1:] for l in out.splitlines() if l.startswith("n")), "")
        if any(cwd == str(Path(r).resolve()) or cwd.startswith(str(Path(r).resolve()) + "/")
               for r in repos):
            found.append(f"{pid} {comm.strip()} in {cwd}")
    return found


_CLASSPATHS = {}
RUN_TIMEOUT_S = 600     # a run is normally about 1.5 minutes
EXIT_GRACE_S = 20       # after the summary: time for the JVM to exit on its own


def classpath(repo):
    """Test classpath of the application module for this checkout. Sibling modules resolve to
    their target/classes (-am), never to ~/.m2. Also compiles the checkout."""
    repo = str(repo)
    if repo not in _CLASSPATHS:
        cp_file = Path(repo) / "application" / "target" / "md-benchmark-classpath.txt"
        r = subprocess.run(
            ["mvn", "-q", "-Dps", "-Dcs", "-DskipTests", "--projects", "application", "-am",
             "test-compile", "dependency:build-classpath", "-Dmdep.includeScope=test",
             f"-Dmdep.outputFile={cp_file}"], cwd=repo, capture_output=True, text=True)
        if r.returncode != 0 or not cp_file.exists():
            sys.exit(f"Could not build the classpath in {repo}:\n" + (r.stdout + r.stderr)[-2000:])
        app = Path(repo) / "application" / "target"
        _CLASSPATHS[repo] = ":".join(
            [str(app / "test-classes"), str(app / "classes"), cp_file.read_text().strip()])
    return _CLASSPATHS[repo]


def run_speedtest(repo, set_name, samples):
    """Run SpeedTest in a plain JVM. SpeedTest leaves a non-daemon thread (updater-ready) running,
    so the JVM may never exit: stop it GRACE seconds after the md summary is printed, and kill
    it after RUN_TIMEOUT_S in any case. Returns the output lines."""
    import threading, time
    cmd = ["java", "-Xmx8G", "-cp", classpath(repo),
           "org.opentripplanner.transit.speed_test.SpeedTest",
           f"--dir={HERE / set_name}", "-p", "md", "-n", str(samples), "-i", "10", "-0", "-T"]
    p = subprocess.Popen(cmd, cwd=repo, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                         text=True, errors="replace")
    lines, state = [], {"done": None}

    def reader():
        for l in p.stdout:
            lines.append(l.rstrip("\n"))
            if state["done"] is None and l.startswith("Test case ids"):
                state["done"] = time.time()   # last line of the md summary

    t = threading.Thread(target=reader, daemon=True)
    t.start()
    t0 = time.time()
    while p.poll() is None:
        now = time.time()
        if state["done"] and now - state["done"] > EXIT_GRACE_S:
            p.terminate()
            break
        if now - t0 > RUN_TIMEOUT_S:
            p.kill()
            lines.append(f"TIMEOUT: killed after {RUN_TIMEOUT_S} s")
            break
        time.sleep(0.5)
    try:
        p.wait(timeout=30)
    except subprocess.TimeoutExpired:
        p.kill()
        p.wait()
    t.join(timeout=10)
    return lines, p.returncode


def launch(repo, set_name, out_dir, label, cycle, samples):
    lines, rc = run_speedtest(repo, set_name, samples)
    tt = [l for l in lines if l.startswith("Total times")]
    if not tt or not any("SUMMARY multi_criteria_destination" in l for l in lines):
        sys.exit(f"SpeedTest in {repo} gave no timings (exit {rc}):\n"
                 + "\n".join(lines)[-2000:])
    cats = [r["category"] for r in csv.DictReader(open(HERE / set_name / "travelSearch.csv", encoding="utf-8"))]
    times = [int(x) for x in re.findall(r"\d+", tt[-1].split(":", 1)[1])]
    if len(times) != len(cats):
        sys.exit(f"Expected {len(cats)} timings, got {len(times)}")
    name = f"{label}__{cycle}__{set_name}.csv"
    with open(Path(out_dir) / name, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["id", "set", "category", "ms"])
        for i, (c, t) in enumerate(zip(cats, times), 1):
            w.writerow([i, set_name, c, t])
    timers = parse_timers(lines)
    if not timers:
        sys.exit("No timers found in the SpeedTest output")
    (Path(out_dir) / "timers").mkdir(exist_ok=True)
    with open(Path(out_dir) / "timers" / name, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["category", "timer", "calls", "total_s"])
        for (c, t), (calls, total) in sorted(timers.items()):
            w.writerow([c, t, calls, total])


# ---------------------------------------------------------------- report

def read_runs(out):
    """{label: {(cycle, set): {(category, timer): (calls, total_s)}}}; the warmup set is left out."""
    runs = {}
    for f in sorted(glob.glob(str(Path(out) / "timers" / "*__*__*.csv"))):
        label, cycle, set_name = Path(f).stem.split("__")
        if set_name == "warmup":
            continue
        d = {}
        for r in csv.DictReader(open(f)):
            d[(r["category"], r["timer"])] = (int(r["calls"]), float(r["total_s"]))
        ms = {c: [] for c in CATEGORIES}
        for r in csv.DictReader(open(Path(out) / Path(f).name)):
            ms[r["category"]].append(int(r["ms"]))
        for c, v in ms.items():
            d[(c, "Total (exact, per case)")] = (len(v), sum(v) / 1000)
        for t in {t for (_, t) in d}:  # "all" = sum of the categories
            parts = [d[(c, t)] for c in CATEGORIES if (c, t) in d]
            d[("all", t)] = (sum(p[0] for p in parts), sum(p[1] for p in parts))
        runs.setdefault(label, {})[(int(cycle), set_name)] = d
    return runs


def t_value(df):
    return T95[max(k for k in sorted(T95) if k <= df)]


def fmt_ci(vals):
    n = len(vals)
    return st.mean(vals), (t_value(n - 1) * st.stdev(vals) / math.sqrt(n) if n > 1 else float("nan"))


def report(out):
    runs = read_runs(out)
    if not runs:
        sys.exit(f"No results in {out}")
    labels = list(runs)
    units = sorted(set.intersection(*[set(v) for v in runs.values()]))  # (cycle, set) in all builds
    if not units:
        sys.exit("The builds have no (cycle, set) in common")
    first = runs[labels[0]][units[0]]
    L = ["# md benchmark report\n"]
    bf = Path(out) / "builds.txt"
    if bf.exists():
        L.append("Builds (from git):")
        for row in bf.read_text(encoding="utf-8").splitlines():
            lab, repo, branch, commit = (row.split("\t") + ["?"] * 4)[:4]
            L.append(f"- {lab}: {commit}, branch {branch}, {repo}")
        L.append("")
    L.append(f"Builds: {', '.join(labels)}. Runs per build: {len(units)} "
             f"(cycles: {len({c for c, _ in units})}, sets: {len({s for _, s in units})}); the warmup set is not included.")
    L.append("Cases per set: " + ", ".join(f"{c}={first[(c, 'Total (exact, per case)')][0]}" for c in CATEGORIES))
    L.append("Times are per set (all cases of the category in one run); avg = per call. `~` = total "
             "below 1 s (SpeedTest rounds totals to 0.1 s). Calls of large timers are rounded to thousands by SpeedTest, so avg per call is approximate for them. The verdict needs the 95% interval to "
             "exclude zero and to be at most 0.5% wide.\n")
    for c in CATEGORIES + ["all"]:
        L.append(f"## {c}\n")
        hdr = ["timer"] + [x for l in labels for x in (f"{l} total s", f"{l} avg ms/call")]
        if len(labels) == 2:
            hdr += ["diff % (95% CI)", "sd of diff %", "verdict"]
        L.append("| " + " | ".join(hdr) + " |")
        L.append("|" + "---|" * len(hdr))
        main_names = {t for _, t in MAIN_TIMERS if t}
        others = sorted({t for (cc, t) in first if cc == c} - main_names - {"Total (exact, per case)"})
        for title, t in MAIN_TIMERS + [(t, t) for t in others]:
            t = t or "Total (exact, per case)"
            if (c, t) not in first:
                continue
            cells = [title if title == t or t == "Total (exact, per case)" else f"{title} ({t})"]
            small = False
            for l in labels:
                tot = st.mean(runs[l][u][(c, t)][1] for u in units)
                calls = st.mean(runs[l][u][(c, t)][0] for u in units)
                small |= tot < 1
                cells += [f"{'~' if tot < 1 else ''}{tot:.2f}", f"{tot / calls * 1000:.3f}" if calls else "-"]
            if len(labels) == 2:
                a, b = labels
                diffs = [(runs[b][u][(c, t)][1] - runs[a][u][(c, t)][1]) / runs[a][u][(c, t)][1] * 100
                         for u in units if runs[a][u][(c, t)][1] > 0]
                if small:
                    cells += ["-", "-", "too small to measure"]
                elif len(diffs) >= 3:
                    m, ci = fmt_ci(diffs)
                    verdict = (("no clear change" if abs(m) <= ci else ("slower" if m > 0 else "faster"))
                               if 2 * ci <= 0.5 else
                               "inconclusive (interval wider than 0.5%)" if abs(m) <= ci else
                               ("slower, but interval wider than 0.5%" if m > 0 else "faster, but interval wider than 0.5%"))
                    cells += [f"{m:+.2f} ± {ci:.2f}", f"{st.stdev(diffs):.2f}", verdict]
                else:
                    cells += ["-", "-", "need >= 3 pairs"]
            L.append("| " + " | ".join(cells) + " |")
        L.append("")
    if len(labels) == 1:
        L.append("One build only: no comparison. Run with two builds for paired differences.")
    text = "\n".join(L)
    (Path(out) / "report.md").write_text(text + "\n", encoding="utf-8")
    print(text)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("cmd", choices=["run", "report", "prepare", "clean"])
    ap.add_argument("--build", action="append", default=[], metavar="LABEL=REPO_DIR")
    ap.add_argument("--cycles", type=int, default=1)
    ap.add_argument("--samples", type=int, default=1)
    ap.add_argument("--sets", default=",".join(SETS))
    ap.add_argument("--out", default=str(HERE / "bench-results"))
    a = ap.parse_args()
    if a.cmd == "run":
        if not a.build:
            sys.exit("Give at least one --build LABEL=REPO_DIR")
        sets = ["warmup"] + a.sets.split(",")
        builds = [tuple(b.split("=", 1)) for b in a.build]
        Path(a.out).mkdir(parents=True, exist_ok=True)
        busy = busy_processes([r for _, r in builds])
        if busy:
            sys.exit("Maven/java is running in a build directory; wait for it to finish:\n  "
                     + "\n  ".join(busy))
        (Path(a.out) / "builds.txt").write_text("".join(
            "\t".join([l, r, *git_info(r)]) + "\n" for l, r in builds), encoding="utf-8")
        dirs = [HERE / s for s in ALL_SET_DIRS]
        total = a.cycles * len(sets) * len(builds)
        k = 0
        try:
            copy_configs(dirs)
            for cycle in range(1, a.cycles + 1):
                for set_name in sets:
                    for label, repo in builds:  # warmup-A, warmup-B, run-1-A, run-1-B, ...
                        k += 1
                        busy = [b for b in busy_processes([r for _, r in builds])
                                if "md_benchmark" not in b]
                        if busy:
                            sys.exit("A build started during the benchmark; results would be "
                                     "disturbed:\n  " + "\n  ".join(busy))
                        print(f"[{k}/{total}] cycle {cycle} {set_name} build {label} ...", flush=True)
                        launch(repo, set_name, a.out, label, cycle, a.samples)
        finally:
            clean_configs(dirs)
    elif a.cmd == "prepare":
        copy_configs([HERE / s for s in ALL_SET_DIRS])
        return
    elif a.cmd == "clean":
        clean_configs([HERE / s for s in ALL_SET_DIRS])
        return
    report(a.out)


if __name__ == "__main__":
    main()
