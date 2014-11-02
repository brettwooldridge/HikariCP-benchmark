for threads in 1 2 4 8 16 32
do
	./benchmark.sh medium $threads -p pool=c3p0,hikari,jdbc,dbcp,c3p0-ht6 -p maxPoolSize=1,2,4,8,16,32 ".*Trx.*"
done
