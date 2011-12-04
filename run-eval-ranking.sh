#!/bin/bash

# Mix the positive and negative links in order to determine whether the negative ones become the outliers

source config

maxRunCount=3

#outBase="reports/sampled/onlyout"
#$inBase="data-latc"

#while read line; do

#    line=`echo $line | grep -v "^\s*#"`

#    if [ -z "$line" ]; then
#        continue
#    fi

nPos=140
nNeg=10

for dir in `find "$rankingRepo" -maxdepth 1 -mindepth 1 -type d`; do

    for (( i=1; i<=$maxRunCount; ++i )); do

	posFile="$dir/positive.nt"
	negFile="$dir/negative.nt"
	endpointsFile="$dir/endpoints.txt"
	outDir="$dir/ranking/$i"


	cmd="java -Xmx4096M -server -d64 -jar target/qa_for_lod-0.0.1-SNAPSHOT-jar-with-dependencies.jar -onlyout -nogui -triples $nPos $posFile #ffaaaa $nNeg $negFile -out $outDir -endpoints $endpointsFile -seed $i"

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


