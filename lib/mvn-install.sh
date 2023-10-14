#!/usr/bin/env bash

mvn install:install-file -Dfile="shiro-release.jar" -DgroupId="com.mikuac" -DartifactId="shiro" -Dversion="release" -Dpackaging=jar
mvn install:install-file -Dfile="YurnSatoriFramework-0.0.2.jar" -DgroupId="com.yurn" -DartifactId="YurnSatoriFramework" -Dversion="0.0.2" -Dpackaging=jar