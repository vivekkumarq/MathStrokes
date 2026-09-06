#!/usr/bin/env bash
#
# Creates the network and the VM in Oracle Cloud, retrying until Ampere A1 capacity appears.
#
#   bash scripts/oracle/00-provision.sh
#
# Ampere A1 is the whole reason for choosing Oracle - a real machine against Render's 0.1
# vCPU and 512 MB - and it is also the hard part. Free A1 capacity is genuinely scarce, and
# India South (Hyderabad) is among the worst regions for it. "Out of host capacity" is the
# normal first answer, not a mistake in the request. So this script asks in a loop rather
# than expecting a human to sit and click, and it walks the fault domains on each pass
# because capacity frees unevenly across them.
#
# Everything it creates is inside the Always Free allowance:
#   - VM.Standard.A1.Flex at 1 OCPU / 6 GB    (a quarter of the A1 allowance - see below)
#   - a 50 GB boot volume                     (allowance is 200 GB across all volumes)
#   - one VCN, one public subnet, one gateway (no charge)
#
# Override the size with OCPUS and MEMORY_GB if capacity ever looks plentiful:
#   OCPUS=4 MEMORY_GB=24 bash scripts/oracle/00-provision.sh
#
# Nothing here touches Render or Netlify.
#
set -euo pipefail

COMPARTMENT="${COMPARTMENT_OCID:-}"
SHAPE="VM.Standard.A1.Flex"
# 1 OCPU / 6 GB rather than the full 4 / 24 the free tier allows. Capacity is fragmented, and
# a small request fits into gaps a large one cannot - asking for the maximum as a single block
# is the slowest way to get anything at all. The tradeoff barely exists at this scale: against
# the 0.1 vCPU and 512 MB the application runs on today this is ten times the CPU and twelve
# times the memory, and it does not spin down. The remaining 3 OCPU of the allowance stay
# available to grow into once something is actually running.
OCPUS="${OCPUS:-1}"
MEMORY_GB="${MEMORY_GB:-6}"
BOOT_GB=50
DISPLAY_NAME="iota-api"
VCN_NAME="iota-vcn"
SUBNET_NAME="iota-public-subnet"
SSH_PUB="${SSH_PUB:-$HOME/.ssh/iota_oracle.pub}"
# DuckDNS gives the backend a real hostname, which Let's Encrypt needs and a bare IP cannot
# have. Pointing it at the new instance happens here rather than by hand because the address
# is only known the moment a launch succeeds, and a stale A record surfaces much later as a
# certificate failure that says nothing about DNS.
#
# The token is a credential and stays out of the repository - it lives in a file in $HOME.
# Both are optional: without them the script just prints the IP for you to set manually.
DUCKDNS_DOMAIN="${DUCKDNS_DOMAIN:-iotaexam}"
DUCKDNS_TOKEN_FILE="${DUCKDNS_TOKEN_FILE:-$HOME/.duckdns-token}"
# Gentle by default. Oracle rate-limits launch_instance per user and a free-tier account
# reaches that limit quickly; asking every few seconds does not find capacity sooner, it just
# converts "no capacity" into "too many requests" and hides the signal we actually want.
RETRY_SECONDS="${RETRY_SECONDS:-300}"
# Pause between individual fault-domain attempts, so one pass is three spaced requests rather
# than a burst of three.
SPACING="${SPACING:-30}"

