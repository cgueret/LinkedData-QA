#!/bin/bash

#outBase="repo-posneg"
source config

minTripleCount=10

for dir in `find "$latcRepo" -type d`; do

	specFile="$dir/spec.xml"
	posFile="$dir/positive.nt"
        negFile="$dir/negative.nt"

	if [ -f "$specFile" -a -f "$posFile" -a -f "$negFile" ]; then

		d="${dir##/*/}"
		echo "Candidate: $d"

		tmpEndpointFile="/tmp/endpoints.txt"
		tmpPosFile="/tmp/positive.nt"
		tmpNegFile="/tmp/negative.nt" 

		./extract-endpoints.sh "$specFile" > "$tmpEndpointFile"
		cat "$posFile" | awk 1 | rapper -i ntriples - http://dummy.org | sort -u > "$tmpPosFile"
                cat "$negFile" | awk 1 | rapper -i ntriples - http://dummy.org | sort -u > "$tmpNegFile"

		posTripleCount=`cat "$tmpPosFile" | wc -l`
                negTripleCount=`cat "$tmpNegFile" | wc -l`

		if [ "$posTripleCount" -lt "$minTripleCount" -o "$negTripleCount" -lt "$minTripleCount" ]; then
			echo "Skip: $d"
			continue;
		fi
		echo "Accept: $d"

		targetDir="$posNegRepo/$d"
		mkdir -p "$targetDir"

		mv "$tmpEndpointFile" "$targetDir"
		mv "$tmpPosFile" "$targetDir"
		mv "$tmpNegFile" "$targetDir"
	fi

done


