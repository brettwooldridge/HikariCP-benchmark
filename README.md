#### Benchmark results as of 2014/03/02 - HikariCP 1.3.1-SNAPSHOT

##### Core i5
```
Benchmark                                 (pool)   Mode   Samples         Mean   Mean error    Units
c.z.h.b.ConnectionBench.cycleCnnection      c3p0  thrpt       100       79.581        0.528   ops/ms
c.z.h.b.ConnectionBench.cycleCnnection    tomcat  thrpt       100     1291.787       18.857   ops/ms
c.z.h.b.ConnectionBench.cycleCnnection      bone  thrpt       100     1993.719       19.132   ops/ms
c.z.h.b.ConnectionBench.cycleCnnection    hikari  thrpt       100     6878.487      133.641   ops/ms

c.z.h.b.ConnectionBench.closeConnection     c3p0  thrpt       100      222.691        2.390   ops/ms
c.z.h.b.ConnectionBench.closeConnection   tomcat  thrpt       100     1982.904       30.690   ops/ms
c.z.h.b.ConnectionBench.closeConnection     bone  thrpt       100     2518.267      210.510   ops/ms
c.z.h.b.ConnectionBench.closeConnection   hikari  thrpt       100    11499.908      260.961   ops/ms

c.z.h.b.ConnectionBench.getConnection       c3p0  thrpt       100      116.720        0.945   ops/ms
c.z.h.b.ConnectionBench.getConnection     tomcat  thrpt       100     2767.765       84.827   ops/ms
c.z.h.b.ConnectionBench.getConnection     hikari  thrpt       100    10690.672      297.902   ops/ms
c.z.h.b.ConnectionBench.getConnection       bone  thrpt       100    12396.906     1596.514   ops/ms

c.z.h.b.StatementBench.cycleSatement        c3p0  thrpt       100     6391.655      107.164   ops/ms
c.z.h.b.StatementBench.cycleSatement        bone  thrpt       100    11291.918      106.988   ops/ms
c.z.h.b.StatementBench.cycleSatement      tomcat  thrpt       100    16712.380      115.013   ops/ms
c.z.h.b.StatementBench.cycleSatement      hikari  thrpt       100    42392.460     1363.968   ops/ms

c.z.h.b.StatementBench.abandonStatement     c3p0  thrpt       100      205.439        2.453   ops/ms
c.z.h.b.StatementBench.abandonStatement     bone  thrpt       100     1810.269       80.821   ops/ms
c.z.h.b.StatementBench.abandonStatement   tomcat  thrpt       100     1966.199       30.170   ops/ms
c.z.h.b.StatementBench.abandonStatement   hikari  thrpt       100    11658.869      264.743   ops/ms

c.z.h.b.StatementBench.closeStatement       c3p0  thrpt       100    13599.157      287.791   ops/ms
c.z.h.b.StatementBench.closeStatement       bone  thrpt       100    51632.826     1095.151   ops/ms
c.z.h.b.StatementBench.closeStatement     hikari  thrpt       100    57358.759     2615.237   ops/ms
c.z.h.b.StatementBench.closeStatement     tomcat  thrpt       100   105256.978      617.305   ops/ms

c.z.h.b.StatementBench.prepareStatement     c3p0  thrpt       100     8879.437       91.770   ops/ms
c.z.h.b.StatementBench.prepareStatement     bone  thrpt       100    12310.751      141.998   ops/ms
c.z.h.b.StatementBench.prepareStatement   tomcat  thrpt       100    15820.841      163.833   ops/ms
c.z.h.b.StatementBench.prepareStatement   hikari  thrpt       100    49107.673     1825.294   ops/ms
```
