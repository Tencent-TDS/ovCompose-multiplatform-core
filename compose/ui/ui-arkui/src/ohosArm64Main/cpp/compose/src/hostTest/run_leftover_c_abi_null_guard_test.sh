#!/bin/sh
# Host leftover C ABI tests. No Harmony SDK / NAPI required.
set -eu
DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
INCLUDE="${DIR}/../main/cpp/compose"
OUT="${TMPDIR:-/tmp}/leftover_c_abi_null_guard_test"
g++ -std=c++17 -Wall -Wextra -Werror -I"${INCLUDE}" \
    "${DIR}/leftover_c_abi_null_guard_test.cpp" -o "${OUT}"
"${OUT}"
