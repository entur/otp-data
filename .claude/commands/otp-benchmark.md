---
name: md-benchmark
description: "Run the md (multi-criteria destination) SpeedTest benchmark on one or two OTP builds, or report on results that are already there. Shows every SpeedTest timer per case category (local, regional, long_distance) and the paired difference between builds. Use to tell if a change made the md search faster or slower."
allowed-tools: Bash, Read, AskUserQuestion
---

# md benchmark

Runs `speedtest/norway-transit-1000/md_benchmark.py` and presents the report. The case sets
(`warmup`, `run-1` .. `run-5`, each 200 local, 100 regional, 100 long_distance, different cases in
each) and the configuration files
are in `speedtest/norway-transit-1000/`. The configuration is edited in that directory only; the
script copies it into the set folders for the run and removes it afterwards.

## Input

$ARGUMENTS

Two forms:

- **run** `LABEL=REPO_DIR [LABEL=REPO_DIR] [cycles=N] [sets=run-1,run-2,...] [out=DIR]`
  Example: `run base=/Users/me/code/otp-dev head=/Users/me/code/otp-feature cycles=2`
  Each `REPO_DIR` is an OpenTripPlanner checkout (worktree) of the build to test. Use two builds to
  compare; one build only gives timings. Default `cycles` is 1.
- **report** `[out=DIR]` - only analyse results that are already in `DIR`.

If no argument is given, or parts of it are missing, ask for the missing input with
AskUserQuestion (one call, all questions together). Do not guess; offer these defaults:

1. **What to do:** run a comparison, or report on existing results (then ask for `out`).
2. **Branch to compare with** (the "base" build). Default `base` = the merge-base of the
   current checkout and `otp/dev-2.x`: `git merge-base HEAD otp/dev-2.x` (run
   `git fetch otp dev-2.x` first). Other options: a branch name or commit the user types in, or
   no comparison (only time the current checkout). The other build, `head`, is the current
   checkout of the OpenTripPlanner repo (`/Users/thomasgran/code/entur/otp/otp`).
3. **Number of runs per cycle** (1..5): how many of the sets `run-1`..`run-5` to use, always
   starting with `run-1`. Default 5. Pass it as `sets=run-1,...,run-N`. The `warmup` set is always
   run in addition. Pairs = runs x cycles, and a comparison needs at least 3 pairs.
4. **Number of cycles.** Default 1.

Show the plan before starting: builds with commit, number of runs, cycles, and the expected time
(see below).

### Preparing the base build

If the base is a branch or commit and no checkout of it exists, create a detached worktree
outside the repo, e.g. `git worktree add --detach /Users/thomasgran/code/entur/otp/worktrees/benchmark-base <commit>`
(reuse it if it already exists and is at that commit; otherwise `git -C <dir> checkout --detach
<commit>`). Tell the user before creating it. The script builds the classpath and compiles the checkout itself (about 20 s per checkout). Also
check that the graph exists for the base's serialization id (step 2 below); if the id differs from the
head's, the two builds need different graphs and the run cannot be compared: stop and tell the
user.

## Before a run

1. Check that every `REPO_DIR` exists and is a git checkout. Show `git -C <dir> log -1 --oneline`
   and the branch for each, so the report can name what was measured.
2. Check the graph exists for each build: `../otp/norway/graph-otp2-<id>.obj` where `<id>` is
   `otp.serialization.version.id` in that checkout's `pom.xml`. `speed-test-config.json` points at
   this file. If it is missing for a build, stop and tell the user to build it. Do not build it.
3. Tell the user the expected time before starting. A run is one set in a new JVM (graph load
   about 25 s, then the 400 cases once): about 1.5 minutes. One
   cycle is warmup + run-1..5 for each build, so 12 runs for two builds, about 20 minutes. Ask for a
   go-ahead if the total is more than 30 minutes.
4. Nothing else heavy may run on the machine during the benchmark, and no Maven or java process may
   run in a build directory (a `mvn clean` there deletes the classes under the benchmark). Mention
   it. The script checks this before the start and before every run, and stops with the process
   list if it finds one.

## Run

From the data project root:

```bash
python3 -I speedtest/norway-transit-1000/md_benchmark.py run \
  --build LABEL=REPO_DIR [--build LABEL=REPO_DIR] --cycles N [--sets ...] --out DIR
```

It takes a long time: run it in the background (`run_in_background`) and do not poll. Each run
is a plain `java` process (classpath from Maven, sibling modules from each checkout's
`target/classes`, never from `~/.m2`). SpeedTest leaves a non-daemon thread (`updater-ready`) running,
so the JVM may not exit by itself: the script stops it 20 s after the md summary and kills it after
10 minutes, so a run cannot hang. A run that gives no timings stops the script with the log tail. The order
in a cycle is warmup-A, warmup-B, run-1-A, run-1-B, ... run-5-A, run-5-B, then the next cycle. If
it fails, the script still removes the config copies; report the error text and stop. If the
process is killed, run `... md_benchmark.py clean` (it removes the config copies from the set
folders).

## Report

`python3 -I speedtest/norway-transit-1000/md_benchmark.py report --out DIR` prints the report and
writes `DIR/report.md`. Show it to the user like this:

1. A header: builds (label, branch, commit), cycles, serialization id, date. Take the commits from
   the `Builds (from git)` lines of the report (`builds.txt`), not from SpeedTest's own version
   line, which can be stale.
2. The tables, one per category (`local`, `regional`, `long_distance`, `all`), every timer, in the
   order the script prints them. Do not drop rows; the user wants all the numbers. In each row:
   total per run and average per call for each build; with two builds also the difference in %
   with its 95% interval, the spread of the differences, and the verdict.
3. A short list of the rows with a verdict of faster or slower, largest first.
4. The limits, stated plainly:
   - rows marked `~` have a total below 1 s per set; SpeedTest rounds totals to 0.1 s, so they
     are noise
   - a verdict with "interval wider than 0.5%" means the run cannot show a 0.5% change; say how
     many more pairs would be needed, roughly: pairs scale with the square of (current interval /
     0.5), and one cycle gives 5 pairs (one pair = same set in the same cycle)
   - a comparison needs at least 3 pairs
5. Point to `DIR/report.md` and the per-run CSVs in `DIR` and `DIR/timers`.

Never call a change real when the verdict says `no clear change` or mentions an interval wider
than 0.5%.

## Notes

- One run = one JVM = one graph load = one set. The set `warmup` is run in every cycle like the
  others (it warms up the machine) but is not in the report. A and B are always compared on the
  same set in the same cycle, so the differences between the sets drop out.
- The timers come from SpeedTest's own timer table. Names: `Total` = `Routing Total`,
  `mc raptor` = `Raptor Mc-DP Route`, `reverse heuristic` = `Raptor MinTravelDuration-Rev Route`,
  `precalculation` = `Routing PreCalculation`, `raptor total` = `Routing Raptor`. The other
  timers (Minute Transit, Minute Transfers, Access, Egress, TripPatternFiltering) are listed
  after the main ones.
- To run a single set by hand: `python3 -I speedtest/norway-transit-1000/md_benchmark.py prepare`,
  run SpeedTest with `--dir=speedtest/norway-transit-1000/<set>`, then `... clean`.
