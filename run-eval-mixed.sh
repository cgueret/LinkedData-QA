#!/bin/bash

# Perform a multi-run evaluation by samplingg accross ALL positive or negative, respectively repos

source config

maxRunCount=10

#outBase="reports/sampled/onlyout"
#$inBase="data-latc"

#while read line; do

#    line=`echo $line | grep -v "^\s*#"`

#    if [ -z "$line" ]; then
#        continue
#    fi

max=150
n=50

type="$1"

if [ -z "$type" ]; then
	echo "Please specify positive or negative"
	exit
fi

args=""
endpoints=""

for dir in `find "$mixedRepo" -maxdepth 1 -mindepth 1 -type d`; do

	candidateFile="$dir/$type.nt"
	if [ -f $candidateFile ]; then
		export args="$args $n $candidateFile"

		endpointsFile="$dir/endpoints.txt"

		export endpoints="$endpoints $endpointsFile"
	fi

done

#echo "$args"
#exit 0

    for (( i=1; i<=$maxRunCount; ++i )); do

	outDir="$mixedRepo/$type/$i"


	cmd="java -Xmx4096M -server -d64 -jar target/qa_for_lod-0.0.1-SNAPSHOT-jar-with-dependencies.jar -onlyout -nogui -triples $args -out $outDir -endpoints $endpoints -seed $i -permissive"

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


