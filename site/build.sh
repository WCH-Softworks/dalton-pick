#!/usr/bin/env bash
# Copies the privacy policies from docs/store/ into the site, so the policy has one source.
# Run from the repo root before building the site (the Pages workflow does).
set -euo pipefail
copy() { # lang, destination folder, title
  mkdir -p "site/$2"
  { printf -- '---\nlang: %s\nref: privacy\ntitle: %s\n---\n\n' "$1" "$3"
    cat "docs/store/privacy-policy.$1.md"; } > "site/$2/index.md"
}
copy en privacy "Privacy policy"
copy es es/privacy "Política de privacidad"
copy pt-BR pt-BR/privacy "Política de privacidade"
