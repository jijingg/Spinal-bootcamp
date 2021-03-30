#! /bin/sh

set -o errexit
set -o nounset
set -o xtrace

if [ ! -f mill ]; then
  curl -L https://github.com/com-lihaoyi/mill/releases/download/0.9.5/0.9.5 > mill && chmod +x mill
fi

./mill version
