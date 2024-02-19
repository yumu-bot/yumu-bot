@echo off
mvn install:install-file -Dfile="shiro-release.jar" -DgroupId="com.mikuac" -DartifactId="shiro" -Dversion="release" -Dpackaging=jar
mvn install:install-file -Dfile="YurnSatoriFramework-0.0.2.jar" -DgroupId="com.yurn" -DartifactId="YurnSatoriFramework" -Dversion="0.0.2" -Dpackaging=jar
mvn install:install-file -Dfile="oirc-1.0-SNAPSHOT.jar" -DgroupId="xyz.365246692.oirc" -DartifactId="oirc" -Dversion="0.0.1" -Dpackaging=jar