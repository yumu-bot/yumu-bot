#!/usr/bin/env bash

mvn install:install-file -Dfile="shiro-relese.jar" -DgroupId="com.mikuac" -DartifactId="shiro" -Dversion="relese" -Dpackaging=jar
mvn install:install-file -Dfile="YurnSatoriFramework-0.0.2.jar" -DgroupId="com.yurn" -DartifactId="YurnSatoriFramework" -Dversion="0.0.2" -Dpackaging=jar