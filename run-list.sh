#!/bin/bash

outBase="reports/sampled/onlyout"
inBase="data-latc"

while read line; do

line=`echo $line | grep -v "^\s*#"`

if [ -z "$line" ]; then
continue
fi

inFile="$inBase/$line"

name=${inFile%.*}
outDir="$outBase/$name"
endpoints="$name-named.txt"


cmd="java -Xmx4096M -server -d64 -jar target/qa_for_lod-0.0.1-SNAPSHOT-jar-with-dependencies.jar -onlyout -nogui -triples $inFile -out $outDir -endpoints $endpoints"

echo "$cmd"
echo ""
echo ""
echo "Processing:"
echo "---------------------------------------------"
echo "Input = $inFile"
echo "Output = $outDir"
echo "Cmd = $cmd"
echo "Endpoint = $endpoints"
$cmd
done