log()  { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
info() { printf '    %s\n' "$*"; }
die()  { printf '\n\033[1;31m%s\033[0m\n' "$*" >&2; exit 1; }

command -v oci >/dev/null || die "The OCI CLI is not on PATH."
[ -f "$SSH_PUB" ] || die "No SSH public key at $SSH_PUB"

if [ -z "$COMPARTMENT" ]; then
    # The tenancy root is itself a compartment and is the right home for a single-application
    # tenancy. A sprawling one would want its own compartment; this does not.
    COMPARTMENT=$(oci iam compartment list --all --query 'data[0]."compartment-id"' --raw-output 2>/dev/null) \
        || die "Could not read the tenancy. Is ~/.oci/config valid? Try: oci iam region list"
fi
info "compartment $COMPARTMENT"

# ---------------------------------------------------------------------------------------
# Network. Created once and reused - the script is safe to re-run while waiting for capacity.
# ---------------------------------------------------------------------------------------
log "Virtual cloud network"
vcn_id=$(oci network vcn list --compartment-id "$COMPARTMENT" --display-name "$VCN_NAME" \
         --query 'data[0].id' --raw-output 2>/dev/null || true)
if [ -z "$vcn_id" ] || [ "$vcn_id" = "null" ]; then
    vcn_id=$(oci network vcn create --compartment-id "$COMPARTMENT" --display-name "$VCN_NAME" \
             --cidr-blocks '["10.0.0.0/16"]' --wait-for-state AVAILABLE \
             --query 'data.id' --raw-output)
    info "created $vcn_id"
else
    info "reusing $vcn_id"
fi

log "Internet gateway"
ig_id=$(oci network internet-gateway list --compartment-id "$COMPARTMENT" --vcn-id "$vcn_id" \
        --query 'data[0].id' --raw-output 2>/dev/null || true)
if [ -z "$ig_id" ] || [ "$ig_id" = "null" ]; then
    ig_id=$(oci network internet-gateway create --compartment-id "$COMPARTMENT" --vcn-id "$vcn_id" \
            --is-enabled true --display-name "iota-igw" --wait-for-state AVAILABLE \
            --query 'data.id' --raw-output)
fi
info "$ig_id"

log "Route table"
rt_id=$(oci network vcn get --vcn-id "$vcn_id" --query 'data."default-route-table-id"' --raw-output)
oci network route-table update --rt-id "$rt_id" --force \
    --route-rules "[{\"destination\":\"0.0.0.0/0\",\"destinationType\":\"CIDR_BLOCK\",\"networkEntityId\":\"$ig_id\"}]" \
    >/dev/null
info "default route -> internet gateway"

log "Security list"
# 22, 80 and 443 only. PostgreSQL (5432) and the application port (8080) are deliberately
# absent: they are reachable from the VM itself and from nowhere else. ufw on the host is a
# second layer behind this one, not a replacement for it.
sl_id=$(oci network vcn get --vcn-id "$vcn_id" --query 'data."default-security-list-id"' --raw-output)
oci network security-list update --security-list-id "$sl_id" --force \
  --egress-security-rules '[{"destination":"0.0.0.0/0","protocol":"all","isStateless":false}]' \
  --ingress-security-rules '[
     {"source":"0.0.0.0/0","protocol":"6","isStateless":false,"tcpOptions":{"destinationPortRange":{"min":22,"max":22}}},
     {"source":"0.0.0.0/0","protocol":"6","isStateless":false,"tcpOptions":{"destinationPortRange":{"min":80,"max":80}}},
     {"source":"0.0.0.0/0","protocol":"6","isStateless":false,"tcpOptions":{"destinationPortRange":{"min":443,"max":443}}},
     {"source":"0.0.0.0/0","protocol":"1","isStateless":false,"icmpOptions":{"type":3,"code":4}}
   ]' >/dev/null
info "ingress 22, 80, 443 (5432 and 8080 stay closed)"

log "Subnet"
subnet_id=$(oci network subnet list --compartment-id "$COMPARTMENT" --vcn-id "$vcn_id" \
            --display-name "$SUBNET_NAME" --query 'data[0].id' --raw-output 2>/dev/null || true)
if [ -z "$subnet_id" ] || [ "$subnet_id" = "null" ]; then
    subnet_id=$(oci network subnet create --compartment-id "$COMPARTMENT" --vcn-id "$vcn_id" \
                --display-name "$SUBNET_NAME" --cidr-block "10.0.1.0/24" \
                --wait-for-state AVAILABLE --query 'data.id' --raw-output)
fi
info "$subnet_id"

# ---------------------------------------------------------------------------------------
# Image. Resolved rather than hardcoded: image OCIDs are region-specific and are replaced
# whenever Canonical publishes a new build, so a literal would rot.
# ---------------------------------------------------------------------------------------
log "Ubuntu 24.04 image for $SHAPE"
image_id=$(oci compute image list --compartment-id "$COMPARTMENT" \
           --operating-system "Canonical Ubuntu" --operating-system-version "24.04" \
           --shape "$SHAPE" --sort-by TIMECREATED --sort-order DESC \
           --query 'data[0].id' --raw-output)
[ -n "$image_id" ] && [ "$image_id" != "null" ] \
    || die "No Ubuntu 24.04 aarch64 image found for $SHAPE in this region."
image_name=$(oci compute image get --image-id "$image_id" --query 'data."display-name"' --raw-output)
info "$image_name"

# ---------------------------------------------------------------------------------------
# Launch, with patience.
# ---------------------------------------------------------------------------------------
existing=$(oci compute instance list --compartment-id "$COMPARTMENT" --display-name "$DISPLAY_NAME" \
           --lifecycle-state RUNNING --query 'data[0].id' --raw-output 2>/dev/null || true)
if [ -n "$existing" ] && [ "$existing" != "null" ]; then
    log "An instance named $DISPLAY_NAME is already running"
    info "$existing"
    oci compute instance list-vnics --instance-id "$existing" \
        --query 'data[0]."public-ip"' --raw-output | sed 's/^/    public ip /'
    exit 0
fi

mapfile -t ADS < <(oci iam availability-domain list --compartment-id "$COMPARTMENT" \
                   --query 'data[].name' --raw-output | tr -d '[]", ' | grep -v '^$')
[ "${#ADS[@]}" -gt 0 ] || die "Could not list availability domains."
log "Availability domains: ${ADS[*]}"

# Fault domains are tried explicitly. Capacity is tracked per fault domain, so a region that
# reports "out of capacity" for one can still have room in another, and letting Oracle pick
# gives up that extra chance.
FDS=(FAULT-DOMAIN-1 FAULT-DOMAIN-2 FAULT-DOMAIN-3)

