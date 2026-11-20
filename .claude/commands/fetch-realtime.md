---
name: fetch-realtime
description: "Fetch a live snapshot of SIRI SX and SIRI ET data from Entur's tst environment via kubectl port-forward, saved under realtime/<region>/<date>/. Use when debugging realtime updaters or needing fresh SX/ET test fixtures."
allowed-tools: Bash
---

# Fetch Realtime SX/ET Snapshot

Fetches one-off snapshots of the SIRI feeds normally consumed by OTP's `siri-sx-updater` and
`siri-et-google-pubsub-updater` (via its `dataInitializationUrl`). Connects to the `tst` Entur
k8s cluster, port-forwards to `realtime-cache`, curls each feed, and saves the raw XML.

## Input

$ARGUMENTS

Optional output directory, relative to `realtime/` in the data project root (the repo root, where `cases.txt` is).
Default: `norway/<today's date, YYYY-MM-DD>`.

## Prerequisites

- `gcloud` authenticated as the user's `@entur.org` account (`gcloud auth list`).
- `kubectl` installed.

## Procedure

### Step 1: Resolve output directory

```bash
ROOT=$(git rev-parse --show-toplevel)
OUT="$ROOT/realtime/${ARGUMENTS:-norway/$(date +%F)}"
mkdir -p "$OUT"
```

### Step 2: Get cluster credentials (skip if context already works)

```bash
kubectl get ns >/dev/null 2>&1 || \
  gcloud container clusters get-credentials kub-ent-tst-001 --region europe-west1 --project ent-kub-tst
```

### Step 3: Fetch SX and ET from `realtime-cache`

Port-forward to the `realtime-cache` Service (no need to resolve a pod name — the Service
load-balances to whichever pod is up):

Run this as ONE Bash call, so the `trap` can stop the port-forward on any exit, including errors:

```bash
set -u
# Shell state does not persist between Bash calls, so resolve the output directory again
OUT="$(git rev-parse --show-toplevel)/realtime/${ARGUMENTS:-norway/$(date +%F)}"
kubectl port-forward svc/realtime-cache 8091:80 -n realtime-cache >/dev/null 2>&1 &
PF1=$!
trap 'kill $PF1 2>/dev/null' EXIT

ready=""
for i in $(seq 1 15); do
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8091/sx 2>/dev/null)
  if [ "$code" = "200" ]; then ready=1; break; fi
  sleep 1
done
if [ -z "$ready" ]; then
  echo "ERROR: realtime-cache did not return 200 on /sx after 15s (last code: $code)" >&2
  exit 1
fi

curl -sf http://localhost:8091/sx -o "$OUT/sx.xml" || { echo "ERROR: fetching /sx failed" >&2; exit 1; }
curl -sf http://localhost:8091/et -o "$OUT/et.xml" || { echo "ERROR: fetching /et failed" >&2; exit 1; }
```

If the command fails, stop and tell the user. Do not report the partial files as a result. Remove
a failed file with `rm -f "$OUT/sx.xml" "$OUT/et.xml"` first.

Notes:
- `/sx` is small (a few MB).
- `/et` is the full ET dump and can be **very large (100-300+ MB)** — it's the same endpoint
  the `siri-et-google-pubsub-updater`'s `dataInitializationUrl` points at. Expect it to take
  a while; don't skip it if disk/time is constrained, just warn the user first.

### Step 4: Report results

```bash
ls -la "$OUT"
wc -c "$OUT"/*.xml
```

Report the output directory and the size of each file.

## Known limitations

- Requires VPN/cluster access to Entur's `tst` GKE cluster (`kub-ent-tst-001`,
  `europe-west1`, project `ent-kub-tst`) — this is internal infrastructure, not reachable
  from outside Entur's network.
- The `/et` dump from `realtime-cache` is uncompressed SIRI XML and can be hundreds of MB;
  there's no filtering option, it's a full snapshot.
- The `trap` in step 3 stops the port-forward on any exit. If a run was interrupted, check for a
  leftover one with `pgrep -fl 'port-forward svc/realtime-cache'`.
