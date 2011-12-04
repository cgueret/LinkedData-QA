#!/bin/bash

source config

repo="$1"

if [ -z "$repo" ]; then
	echo "No repo specified"
	exit -1
fi

for dir in `find "$repo" -maxdepth 1 -mindepth 1 -type d`; do
	endpointsFile="$dir/endpoints.txt"
	if [ -f "$endpointsFile" ]; then
		tmpFile="$endpointsFile.bak"
		mv "$endpointsFile" "$tmpFile"
		cat "$tmpFile" | sed "s|$2|$3|g" > "$endpointsFile"
	fi
done

