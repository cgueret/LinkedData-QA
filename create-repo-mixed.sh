#!/bin/bash

#outBase="repo-posneg"
source config



type="$1"
minTripleCount=${2:-10}

if [ -z "$type" ]; then
	echo "Please specify positive or negative"
	exit
fi

for dir in `find "$latcRepo" -type d`; do

	specFile="$dir/spec.xml"
        file="$dir/$type.nt"

	if [ -f "$specFile" -a -f "$file" ]; then

		d="${dir##/*/}"
		echo "Candidate: $d"

		tmpEndpointFile="/tmp/endpoints.txt"
		tmpFile="/tmp/$type.nt"

		./extract-endpoints.sh "$specFile" > "$tmpEndpointFile"
		cat "$file" | awk 1 | rapper -i ntriples - http://dummy.org | sort -u > "$tmpFile"

		tripleCount=`cat "$tmpFile" | wc -l`

		if [ "$tripleCount" -lt "$minTripleCount" ]; then
			echo "Skip: $d"
			continue;
		fi
		echo "Accept: $d"

		targetDir="$mixedRepo/$d"
		mkdir -p "$targetDir"

		mv "$tmpEndpointFile" "$targetDir"
		mv "$tmpFile" "$targetDir"
	fi

done


