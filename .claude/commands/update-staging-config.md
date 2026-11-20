---
name: update-staging-config
description: "Update config/staging/ from the Entur OTP deployment config (Helm templates on the main_config branch of entur/OpenTripPlanner). Fetches the templates, renders them with the tst values, applies the local-debugging filter, and shows a diff. Use when staging config has changed upstream."
allowed-tools: Bash, Read, Edit, Write
---

# Update config/staging

`config/staging/` is a **filtered copy** of the config that Entur runs in the tst (staging)
environment. The source is NOT plain JSON. It is Helm templates on the `main_config` branch of
https://github.com/entur/OpenTripPlanner, under `entur/deployment-config/helm/otp2/`.

## Input

$ARGUMENTS

Optional: a different branch or ref for `main_config`. Default: `main_config`.
Optional: `--dry-run` to show the diff only and write nothing.

## Source → target map

All paths are relative to `entur/deployment-config/helm/otp2/` on the branch.

| Target file | Source |
|-------------|--------|
| `config/staging/otp-config.json` | `templates/configmap-graph-builder.yaml` → `data.otp-config.json` (graph builder, which has the build-time features) merged with the feature list in `templates/otp2-feature-configmap.yaml` |
| `config/staging/build-config.json` | `templates/configmap-graph-builder.yaml` → `data.build-config.json` |
| `config/staging/router-config.json` | `templates/configmap.yaml` → `data.router-config.json` |

Values come from two env files, each layered over `values.yaml`:
- `env/values-kub-ent-tst.yaml` — the graph builder (`build-config.json`, `otp-config.json`). Contains `cronjobRedeployGraphBuilder`, `vehicleRentalGeofencing` and `gbfs`.
- `env/values-kub-ent-jp-tst.yaml` — the serving journey planner (`router-config.json` and the feature flags). Contains `configuration:` (updater URLs, timeouts, `vehicleRentalServiceDirectory`).

`values-kub-ent-tst.yaml` has `journeyPlanner.enabled: false`, so it cannot render `configmap.yaml`.
Fetch both files.

Local files are stored pretty-printed. Do not diff text. Strip `//` comment lines from the
rendered and the local file, parse both as JSON, and compare with sorted keys. Then hand-edit
only the real differences into the local file, so its comments and local-only lines survive.
`otp-config.json` is the graph-builder `otp-config.json` plus the feature flags from
`otp2-feature-configmap.yaml`, so only compare the keys that exist in the graph builder part.

## Procedure

### Step 1: Fetch sources

Work in the scratchpad directory, not in the project. Fetch with `gh api` (the repo is private to
Entur; `gh` is already authenticated):

```bash
B=repos/entur/OpenTripPlanner/contents/entur/deployment-config/helm/otp2
for f in values.yaml env/values-kub-ent-tst.yaml templates/_helpers.tpl \
         templates/configmap.yaml templates/configmap-graph-builder.yaml \
         templates/otp2-feature-configmap.yaml; do
  mkdir -p "$SCRATCH/$(dirname "$f")"
  gh api "$B/$f?ref=main_config" --jq .content | base64 -d > "$SCRATCH/$f"
done
```

### Step 2: Render the templates

If `helm` is installed, render with it and take the ConfigMap data from the output:

```bash
helm template otp2 "$SCRATCH" -f "$SCRATCH/env/values-kub-ent-tst.yaml" \
  --set image.identifier=staging:COPY
```

(`helm` is not installed by default. Do not install it without asking. Without it, render by
hand: substitute each `{{ .Values.x }}` from the merged values, resolve `{{- if }}` blocks,
and expand `{{ .Values.gbfs | toPrettyJson }}` as JSON. The templates only use simple
substitutions.)

The rendered JSON can contain `//` comments. OTP accepts them; keep them.

### Step 3: Apply the local-debugging filter

Start from the rendered output, then apply these rules. Use the current files in
`config/staging/` as the reference for how each rule was applied before. The local files also
hold commented-out blocks with explanatory headers, so keep that style.

1. **`configVersion`**: set to `COPY STAGING <today, YYYY-MM-DD>` in all three files.
2. **Google Cloud Storage sources are not read locally.** Comment out, with a header line
   that says why:
   - `osm` → `// READ OSM DATA FROM DEFAULT LOCATION, NOT GCS`
   - `dem` → keep commented out. Add the local alternative `../../dem/<file name>.tif` under the `gs://` source. Header: `// ENABLED IN STAGING, BUT LOADING ELEVATION DATA IS NORMALY NOT REQUIERED FOR LOCAL DEBUGGING`
   - `transitFeeds` → `// ENABLED IN STAGING - COMMENT IN IF NEEDED`. Local runs read NeTEx from disk, see the active `transitFeeds` block already in the file and keep it as is.
   - `emission`, `empiricalDelay` → commented out the same way.
   - `vehicleRentalGeofencing` → commented out, with a note that it is enabled in staging and refers to `gbfs` in `otp-config.json`.
3. **Graph paths are local**: replace the `gs://…` values of `streetGraph`, `graph` and
   `buildReportDir` with `streetGraph-${otp.serialization.version.id}.obj`,
   `graph-otp2-${otp.serialization.version.id}.obj` and `report`.
4. **`otp-config.json`**: keep `GoogleCloudStorage` as in the source. Keep `"CarPooling": false`
   and the other local feature lines that the current file has and the source lacks, unless the
   user says otherwise. The `gbfs` block is copied as rendered.
5. **`router-config.json`**:
   - Comment out `vehicleRentalServiceDirectory` (noisy locally).
   - Keep the local SIRI-ET updater that reads `file://{{OTP_SETUP_PATH}}/rt`. Do not replace it.
   - Comment out the staging realtime updaters (`siri-sx-updater`, `siri-et-carpooling-updater`,
     etc.) under a `// FROM STAGING` header, and keep them in sync with the source.
   - Keep the `{{OTP_SETUP_PATH}}` placeholder. `setup-data` fills it in.
6. **Keep local-only additions** (such as `"cache": { "enabled": true }` in build-config) unless
   the source now defines the same key. If unsure whether a local line is intentional, keep it
   and mention it in the summary.
7. Do not copy Helm syntax (`{{ … }}`) into the output. The only allowed `{{…}}` is
   `{{OTP_SETUP_PATH}}`.

### Step 4: Show the diff and apply

1. Write the result to `config/staging/`, unless `--dry-run`.
2. Run `git diff -- config/staging` and summarize the changes per file in plain words:
   what is new upstream, what was removed, and which local-only lines were kept.
3. Validate: strip `//` comment lines, then check that each file parses.
   ```bash
   for f in config/staging/*.json; do
     sed -E 's/^\s*\/\/.*$//' "$f" | sed 's/{{OTP_SETUP_PATH}}/x/' | jq empty && echo "ok $f"
   done
   ```
   Inline `//` comments after values will make `jq` fail. If that happens, check those files by
   eye and say so.

### Step 5: Report

Tell the user:
- The branch and commit SHA of `main_config` that was used (`gh api repos/entur/OpenTripPlanner/commits/main_config --jq .sha`).
- Which keys changed in each file.
- Any rule you had to guess.

Do not commit. The user commits the change themselves.
