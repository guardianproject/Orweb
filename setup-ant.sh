#!/bin/sh

target="android-17"

for f in `find external/ -name project.properties`; do
projectdir=`dirname $f`
    echo "Updating ant setup in $projectdir:"
    android update lib-project -p $projectdir -t $target
done
android update project -p . --subprojects -t $target --name orweb

cp libs/android-support-v4.jar external/ActionBarSherlock/actionbarsherlock/libs/android-support-v4.jar
cp libs/android-support-v4.jar external/netcipher/libonionkit/libs/android-support-v4.jar

