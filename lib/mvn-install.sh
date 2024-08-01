#!/usr/bin/env bash

if [ $# -ne 1 ]; then
    echo "Usage: $0 <jar file>"
    exit 1
fi

echo "groupId: "
read group
echo "artifactId: "
read artifact
echo "version: "
read version

mvn install:install-file -Dfile="$1" -DgroupId="$group" -DartifactId="$artifact" -Dversion="$version" -Dpackaging=jar
