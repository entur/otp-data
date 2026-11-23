# norway-transit-1000

1000 routing requests for the SpeedTest, taken from recordings of real requests to the Entur
Journey Planner (`../../routing-requests-player/dumps/*_plan.csv`, 2026).
Every case returns at least one itinerary with transit (not only street) in the Norway graph.

- `travelSearch.csv` - the 1000 cases. Times are local time, the date is `testDate` in
  `speed-test-config.json`.
- `travelSearch-expected-results-<profile>.csv` - expected results, generated with the base
  commit (`137e7ca618`, `dev-2.x`), 10 itineraries per case. Profiles: `sr`, `srr`, `mc`, `md`,
  `bd`, `bdr`.
  - `srr` and `bdr` have results for only the 32 arrive-by cases (a reverse search needs an
    arrival time). `mc` has no result for 2 cases, `bd` for 32 cases (same as the base commit).
- `router-config.json`, `otp-config.json`, `build-config.json` - copy of `../../config/staging`.
  The `updaters` section is removed from the router config (SpeedTest has no realtime).
- Coordinates of anything that is not an NSR stop place (user positions, `my_location`, addresses,
  POIs) are snapped to a ~100 m grid (0.0009 deg latitude, 0.0018 deg longitude), to avoid keeping
  exact positions. NSR stop place coordinates are rounded to 5 decimals (~1 m). Only NSR place ids are kept in
  `fromPlace`/`toPlace`: OTP drops any other id and uses the coordinates, so they are removed.
- `category` is the straight-line distance from origin to destination: `local` (< 20 km), 
  `regional` (20-100 km) and `long_distance` (>= 100 km).
- `ignoreStreetResults` is `true`: street-only itineraries are not part of the results.

## How the cases were made

1. `extract_candidates.py` reads the recordings and writes ~2000 candidates: first page only, walk or
   flex access, transit modes, deduplicated and shuffled with a fixed seed.
2. The candidates were run with SpeedTest (`ignoreStreetResults=true`).
3. `select_cases.py` picks the first 1000 with a transit itinerary and renumbers them.

## Run

The graph must be built first (same data and config as `../../otp/norway`, serialization id
must match the code you test):

```
java -Xmx48g -jar otp-shaded.jar --build --save ../../otp/norway
mvn --projects application exec:java -Dexec.mainClass="org.opentripplanner.transit.speed_test.SpeedTest" \
  -Dexec.classpathScope=test -Dexec.args="--dir=../data/speedtest/norway-transit-1000 -p md,sr -n 1 -i 10"
```

Differences in a few cases are expected: the street search is not deterministic (for example a
stop and its equivalent quay, seen in 2-3 cases per run).