attempt=0
backoff="$RETRY_SECONDS"
log "Requesting $SHAPE  ${OCPUS} OCPU / ${MEMORY_GB} GB"
info "Retrying until capacity appears. Ctrl-C to stop; re-running is safe."
while true; do
    for ad in "${ADS[@]}"; do
        for fd in "${FDS[@]}"; do
            attempt=$((attempt + 1))
            printf '    [%s] attempt %-4d %s / %s ... ' "$(date -u +%H:%M:%S)" "$attempt" "${ad##*:}" "$fd"
            if out=$(oci compute instance launch \
                        --compartment-id "$COMPARTMENT" \
                        --availability-domain "$ad" \
                        --fault-domain "$fd" \
                        --display-name "$DISPLAY_NAME" \
                        --shape "$SHAPE" \
                        --shape-config "{\"ocpus\":$OCPUS,\"memoryInGBs\":$MEMORY_GB}" \
                        --image-id "$image_id" \
                        --boot-volume-size-in-gbs "$BOOT_GB" \
                        --subnet-id "$subnet_id" \
                        --assign-public-ip true \
                        --ssh-authorized-keys-file "$SSH_PUB" \
                        --wait-for-state RUNNING \
                        --query 'data.id' --raw-output 2>&1); then
                echo "launched"
                instance_id=$(printf '%s' "$out" | tail -1)
                ip=$(oci compute instance list-vnics --instance-id "$instance_id" \
                     --query 'data[0]."public-ip"' --raw-output)
                printf '\n  Instance running after %d attempts.\n\n' "$attempt"
                printf '    id     %s\n'     "$instance_id"
                printf '    shape  %s  %s OCPU / %s GB\n' "$SHAPE" "$OCPUS" "$MEMORY_GB"
                printf '    image  %s\n'     "$image_name"
                printf '    ip     %s\n\n'   "$ip"
                if [ -f "$DUCKDNS_TOKEN_FILE" ]; then
                    printf '  Pointing %s.duckdns.org at %s ... ' "$DUCKDNS_DOMAIN" "$ip"
                    tok=$(tr -d '[:space:]' < "$DUCKDNS_TOKEN_FILE")
                    # DuckDNS answers 200 whether it worked or not, with a bare OK or KO as the body, so
                    # the body is the only thing worth checking.
                    duck=$(curl -s --max-time 20 "https://www.duckdns.org/update?domains=$DUCKDNS_DOMAIN&token=$tok&ip=$ip" || true)
                    if [ "$duck" = "OK" ]; then
                        echo "OK"
                    else
                        echo "FAILED (answered ${duck:-nothing})"
                        echo "    Set it by hand at https://www.duckdns.org/domains before asking for a certificate."
                    fi
                else
                    echo "  No DuckDNS token at $DUCKDNS_TOKEN_FILE - set the A record by hand."
                fi
                echo
                printf '  Connect:\n    ssh -i ~/.ssh/iota_oracle ubuntu@%s\n\n' "$ip"
                printf '  Then:\n'
                printf '    scp -i ~/.ssh/iota_oracle scripts/oracle/01-bootstrap.sh ubuntu@%s:~\n' "$ip"
                printf '    ssh -i ~/.ssh/iota_oracle ubuntu@%s "bash ~/01-bootstrap.sh"\n' "$ip"
                exit 0
            fi

            if printf '%s' "$out" | grep -qi 'out of host capacity\|OutOfCapacity\|Out of capacity'; then
                echo "no capacity"
                backoff="$RETRY_SECONDS"
                sleep "$SPACING"
            elif printf '%s' "$out" | grep -qi 'TooManyRequests\|"status": *429'; then
                # Oracle throttles launch_instance per user, and free-tier accounts hit it
                # easily. Retrying harder is counterproductive - the throttle widens under
                # load - so each 429 doubles the wait, up to half an hour. This is why the
                # loop is deliberately unhurried: a request refused for being too frequent
                # is not the same problem as a request refused for want of a machine.
                echo "throttled (429) - backing off ${backoff}s"
                sleep "$backoff"
                backoff=$(( backoff * 2 ))
                [ "$backoff" -gt 1800 ] && backoff=1800
            elif printf '%s' "$out" | grep -qi 'LimitExceeded\|QuotaExceeded'; then
                # A service limit is not transient and will not clear by waiting.
                echo "LIMIT"
                die "Hit a service limit rather than a capacity shortage:

$out

Check Governance > Limits, Quotas and Usage for VM.Standard.A1.Flex. A Free Tier account is
allowed 4 OCPU and 24 GB of A1 in total, so this usually means an A1 instance already exists
somewhere in the tenancy - including a stopped one, which still holds the allocation."
            else
                echo "error"
                die "Launch failed for a reason that is not capacity:

$out"
            fi
        done
    done
    sleep "$RETRY_SECONDS"
done
