@echo off
setlocal

set JAVA_OPTIONS=-server -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xms1096m -Xmx1096m

call mvn clean package

set JMH_THREADS=-t 8
if "%2" == "-t" (
   set JMH_THREADS=-t %3
   shift /2
   shift /2
)

set command=%~1

if "quick" == "%command%" (
   java -jar target\microbenchmarks.jar -jvmArgs "%JAVA_OPTIONS%" -wi 3 -i 8 %JMH_THREADS% -f 2 %2 %3 %4 %5 %6 %7
)
if "medium" == "%command%" (
   java -jar target\microbenchmarks.jar -jvmArgs "%JAVA_OPTIONS%" -wi 3 -f 8 -i 6 %JMH_THREADS% %2 %3 %4 %5 %6 %7
)
if "long" == "%command%" (
   java -jar target\microbenchmarks.jar -jvmArgs "%JAVA_OPTIONS%" -wi 5 -f 20 -i 15 %JMH_THREADS% %2 %3 %4 %5 %6 %7
)


rem java -jar ./target/microbenchmarks.jar -jvmArgs "$JAVA_OPTIONS" -wi 3 -i 15 -t 8 $1 $2 $3 $4 $5 $6 $7 $8 $9
