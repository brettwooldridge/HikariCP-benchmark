#!/bin/bash

JAVA_OPTIONS="-server -XX:-RestrictContended -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xms1096m -Xmx1096m"

if [[ "clean" == "$1" ]]; then
   mvn clean package
   shift
fi


java -cp ./target/microbenchmarks.jar:./target/test-classes $JAVA_OPTIONS com.zaxxer.hikari.benchmark.SpikeLoadTest $1 $2 $3 $4 $5 $6 $7 $8 $9
