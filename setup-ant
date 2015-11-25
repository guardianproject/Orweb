#!/bin/sh

if ! which android > /dev/null; then
    if [ -z $ANDROID_HOME ]; then
        if [ -e ~/.android/bashrc ]; then
            . ~/.android/bashrc
        else
            echo "'android' not found, ANDROID_HOME must be set!"
            exit
        fi
    else
        export PATH="${ANDROID_HOME}/tools:$PATH"
    fi
fi
projectname=`sed -n 's,.*name="app_name">\(.*\)<.*,\1,p' res/values/strings.xml`

# fetch target from project.properties
eval `grep '^target=' project.properties`

for f in `find external/ -name project.properties`; do
projectdir=`dirname $f`
    echo "Updating ant setup in $projectdir:"
    android update lib-project -p $projectdir -t $target
done
android update project -p . --subprojects -t $target --name $projectname

cp libs/android-support-v4.jar external/netcipher/libnetcipher/libs/android-support-v4.jar

