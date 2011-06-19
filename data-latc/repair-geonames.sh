for x in `ls -1 *.nt`; do
echo "$x"
cat "$x" | sed -r 's|(geonames\.org/[0-9]+)>|\1/>|g' > tmp.out
mv tmp.out "$x"
done

