---
name: setup-data
description: "Set up OTP test data for a specific case. Downloads NeTEx and/or OSM files, optionally filters them, and copies config files. Use when debugging OTP routing issues or testing a new feature."
allowed-tools: Bash, Read, Edit
---

# Setup OTP Test Data

Downloads and prepares data for an OTP test case from `cases.txt`.

## Input

$ARGUMENTS

The argument should be:
- A **case name** — either the full path (e.g., `otp/fylker/oslo`) or a short name (e.g., `oslo`)
- Optional **flags**: `--netex`, `--osm`, `--filter-netex`, `--config <dir>`, `--main-config <dir>`

If no argument is provided, list available cases and ask the user to pick one.

## Data project

All commands run from `/Users/thomasgran/code/entur/otp/data` — this directory contains
`cases.txt` (read at startup as a relative path) and the downloaded data.

## Flags

| Flag | Default | Description |
|------|---------|-------------|
| `--netex` | ON when no flags set | Download and unpack NeTEx files |
| `--osm` | OFF | Download and filter OSM file (requires `osmium`) |
| `--filter-netex` | OFF | Filter NeTEx by GeoJSON polygon (removes stops/quays outside the polygon and service journeys that leave it) |
| `--config <dir>` | none | Copy default config from `config/<dir>/` |
| `--main-config <dir>` | none | Copy main config, overwrites default config |

If neither `--netex` nor `--osm` is given, `--netex` is implied.

## Procedure

### Step 1: Resolve the case

If no argument is provided, read `cases.txt` and list the available cases, then ask the user which one to use.

```bash
grep -v '^\s*#\|^\s*$\|NAME.*OSM' /Users/thomasgran/code/entur/otp/data/cases.txt | awk -F'|' '{print NR". "$1}' | column -t
```

If an argument is given, verify the case exists:

```bash
grep -v '^\s*#\|^\s*$\|NAME.*OSM' /Users/thomasgran/code/entur/otp/data/cases.txt | awk -F'|' '{print $1}' | tr -d ' '
```

Partial name matching is handled by the Kotlin code (e.g., `oslo` → `otp/fylker/oslo`). If the
name is ambiguous (multiple matches), list them and ask the user to be more specific.

### Step 2: Adding a new case (if needed)

If the requested case does not appear in `cases.txt`, help the user add it:

1. **Choose a path** — suggest `otp/cases/<name>` for one-off debug cases or `otp/fylker/<name>` for regional cases
2. **Choose OSM** — almost always `norway` (the full Norway OSM)
3. **Choose GeoJSON** — list existing files in `geojson/`:
   ```bash
   ls /Users/thomasgran/code/entur/otp/data/geojson/
   ```
   If none fits, instruct the user to create one at https://geojson.io/ and save it as
   `geojson/<name>.geojson`. Leave blank to get a symlink to the full Norway OSM.
4. **Choose transit feeds** — list the IDs from `config.kt`:
   ```bash
   grep 'ids =' /Users/thomasgran/code/entur/otp/data/src/main/config.kt
   ```
5. **Append the row** to `cases.txt` using the pipe-delimited format:
   ```
   otp/cases/<name>             | norway | <geojson>         | <feeds...>
   ```
   Keep columns aligned with the existing rows for readability.

### Step 3: Run the setup

Build the project (skip if already compiled and no source changes):

```bash
cd /Users/thomasgran/code/entur/otp/data && mvn compile -q 2>&1 | grep -v 'WARNING'
```

Run the setup:

```bash
cd /Users/thomasgran/code/entur/otp/data && mvn exec:java -q -Dexec.args="<case> [flags...]" 2>&1
```

Example invocations:
```bash
# NeTEx only (default)
cd /Users/thomasgran/code/entur/otp/data && mvn exec:java -q -Dexec.args="oslo"

# NeTEx + OSM
cd /Users/thomasgran/code/entur/otp/data && mvn exec:java -q -Dexec.args="oslo --netex --osm"

# With staging config
cd /Users/thomasgran/code/entur/otp/data && mvn exec:java -q -Dexec.args="oslo --netex --config staging"
```

Multiple flags: pass as a single space-separated string in `-Dexec.args`.

### Step 4: Report results

After a successful run, report:
- The output directory: `/Users/thomasgran/code/entur/otp/data/<case-path>/` (e.g., `otp/fylker/oslo/`)
- What was downloaded (NeTEx files, OSM, config)
- How to point OTP at this data:
  ```bash
  java -Xmx4G -jar otp-shaded/target/otp-shaded-*.jar --build --serve /path/to/data/<case-path>
  ```

## Known limitations

- The OSM download is cached for 14 days. Delete `osm/norway-latest.osm.pbf` to force a fresh download.
- NeTEx is always re-downloaded (1-day cache) on each run.
- `--filter-netex` requires a GeoJSON polygon defined for the case. If the case has no GeoJSON, the flag is silently skipped with a warning.