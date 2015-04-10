#!/bin/bash

JAVA_OPTIONS="-server -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xmx512m"

JMH_THREADS="-t 8"
if [[ "$2" == "-t" ]]; then
   JMH_THREADS="-t $3"
   set -- "$1" "${@:4}"
fi

if [[ ! -e "./target/microbenchmarks.jar" ]]; then
   mvn clean package
fi

if [[ "quick" == "$1" ]]; then
   java -jar ./target/microbenchmarks.jar -jvmArgs "$JAVA_OPTIONS" -wi 3 -i 8 $JMH_THREADS -f 2 $2 $3 $4 $5 $6 $7 $8 $9
elif [[ "medium" == "$1" ]]; then
   java -jar ./target/microbenchmarks.jar -jvmArgs "$JAVA_OPTIONS" -wi 3 -t 8 -f 3 $2 $3 $4 $5 $6 $7 $8 $9
elif [[ "profile" == "$1" ]]; then
   java -server $JAVA_OPTIONS -agentpath:/Applications/jprofiler8/bin/macos/libjprofilerti.jnilib=port=8849 -jar ./target/microbenchmarks.jar -r 5 -wi 3 -i 8 -t 8 -f 0 $2 $3 $4 $5 $6 $7 $8 $9
elif [[ "debug" == "$1" ]]; then
   java -server $JAVA_OPTIONS -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y -jar ./target/microbenchmarks.jar -r 5 -wi 3 -i 8 -t 8 -f 0 $2 $3 $4 $5 $6 $7 $8 $9
else
   java -jar ./target/microbenchmarks.jar -jvmArgs "$JAVA_OPTIONS" -wi 3 -i 15 -t 8 $1 $2 $3 $4 $5 $6 $7 $8 $9
fi
