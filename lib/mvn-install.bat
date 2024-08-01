@echo off

if "%~1"=="" (
    echo Usage: %0 ^<jar file^>
    exit /b 1
)

set /p group="groupId: "
set /p artifact="artifactId: "
set /p version="version: "

mvn install:install-file -Dfile="%~1" -DgroupId="%group%" -DartifactId="%artifact%" -Dversion="%version%" -Dpackaging=jar