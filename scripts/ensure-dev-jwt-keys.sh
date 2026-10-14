#!/bin/sh
# Dev, QA and tests load this pair from the classpath. It is not a production
# secret and must not be committed. Prod images delete it before packaging.
set -eu

dir="src/main/resources/jwt"
priv="$dir/dev-private.pem"
pub="$dir/dev-public.pem"

if [ -f "$priv" ] && [ -f "$pub" ]; then
  exit 0
fi

mkdir -p "$dir"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$priv"
openssl rsa -pubout -in "$priv" -out "$pub"
chmod 600 "$priv"
