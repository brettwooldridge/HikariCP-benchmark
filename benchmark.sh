#!/bin/bash

if [[ ! -e "./target/microbenchmarks.jar" ]]; then
   mvn clean package
fi

if [[ "quick" == "$1" ]]; then
   java -jar ./target/microbenchmarks.jar -jvmArgs "-server" -wi 8 -i 8 -t 8 -f 2 $2 $3 $4 $5 $6 $7 $8 $9
elif [[ "medium" == "$1" ]]; then
   java -jar ./target/microbenchmarks.jar -jvmArgs "-server" -t 8 -f 3 $2 $3 $4 $5 $6 $7 $8 $9
elif [[ "profile" == "$1" ]]; then
   java -server -agentpath:/Applications/jprofiler8/bin/macos/libjprofilerti.jnilib=port=8849 -jar ./target/microbenchmarks.jar -r 5 -wi 8 -i 8 -t 8 -f 0 $2 $3 $4 $5 $6 $7 $8 $9
else
   java -jar ./target/microbenchmarks.jar -jvmArgs "-server" -wi 15 -i 10 -t 8 $1 $2 $3 $4 $5 $6 $7 $8 $9
fi
