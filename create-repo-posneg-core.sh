#!/bin/bash

#outBase="repo-posneg"
source config

outBase=$1
if [ -z "$outBase" ]; then
	echo "Please specify the directory for the output directory"
	exit -1
fi

minPosTripleCount=${2:-50}
minNegTripleCount=${3:-$minPosTripleCount}

echo "minPosTripleCount: $minPosTripleCount"
echo "minNegTripleCount: $minNegTripleCount"

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

		if [ "$posTripleCount" -lt "$minPosTripleCount" -o "$negTripleCount" -lt "$minNegTripleCount" ]; then
			echo "Skip: $d"
			continue;
		fi
		echo "Accept: $d"

		targetDir="$outBase/$d"
		mkdir -p "$targetDir"

		mv "$tmpEndpointFile" "$targetDir"
		mv "$tmpPosFile" "$targetDir"
		mv "$tmpNegFile" "$targetDir"
	fi

done


