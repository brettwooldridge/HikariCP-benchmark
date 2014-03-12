#### JMH Connection Pool Microbenchmarks

This set of microbenchmaks was developed to refine the HikariCP JDBC connection pool implementation, but it actually runs the same benchmarks across multiple pools.

Initially HikariCP used a set of "homegrown" benchmarks.  But we came to understand that benchmarking on the JVM, which employs Dead Code Elimination (DCE), lock-coalescing, inlining, loop-unrolling, on-stack replacement (OSR) and a myriad of other tricks, renders most attempts at benchmarking inaccurate at best and invalid at worst.

The Oracle JVM performance team, primarily Aleksey Shipilёv, developed a microbenchmarking framework called JMH. It provides the infrastructure (if used properly) for accurate comparative measurement of JVM-based execution.  If you are interested in microbenchmarking at all, or just curious about all the wonderful things the JVM does, I highly recommend reading [this slideshare](http://www.slideshare.net/ConstantineNosovsky/nosovsky-java-microbenchmarking).

#### How to run?
 * Clone this project
 * Run ``mvn clean package``
 * Run the ``./benchmark.sh`` script

The ``benchmark.sh`` script is a wrapper around JMH execution.  A full run of the benchmark will take over two hours.

There are several more options you can provide to the ``benchmark.sh``.  There are a lot actually, but these are most useful...

**Specify Shorter Runs**<br/>
There are two options provided by the script: ``quick`` and ``medium``.  *quick* will take about 20 minutes to run, *medium* will take about an hour.  It is extrememly boring to watch, and you can't do anything else on the PC where the benchmark is running without affecting the results, so have dinner, run some errands, etc.
```
./benchmark.sh quick
```
If specified with other options, ``quick`` or ``medium`` must be the first option.

-----------------------------------------------------------

**Specify Specific Pools**<br/>
```
./benchmark.sh -p pool=hikari,bone
```
Where ``pool`` is a comma-separated list (*hikari*, *bone*, *tomcat*, *c3p0*, *vibur*).

-----------------------------------------------------------

**Specify Pool Size**<br/>
```
./benchmark.sh -p maxPoolSize=4
```
Pool size is only applicable for the *Connection Cycle* test, attempting to run the *Statement Cycle* test with a pool smaller than the number of threads (8) will result in testing failures.  The *Connection Cycle* test runs with 8 threads, so to test a contrained pool condition set *maxPoolSize* to a smaller number (eg. 4).  See comments about threading below.

-----------------------------------------------------------
**Specify which Benchmark**<br/>
There are two benchmarks in the suite currently: *ConnectionBench* and *StatementBench*.  By default both benchmarks are run, but if you want to run one or the other you can use a JMH option using a regex (regular experession) to do so.  For example, to only run the *ConnectionBench* use:
```
./benchmark.sh ".*Statement.*"
```

-----------------------------------------------------------

All of the options can be combined:
```
./benchmark.sh medium -p pool=hikari,bone -p maxPoolSize=4 ".*Connection.*"
```
-----------------------------------------------------------
#### Threading
With microbenchmarks, it is typically not valid to test with more threads than cores, i.e. the results are not considered reliable.  An Intel Core i7 with 4-physcial cores and 4 HyperThread cores can be run with a thread count of 8 reliably.  The further above that number you test, the wider the margin of error.

-----------------------------------------------------------
#### Current Results

As of: 2014/03/08

*ConnectionCycle* measures cycles of ``DataSource.getConnection()/Connection.close()``. *StatementCycle* measures cycles of ``Connection.prepareStatement()``, ``Statement.execute()``, ``Statement.close()``.

[Click here to view unscaled version](https://github.com/brettwooldridge/HikariCP-benchmark/blob/master/README.md)

![](http://github.com/brettwooldridge/HikariCP/wiki/Benchmarks.png)

