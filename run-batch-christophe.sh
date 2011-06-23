#!/bin/bash
CMD="java -Xmx4096M -server -d64 -jar target/qa_for_lod-0.0.1-SNAPSHOT-jar-with-dependencies.jar -nogui -onlyout "
#FILES="geonames-linkedgeodata-library geonames-linkedgeodata-medical_centre geonames-linkedgeodata-memorial2 geonames-linkedgeodata-memorial geonames-linkedgeodata-mountain geonames-linkedgeodata-museum geonames-linkedgeodata-parking geonames-linkedgeodata-park geonames-linkedgeodata-petrol_station geonames-linkedgeodata-pier geonames-linkedgeodata-place_of_worship geonames-linkedgeodata-police_post geonames-linkedgeodata-post_office geonames-linkedgeodata-restaurant geonames-linkedgeodata-river geonames-linkedgeodata-school geonames-linkedgeodata-shelter geonames-linkedgeodata-shop geonames-linkedgeodata-stadium geonames-linkedgeodata-university geonames-linkedgeodata-veterinary"
FILES="gho-linkedct-country gho-pubmed-country linkedgeodata-airportdata linkedgeodata-fao ourairports-ordnancesurvey stad-rmon-person geonames-linkedgeodata-library geonames-linkedgeodata-medical_centre geonames-linkedgeodata-memorial2 geonames-linkedgeodata-memorial geonames-linkedgeodata-mountain geonames-linkedgeodata-museum geonames-linkedgeodata-parking geonames-linkedgeodata-park geonames-linkedgeodata-petrol_station geonames-linkedgeodata-pier geonames-linkedgeodata-place_of_worship geonames-linkedgeodata-police_post geonames-linkedgeodata-post_office geonames-linkedgeodata-restaurant geonames-linkedgeodata-river geonames-linkedgeodata-school geonames-linkedgeodata-shelter geonames-linkedgeodata-shop geonames-linkedgeodata-stadium geonames-linkedgeodata-university geonames-linkedgeodata-veterinary"
# blacklisted :  linkedct-pubmed-country 

for file in $FILES
do
	echo "Process $file"
	params="-triples data-latc/$file.nt -endpoints data-latc/$file-named.txt"
	$CMD $params
done

