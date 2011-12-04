#!/bin/bash

# Downloads the Saxon XSLT processor from sourceforge, extracts it to /usr/lib/saxon, and creates an executable /usr/bin/saxon

if [ "$USER" != "root" ]; then
	echo "This script must be run as root"
	exit -1
fi

baseDir="/usr/lib/saxon"
installDir="$baseDir/he9-3-0-11j"

mkdir -p "$installDir"
ln -s "$installDir" "$baseDir/current"

wget -O"$installDir/saxonhe9-3-0-11j.zip" 'http://downloads.sourceforge.net/project/saxon/Saxon-HE/9.3/saxonhe9-3-0-11j.zip?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Fsaxon%2Ffiles%2FSaxon-HE%2F9.3%2F&ts=1323001241&use_mirror=switch'

cd "$installDir"
unzip "$installDir/saxonhe9-3-0-11j.zip"

echo -e '#!/bin/bash\njava -jar '"$baseDir/current/saxon9he.jar"' "$@"\n' > /usr/bin/saxon
chmod +x /usr/bin/saxon

