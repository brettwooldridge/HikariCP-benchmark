#### Benchmark results as of 2014/02/26

##### Core i7
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

##### Core i5
```
Benchmark                                      (pool)   Mode   Samples         Mean   Mean error    Units
c.z.h.b.ConnectionBench.testConnection           bone  thrpt       100     2023.072       16.928   ops/ms
c.z.h.b.ConnectionBench.testConnection           c3p0  thrpt       100      270.428        3.062   ops/ms
c.z.h.b.ConnectionBench.testConnection         hikari  thrpt       100     6864.158      142.339   ops/ms
c.z.h.b.ConnectionBench.testConnection         tomcat  thrpt       100     1593.279       31.570   ops/ms
c.z.h.b.StatementBench.testPreparedStatement     bone  thrpt       100     9265.518      112.143   ops/ms
c.z.h.b.StatementBench.testPreparedStatement     c3p0  thrpt       100     4312.603       38.862   ops/ms
c.z.h.b.StatementBench.testPreparedStatement   hikari  thrpt       100    81525.157     1353.693   ops/ms
c.z.h.b.StatementBench.testPreparedStatement   tomcat  thrpt       100    52746.594     2194.556   ops/ms
c.z.h.b.StatementBench.testStatement             bone  thrpt       100     9035.818       93.496   ops/ms
c.z.h.b.StatementBench.testStatement             c3p0  thrpt       100     4702.436       64.690   ops/ms
c.z.h.b.StatementBench.testStatement           hikari  thrpt       100    83577.974     1725.596   ops/ms
c.z.h.b.StatementBench.testStatement           tomcat  thrpt       100    54129.247      647.426   ops/ms
```
