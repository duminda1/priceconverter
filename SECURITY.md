# Security Policy

## Supported Versions

This project currently supports the **latest version on the `main` branch**.

Security fixes are applied only to the most recent version of the codebase.
Older releases are not actively maintained.

| Version | Supported |
|--------|-----------|
| main   | ✅ Yes    |
| older releases | ❌ No |

---

## Reporting a Vulnerability

If you discover a security vulnerability, **please do not open a public issue or pull request**.

Instead, report it privately using **one of the following methods**:

### Preferred method
- Use **GitHub Security Advisories**:
  - Go to the repository
  - Click **Security → Report a vulnerability**
  - Submit the details privately

### Alternative
- Email: **duminda@outlook.com**
  - Include as much detail as possible:
    - Description of the issue
    - Steps to reproduce
    - Potential impact
    - Affected components or files

---

## Response Process

- You can expect an **initial response within 72 hours**
- If the issue is confirmed:
  - A fix will be developed privately
  - A security update will be released
  - Credit will be given if requested
- If the issue is declined, we will explain why

---

## Responsible Disclosure

Please allow a reasonable time for the issue to be fixed before any public disclosure.

We appreciate responsible disclosure and efforts to improve the security of this project.

---

## Certificate Pinning (Release Builds)

Release builds enable certificate pinning for the exchange rate APIs via OkHttp. Debug
builds skip pinning to keep local development flexible. Pin mismatches are handled
like other network errors and do not block app startup.

Pins are SPKI SHA-256 hashes in OkHttp's `sha256/<base64>` format. They were obtained
with OpenSSL using the server's certificate chain. Example commands:

```bash
HOST=api.exchangerate.host

# Leaf certificate pin (first cert in the chain).
echo | openssl s_client -servername "$HOST" -connect "$HOST:443" 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64

# Intermediate certificate pin (second cert in the chain).
echo | openssl s_client -showcerts -servername "$HOST" -connect "$HOST:443" 2>/dev/null \
  | awk 'BEGIN{c=0;p=0} /BEGIN CERTIFICATE/{c++; if(c==2) p=1} p{print} /END CERTIFICATE/{if(p){exit}}' \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

Repeat the same commands with `HOST=api.frankfurter.app`.

Automation:
- Run `scripts/generate_pins.sh --out /tmp/pins.env` to generate pins in KEY=VALUE format.
- Set `PINS_FILE=/tmp/pins.env` when running release builds, or export the two pin variables.

Rotation strategy:
- Keep two pins per host (current leaf + current intermediate).
- Recompute pins on CA changes or before the leaf certificate expires.
- Update the release `buildConfigField` values in `app/build.gradle.kts` and keep the
  previous pins until the updated release is broadly available.
