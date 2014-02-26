##### Benchmark results as of 2014/02/26

```
Benchmark                                      (pool)   Mode   Samples         Mean   Mean error    Units
c.z.h.b.StatementBench.testPreparedStatement     c3p0  thrpt        50    10030.327       78.519   ops/ms
c.z.h.b.StatementBench.testPreparedStatement     bone  thrpt        50    19247.212      400.775   ops/ms
c.z.h.b.StatementBench.testPreparedStatement   tomcat  thrpt        50   130633.151     3780.265   ops/ms
c.z.h.b.StatementBench.testPreparedStatement   hikari  thrpt        50   138602.485    10989.573   ops/ms

Benchmark                                      (pool)   Mode   Samples         Mean   Mean error    Units
c.z.h.b.StatementBench.testStatement             c3p0  thrpt        50    10986.444      166.185   ops/ms
c.z.h.b.StatementBench.testStatement             bone  thrpt        50    19924.987      236.259   ops/ms
c.z.h.b.StatementBench.testStatement           tomcat  thrpt        50   147461.538     3464.517   ops/ms
c.z.h.b.StatementBench.testStatement           hikari  thrpt        50   156471.167    12674.897   ops/ms

Benchmark                                      (pool)   Mode   Samples         Mean   Mean error    Units
c.z.h.b.ConnectionBench.testConnection           c3p0  thrpt        50      378.237        2.213   ops/ms
c.z.h.b.ConnectionBench.testConnection         tomcat  thrpt        50     2038.786        8.925   ops/ms
c.z.h.b.ConnectionBench.testConnection           bone  thrpt        50     2571.159       45.731   ops/ms
c.z.h.b.ConnectionBench.testConnection         hikari  thrpt        50     5695.939      194.770   ops/ms
```
