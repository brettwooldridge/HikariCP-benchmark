#### JMH Connection Pool Microbenchmarks

This set of microbenchmaks was developed to refine the HikariCP JDBC connection pool implementation. Initially HikariCP used a set of "homegrown" benchmarks.  But we came to understand that benchmarking on the JVM, which employs Dead Code Elimination (DCE), lock-coalescing, inlining, loop-unrolling, on-stack replacement (OSR) and a myriad of other tricks, renders most attempts at benchmarking inaccurate at best and invalid at worst.

The Oracle JVM performance team, primarily Aleksey Shipilёv, developed a microbenchmarking framework called JMH. It provides the infrastructure (if used properly) for accurate comparative measurement of JVM-based execution.  If you are interested in microbenchmarking at all, or just curious about all the wonderful things the JVM does, I highly recommend reading [this slideshare](http://www.slideshare.net/ConstantineNosovsky/nosovsky-java-microbenchmarking).

#### How to run?
 * Clone this project
 * Run ``mvn clean package``
 * Run the ``./benchmark.sh`` script

The ``benchmark.sh`` script is a wrapper around JMH execution.  A full run of the benchmark will take over two hours.  There are two options provided by the script: ``quick`` and ``medium``.  *quick* will take about 20 minutes to run, *medium* will take about an hour.  It is extrememly boring to watch, and you can't do anything else on the PC where the benchmark is running without affecting the results, so have dinner, run some errands, etc.
