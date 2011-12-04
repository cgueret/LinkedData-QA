#!/bin/bash

source config

./replace-endpoint-core.sh "$posNegRepo" "$1" "$2"
./replace-endpoint-core.sh "$rankingRepo" "$1" "$2"
./replace-endpoint-core.sh "$mixedRepo" "$1" "$2"

