#!/bin/bash

# Perform a multi-run evaluation on a specific directory.

source config

maxRunCount=1

#outBase="reports/sampled/onlyout"
#$inBase="data-latc"

#while read line; do

#    line=`echo $line | grep -v "^\s*#"`

#    if [ -z "$line" ]; then
#        continue
#    fi

n=50

type="$1"

if [ -z "$type" ]; then
	echo "Please specify positive or negative"
	exit
fi

for dir in `find "$posNegRepo" -maxdepth 1 -mindepth 1 -type d`; do

    for (( i=1; i<=$maxRunCount; ++i )); do

	file="$dir/$type.nt"
	endpointsFile="$dir/endpoints.txt"
	outDir="$dir/$type/$i"


	cmd="java -Xmx4096M -server -d64 -jar target/qa_for_lod-0.0.1-SNAPSHOT-jar-with-dependencies.jar -onlyout -nogui -triples $n $file -out $outDir -endpoints $endpointsFile -seed $i"

       echo "$cmd"
       echo ""

       echo ""
       echo "Processing:"
       echo "---------------------------------------------"
	echo "Type            : $type"
	echo "Source Directory: $dir"
	echo "Output Directory: $outDir"
	echo "Seed            : $i"
	echo "Endpoint        : $endpointsFile"
	$cmd
    done
done


