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

for dir in `find "$posNegRepo" -maxdepth 1 -mindepth 1 -type d`; do

    for (( i=1; i<=$maxRunCount; ++i )); do

	posFile="$dir/positive.nt"
	negFile="$dir/negative.nt"
	endpointsFile="$dir/endpoints.txt"
	posOutDir="$dir/$i/positive"
	negOutDir="$dir/$i/negative"

       cmdPos="java -Xmx4096M -server -d64 -jar target/qa_for_lod-0.0.1-SNAPSHOT-jar-with-dependencies.jar -onlyout -nogui -triples $n $posFile -out $posOutDir -endpoints $endpointsFile -seed $i"

	cmdNeg="java -Xmx4096M -server -d64 -jar target/qa_for_lod-0.0.1-SNAPSHOT-jar-with-dependencies.jar -onlyout -nogui -triples $n $negFile -out $negOutDir -endpoints $endpointsFile -seed $i"

       echo "$cmd"
       echo ""

       echo ""
       echo "Processing:"
       echo "---------------------------------------------"
	echo "Source Directory            : $dir"
	echo "Output Directory (positive) : $posOutDir"
	echo "Output Directory (negative) : $negOutDir"
	echo "Seed                        : $i"
#       echo "Cmd = $cmd"
	echo "Endpoint                    : $endpointsFile"
       $cmdPos
	$cmdNeg
    done
done


