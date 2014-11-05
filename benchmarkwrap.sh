for threads in 1 2 4 8 16 32
do
	./benchmark.sh medium $threads -p pool=tomcat -p maxPoolSize=1,2,4,8,16,32 ".*Trx.*"
done
