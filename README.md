[![Dependency Status](https://www.versioneye.com/user/projects/55501400f7db0da74e00017f/badge.svg?style=flat)](https://www.versioneye.com/user/projects/55501400f7db0da74e00017f)

![](https://github.com/brettwooldridge/HikariCP/wiki/HikariCP-bench-2.6.0.png)

*ConnectionCycle* measures cycles of ``DataSource.getConnection()/Connection.close()``. *StatementCycle* measures cycles of ``Connection.prepareStatement()``, ``Statement.execute()``, ``Statement.close()``.

#### JMH Connection Pool Microbenchmarks

This set of microbenchmaks was developed to refine the [HikariCP](https://github.com/brettwooldridge/HikariCP) JDBC connection pool implementation, but it actually runs the same benchmarks across multiple pools.

We have come to understand that benchmarking on the JVM, which employs Dead Code Elimination (DCE), lock-coalescing, inlining, loop-unrolling, on-stack replacement (OSR) and a myriad of other tricks, renders most attempts at benchmarking completely invalid -- *including our own original benchmarks*.  Read all the things that [even smart] [people get wrong](https://groups.google.com/forum/#!msg/mechanical-sympathy/m4opvy4xq3U/7lY8x8SvHgwJ) about benchmarking on the JVM.

The Oracle JVM performance team, primarily Aleksey Shipilёv, developed a microbenchmarking framework called JMH. It provides the infrastructure (if used properly) for accurate comparative measurement of JVM-based execution.  If you are interested in microbenchmarking at all, or just curious about all the wonderful things the JVM does, I highly recommend reading [this slideshare](http://www.slideshare.net/ConstantineNosovsky/nosovsky-java-microbenchmarking).

#### How to run?
 * ``git clone https://github.com/brettwooldridge/HikariCP-benchmark.git``
 * ``cd HikariCP-benchmark``
 * ``mvn clean package``
 * ``./benchmark.sh``

The ``benchmark.sh`` script is a wrapper around JMH execution.  A full run of the benchmark will take about 45 minutes for all pools.

There are several more options you can provide to the ``benchmark.sh``.  There are a lot actually, but these are most useful...

**Specify Shorter Runs**<br/>
There are two options provided by the script: ``quick`` and ``medium``.  *quick* will take about 5 minutes to run, *medium* will take about 20 minutes -- for all pools.  It is extrememly boring to watch, and you can't do anything else on the PC where the benchmark is running without affecting the results, so have dinner, run some errands, etc.
```
./benchmark.sh quick
```
If specified with other options, ``quick`` or ``medium`` must be the first option.

-----------------------------------------------------------

**Specify Specific Pools**<br/>
```
./benchmark.sh -p pool=hikari,bone
```
Where ``pool`` is a comma-separated list (*hikari*, *dbcp2*, *tomcat*, *c3p0*, *vibur*).  Specifying a specific pool or subset of pools will shorten run times.

-----------------------------------------------------------

**Specify Pool Size**<br/>
```
./benchmark.sh -p maxPoolSize=16
```
Pool size is only applicable for the *Connection Cycle* test, attempting to run the *Statement Cycle* test with a pool smaller than the number of threads (8) will result in testing failures.  The *Connection Cycle* test runs with 8 threads, so to test a contrained pool condition set *maxPoolSize* to a smaller number (eg. 4).

-----------------------------------------------------------
**Specify which Benchmark**<br/>
There are two benchmarks in the suite currently: *ConnectionBench* and *StatementBench*.  By default both benchmarks are run, but if you want to run one or the other you can use a JMH option using a regex (regular experession) to do so.  For example, to only run the *StatementBench* use:
```
./benchmark.sh ".*Statement.*"
```

-----------------------------------------------------------

All of the options can be combined:
```
./benchmark.sh medium -p pool=hikari,vibur -p maxPoolSize=4 ".*Connection.*"
```
-----------------------------------------------------------
