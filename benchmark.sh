#!/bin/bash

JAVA_OPTIONS="-server -XX:-RestrictContended -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xms1096m -Xmx1096m"

if [[ "clean" == "$1" ]]; then
   mvn clean package
   shift
fi

if [[ "gcprof" == "$1" ]]; then
   JAVA_OPTIONS="$JAVA_OPTIONS -Xloggc:gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps"
   shift
fi

JMH_THREADS="-t 8"
if [[ "$2" == "-t" ]]; then
   JMH_THREADS="-t $3"
   set -- "$1" "${@:4}"
fi

if [[ "quick" == "$1" ]]; then
   java -jar ./target/microbenchmarks.jar -jvmArgs "$JAVA_OPTIONS" -wi 3 -i 8 $JMH_THREADS -f 2 $2 $3 $4 $5 $6 $7 $8 $9
elif [[ "medium" == "$1" ]]; then
   java -jar ./target/microbenchmarks.jar -jvmArgs "$JAVA_OPTIONS" -wi 3 -f 8 -i 6 $JMH_THREADS $2 $3 $4 $5 $6 $7 $8 $9
elif [[ "long" == "$1" ]]; then
   java -jar ./target/microbenchmarks.jar -jvmArgs "$JAVA_OPTIONS" -wi 3 -i 15 $JMH_THREADS $2 $3 $4 $5 $6 $7 $8 $9
elif [[ "profile" == "$1" ]]; then
   java -server $JAVA_OPTIONS -agentpath:/Applications/jprofiler8/bin/macos/libjprofilerti.jnilib=port=8849 -jar ./target/microbenchmarks.jar -r 5 -wi 3 -i 8 $JMH_THREADS -f 0 $2 $3 $4 $5 $6 $7 $8 $9
elif [[ "debug" == "$1" ]]; then
   java -server $JAVA_OPTIONS -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y -jar ./target/microbenchmarks.jar -r 5 -wi 3 -i 8 -t 8 -f 0 $2 $3 $4 $5 $6 $7 $8 $9
else
   java -jar ./target/microbenchmarks.jar -jvmArgs "$JAVA_OPTIONS" -wi 3 -i 15 -t 8 $1 $2 $3 $4 $5 $6 $7 $8 $9
fi

