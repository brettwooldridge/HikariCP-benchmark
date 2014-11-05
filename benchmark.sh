#!/bin/bash

JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.25-0.b18.4.amzn1.x86_64/
PATH=$PATH:~/apache-maven-3.2.3/bin/

JAVA_OPTIONS="-server -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xmx5120m \
  -Djavax.management.builder.initial= \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.local.only=false \
  -Dcom.sun.management.jmxremote.password.file=/home/ec2-user/tomcat/conf/jmxremote.password \
  -Dcom.sun.management.jmxremote.access.file=/home/ec2-user/tomcat/conf/jmxremote.access"



if [[ ! -e "./target/microbenchmarks.jar" ]]; then
   mvn clean package
fi

if [[ "quick" == "$1" ]]; then
   java -jar ./target/microbenchmarks.jar -jvmArgs "$JAVA_OPTIONS" -wi 3 -i 8 -t $2 -f 2 $3 $4 $5 $6 $7 $8 $9
elif [[ "medium" == "$1" ]]; then
   java -jar ./target/microbenchmarks.jar -jvmArgs "$JAVA_OPTIONS" -wi 3 -t $2 -f 3 $3 $4 $5 $6 $7 $8 $9
elif [[ "profile" == "$1" ]]; then
   java -server $JAVA_OPTIONS -agentpath:/Applications/jprofiler8/bin/macos/libjprofilerti.jnilib=port=8849 -jar ./target/microbenchmarks.jar -r 5 -wi 3 -i 8 -t $2 -f 0 $3 $4 $5 $6 $7 $8 $9
elif [[ "debug" == "$1" ]]; then
   java -server $JAVA_OPTIONS -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y -jar ./target/microbenchmarks.jar -r 5 -wi 3 -i 8 -t $2 -f 0 $3 $4 $5 $6 $7 $8 $9
else
   java -jar ./target/microbenchmarks.jar -jvmArgs "$JAVA_OPTIONS" -wi 3 -i 15 -t $1 $2 $3 $4 $5 $6 $7 $8 $9
fi
