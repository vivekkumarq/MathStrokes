#!/usr/bin/env bash
#
# Puts Nginx in front of the application and obtains a Let's Encrypt certificate for it.
#
#   bash ~/04-nginx-tls.sh api.example.com you@example.com
#
# A hostname is required and cannot be worked around. The Angular app is served over HTTPS
# from Netlify, so a browser will refuse to call an http:// backend at all - it is blocked as
# mixed content before the request is made. Let's Encrypt will not issue a certificate for a
# bare IP address. So the backend needs a name it can prove it owns.
#
# If no domain is owned, a free DuckDNS subdomain works and is a real, verifiable hostname:
# register at duckdns.org, point it at this VM's public IP, and pass e.g. iota-api.duckdns.org
# here. It is not as tidy as a domain you own, but it is genuinely free and the TLS is real.
#
set -euo pipefail

DOMAIN="${1:-}"
EMAIL="${2:-}"
APP_PORT="${APP_PORT:-8080}"

log() { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
die() { printf '\n\033[1;31m%s\033[0m\n' "$*" >&2; exit 1; }

[ -n "$DOMAIN" ] || die "Usage: 04-nginx-tls.sh <hostname> <email>"
[ -n "$EMAIL" ]  || die "Usage: 04-nginx-tls.sh <hostname> <email>"

log "Checking DNS points here before asking for a certificate"
# Let's Encrypt rate-limits failures. Confirming the A record first turns a wasted attempt
# against that limit into a clear message.
public_ip=$(curl -s --max-time 10 https://api.ipify.org || true)
resolved=$(getent hosts "$DOMAIN" | awk '{print $1}' | head -1 || true)
echo "  this VM  : ${public_ip:-unknown}"
echo "  $DOMAIN : ${resolved:-does not resolve}"
[ -n "$resolved" ] || die "$DOMAIN does not resolve. Create the A record first, then re-run."
if [ "$resolved" != "$public_ip" ]; then
    die "$DOMAIN resolves to $resolved, not to this VM ($public_ip).
Fix the A record and wait for it to propagate before re-running."
fi

log "Nginx site"
sudo tee /etc/nginx/sites-available/iota >/dev/null <<CONF
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN;

    # Certbot rewrites this block to redirect to HTTPS once the certificate exists. Until
    # then plain HTTP has to answer, because that is how the ACME challenge is served.
    location / {
        proxy_pass http://127.0.0.1:$APP_PORT;

        # Spring Security builds absolute URLs and applies rate limiting from the client
        # address. Without these it would see every request as coming from 127.0.0.1, which
        # would make the per-IP registration limit apply to the whole world at once.
        proxy_set_header Host              \$host;
        proxy_set_header X-Real-IP         \$remote_addr;
        proxy_set_header X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header X-Forwarded-Host  \$host;

        # An exam submission carries every answer at once, and the default 1 MB body limit
        # is uncomfortably close for a long paper.
        client_max_body_size 10m;

        # Longer than the default 60s: a cold JVM answering its first query after a restart
        # can exceed it, and a 504 at that moment looks like an outage rather than a warm-up.
        proxy_read_timeout    120s;
        proxy_connect_timeout 10s;

        # Buffering off so an error surfaces immediately rather than being held by Nginx.
        proxy_buffering off;
    }
}
CONF

sudo ln -sfn /etc/nginx/sites-available/iota /etc/nginx/sites-enabled/iota
# The packaged default site answers on the same port and would otherwise win on a plain
# IP request, which makes debugging confusing.
sudo rm -f /etc/nginx/sites-enabled/default

sudo nginx -t
sudo systemctl reload nginx
echo "  nginx reloaded"

log "Certificate"
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq certbot python3-certbot-nginx
sudo certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos --email "$EMAIL" --redirect

log "Renewal"
# Certbot installs a systemd timer on Ubuntu. A dry run proves renewal will actually work in
# 60 days rather than discovering at expiry that the challenge path is broken.
sudo systemctl list-timers 'certbot*' --no-pager | head -3
sudo certbot renew --dry-run

log "End to end"
curl -sS -o /dev/null -w "  https://$DOMAIN/api/tests -> %{http_code}\n" "https://$DOMAIN/api/tests" || true

cat <<DONE

Nginx is serving https://$DOMAIN and proxying to the application on 127.0.0.1:$APP_PORT.

A 401 or 403 from the check above is a success: it means the request reached Spring Security
and was rejected for want of a token. A 502 means Nginx could not reach the application -
check 'systemctl status iota'.

Nothing on Netlify or Render has been touched. The frontend still talks to Render until you
change it deliberately.

Next, and only once this URL is proven from your own browser:
  - add $DOMAIN to CORS_ALLOWED_ORIGINS is NOT needed if you keep the Netlify proxy
  - point netlify.toml's /api/* rule at https://$DOMAIN/api/:splat
  - update frontend/src/environments/environment.prod.ts, which currently hardcodes
    the Render URL absolutely and would otherwise bypass the proxy entirely
DONE
