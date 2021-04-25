package com.zaxxer.hikari.benchmark;

import static com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry.STATE_IN_USE;
import static com.zaxxer.hikari.util.ConcurrentBag.IConcurrentBagEntry.STATE_NOT_IN_USE;
import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;
import static java.lang.Integer.parseInt;
import static java.lang.System.nanoTime;
import static java.lang.Thread.MAX_PRIORITY;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.DbcpPoolAccessor;
import org.apache.commons.dbcp2.TomcatPoolAccessor;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.PooledConnection;
import org.apache.tomcat.jdbc.pool.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vibur.dbcp.ViburDBCPDataSource;
import org.vibur.dbcp.pool.Hook.CloseConnection;
import org.vibur.dbcp.pool.Hook.GetConnection;
import org.vibur.dbcp.pool.Hook.InitConnection;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.benchmark.stubs.StubDriver;
import com.zaxxer.hikari.benchmark.stubs.StubStatement;
import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.pool.HikariPoolAccessor;

public class SpikeLoadTest
{
   private static final Logger LOGGER = LoggerFactory.getLogger(SpikeLoadTest.class);

   public static final String jdbcUrl = "jdbc:stub";

   private static final int MIN_POOL_SIZE = 5;
   private static final int MAX_POOL_SIZE = 50;

   private DataSource DS;

   private int requestCount;

   private String pool;

   private DbcpPoolAccessor dbcpPool;

   private ViburPoolHooks viburPool;

   private HikariPoolAccessor hikariPoolAccessor;

   private AtomicInteger threadsRemaining;

   private AtomicInteger threadsPending;

   private ComboPooledDataSource c3p0;

   private TomcatPoolAccessor tomcat;

   private DruidDataSource druid;

   public static void main(String[] args) throws InterruptedException
   {
      SpikeLoadTest test = new SpikeLoadTest();

      test.setup(args);

      test.start(parseInt(args[0]));
   }

   private void setup(String[] args)
   {
      try {
         Class.forName("com.zaxxer.hikari.benchmark.stubs.StubDriver");
         StubDriver driver = (StubDriver) DriverManager.getDriver(jdbcUrl);
         LOGGER.info("Using driver ({}): {}", jdbcUrl, driver);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }

      pool = args[1];
      IntStream.of(0, 1).forEach( i -> {
         switch (pool) {
         case "hikari":
            setupHikari();
            hikariPoolAccessor = new HikariPoolAccessor(getHikariPool(DS));
            break;
         case "dbcp2":
            setupDbcp2();
            dbcpPool = (DbcpPoolAccessor) DS;
            break;
         case "c3p0":
            setupC3P0();
            c3p0 = (ComboPooledDataSource) DS;
            break;
         case "tomcat":
            setupTomcat();
            tomcat = (TomcatPoolAccessor) DS;
            break;
         case "vibur":
            setupVibur();
            break;
         case "druid":
            setupDruid();
            break;
         default:
            throw new IllegalArgumentException("Unknown connection pool specified");
         }

         if (i == 0) {
            try {
               LOGGER.info("Warming up pool...");
               LOGGER.info("Warmup blackhole {}", warmupPool());
               shutdownPool(DS);
            }
            catch (InterruptedException e) {
            }
         }
      });

      quietlySleep(SECONDS.toMillis(2));

      this.requestCount = parseInt(args[2]);
   }

   private void start(int connectDelay) throws InterruptedException
   {
      List<RequestThread> list = new ArrayList<>();
      for (int i = 0; i < requestCount; i++) {
         RequestThread rt = new RequestThread();
         list.add(rt);
      }

      StubDriver.setConnectDelayMs(connectDelay);
      StubStatement.setExecuteDelayMs(2L);

      Timer timer = new Timer(true);
      ExecutorService executor = Executors.newFixedThreadPool(50); /*, new ThreadFactory() {
         @Override
         public Thread newThread(Runnable r)
         {
            Thread t = new Thread(r);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
         }
      }); */

      quietlySleep(SECONDS.toMillis(3));

      threadsRemaining = new AtomicInteger(requestCount);
      threadsPending = new AtomicInteger(0);

      LOGGER.info("SpikeLoadTest starting.");

      currentThread().setPriority(MAX_PRIORITY);

      timer.schedule(new TimerTask() {
         public void run() {
            for (int i = 0; i < requestCount; i++) {
               final Runnable runner = list.get(i);
                  executor.execute(runner);
               }
            }
         }, 1);

      final long startTime = nanoTime();

      List<PoolStatistics> statsList = new ArrayList<>();
      PoolStatistics poolStatistics;
      do {
         poolStatistics = getPoolStatistics(startTime, threadsPending.get());
         statsList.add(poolStatistics);

         final long spinStart = nanoTime();
         do {
            // spin
         } while (nanoTime() - spinStart < 250_000 /* 0.1ms */);
      }
      while (threadsRemaining.get() > 0  || poolStatistics.activeConnections > 0);

      long endTime = nanoTime();

      executor.shutdown();

      LOGGER.info("SpikeLoadTest completed in {}ms", MILLISECONDS.convert(endTime - startTime, NANOSECONDS));

      dumpStats(statsList, list);
   }

   private void dumpStats(List<PoolStatistics> statsList, List<RequestThread> list)
   {
      System.out.println(String.format("%10s%8s%8s%8s%8s", "Time", "Total", "Active", "Idle", "Wait"));
      for (PoolStatistics stats : statsList) {
         System.out.println(stats);
      }

      System.out.println("\n" + String.format("%10s%8s%8s%20s", "Total", "Conn", "Query", "Thread"));
      for (RequestThread req : list) {
         System.out.println(req);
      }
   }

   private class RequestThread extends TimerTask implements Runnable
   {
      @SuppressWarnings("unused")
      Exception exception;
      String name;
      long startTime;
      long endTime;
      long connectTime;
      long queryTime;

      @Override
      public void run()
      {
         name = currentThread().getName();

         threadsPending.incrementAndGet();
         startTime = nanoTime();
         try (Connection connection = DS.getConnection()) {
            connectTime = nanoTime();
            threadsPending.decrementAndGet();
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT x FROM faux")) {
               queryTime = nanoTime();
            }
         }
         catch (SQLException e) {
            exception = e;
         }
         finally {
            endTime = nanoTime();
            threadsRemaining.decrementAndGet();
         }
      }

      @Override
      public String toString()
      {
         return String.format("%10d%8d%8d%20s",
                              NANOSECONDS.toMicros(endTime - startTime),
                              NANOSECONDS.toMicros(connectTime - startTime),
                              NANOSECONDS.toMicros(queryTime - connectTime),
                              name);
      }
   }

   private static class PoolStatistics
   {
      long timestamp = nanoTime();
      int activeConnections;
      int idleConnections;
      int pendingThreads;
      int totalConnections;

      PoolStatistics(final long baseTime) {
         timestamp = nanoTime() - baseTime;
      }

      @Override
      public String toString()
      {
         return String.format("%10d%8d%8d%8d%8d", NANOSECONDS.toMicros(timestamp), totalConnections, activeConnections, idleConnections, pendingThreads);
      }
   }

   private PoolStatistics getPoolStatistics(final long baseTime, int remaining)
   {
      PoolStatistics stats = new PoolStatistics(baseTime);

      switch (pool) {
      case "hikari":
         final int[] poolStateCounts = hikariPoolAccessor.getPoolStateCounts();
         stats.activeConnections = poolStateCounts[STATE_IN_USE];
         stats.idleConnections = poolStateCounts[STATE_NOT_IN_USE];
         stats.totalConnections = poolStateCounts[4];
         stats.pendingThreads = remaining;
         break;
      case "dbcp2":
         stats.activeConnections = dbcpPool.getNumActive();
         stats.idleConnections = dbcpPool.getNumIdle();
         stats.totalConnections = dbcpPool.getNumTotal();
         stats.pendingThreads = remaining;
         break;
      case "tomcat":
         stats.activeConnections = tomcat.getNumActive();
         stats.idleConnections = tomcat.getNumIdle();
         stats.totalConnections = tomcat.getNumTotal();
         stats.pendingThreads = remaining;
         break;
      case "c3p0":
         try {
            stats.activeConnections = c3p0.getNumBusyConnectionsDefaultUser();
            stats.idleConnections = c3p0.getNumIdleConnectionsDefaultUser();
            stats.totalConnections = c3p0.getNumConnectionsDefaultUser();
            stats.pendingThreads = remaining;
         }
         catch (SQLException e) {
            throw new RuntimeException(e);
         }
         break;
      case "vibur":
         stats.activeConnections = viburPool.getActive();
         stats.idleConnections = viburPool.getIdle();
         stats.totalConnections = viburPool.getTotal();
         stats.pendingThreads = remaining;
         break;
      case "druid":
         stats.activeConnections = druid.getActiveCount();
         stats.idleConnections = druid.getMinIdle();
         stats.totalConnections = (int) druid.getCreateCount();
         stats.pendingThreads = remaining;
         break;
      }

      return stats;
   }

   private long warmupPool() throws InterruptedException
   {
      final LongAdder j = new LongAdder();
      ExecutorService executor = Executors.newFixedThreadPool(10);
      for (int k = 0; k < 10; k++) {
         executor.execute(() -> {
            for (int i = 0; i < 100_000; i++) {
               try (Connection connection = DS.getConnection();
                    Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT x FROM faux")) {
                  j.add(resultSet.getInt(i));
               }
               catch (SQLException e) {
                  throw new RuntimeException(e);
               }
            }
         });
      }

      executor.shutdown();
      executor.awaitTermination(60, SECONDS);

      return j.sum();
   }

   private void setupDbcp2()
   {
      BasicDataSource ds = new DbcpPoolAccessor();
      ds.setUrl(jdbcUrl);
      ds.setUsername("brettw");
      ds.setPassword("");
      ds.setInitialSize(MIN_POOL_SIZE);
      ds.setMinIdle(MIN_POOL_SIZE);
      ds.setMaxIdle(MAX_POOL_SIZE);
      ds.setMaxTotal(MAX_POOL_SIZE);
      ds.setMaxWaitMillis(8000);

      ds.setSoftMinEvictableIdleTimeMillis(MINUTES.toMillis(10));
      ds.setTimeBetweenEvictionRunsMillis(SECONDS.toMillis(30));

      ds.setDefaultAutoCommit(false);
      ds.setRollbackOnReturn(true);
      ds.setEnableAutoCommitOnReturn(false);
      ds.setTestOnBorrow(true);
      ds.setCacheState(true);
      ds.setFastFailValidation(true);

      try {
         // forces internal pool creation
         ds.getLogWriter();
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }

      DS = ds;
   }

   private void setupHikari()
   {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(jdbcUrl);
      config.setUsername("brettw");
      config.setPassword("");
      config.setMinimumIdle(MIN_POOL_SIZE);
      config.setMaximumPoolSize(MAX_POOL_SIZE);
      config.setConnectionTimeout(8000);
      config.setAutoCommit(false);

      DS = new HikariDataSource(config);
   }

   protected void setupDruid() {
      DruidDataSource dataSource = new DruidDataSource();

      dataSource.setInitialSize(MIN_POOL_SIZE);
      dataSource.setMaxActive(MAX_POOL_SIZE);
      dataSource.setMinIdle(MIN_POOL_SIZE);
      dataSource.setMaxIdle(MAX_POOL_SIZE);
      dataSource.setPoolPreparedStatements(true);
      dataSource.setDriverClassName("com.zaxxer.hikari.benchmark.stubs.StubDriver");
      dataSource.setUrl(jdbcUrl);
      dataSource.setUsername("brettw");
      dataSource.setPassword("");
      dataSource.setValidationQuery("SELECT 1");
      dataSource.setTestOnBorrow(true);
      dataSource.setDefaultAutoCommit(false);
      dataSource.setMaxWait(8000);
      dataSource.setUseUnfairLock(true);

      druid = dataSource;

      DS = dataSource;
   }

   protected void setupTomcat()
   {
       PoolProperties props = new PoolProperties();
       props.setUrl(jdbcUrl);
       props.setDriverClassName("com.zaxxer.hikari.benchmark.stubs.StubDriver");
       props.setUsername("brettw");
       props.setPassword("");
       props.setInitialSize(MIN_POOL_SIZE);
       props.setMinIdle(MIN_POOL_SIZE);
       props.setMaxIdle(MAX_POOL_SIZE);
       props.setMaxActive(MAX_POOL_SIZE);
       props.setMaxWait(8000);

       props.setDefaultAutoCommit(false);

       props.setRollbackOnReturn(true);
       props.setUseDisposableConnectionFacade(true);
       props.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState"); //;org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
       props.setTestOnBorrow(true);
       props.setValidationInterval(250);
       props.setValidator(new Validator() {
           @Override
           public boolean validate(Connection connection, int validateAction)
           {
               try {
                   return (validateAction == PooledConnection.VALIDATE_BORROW ? connection.isValid(0) : true);
               }
               catch (SQLException e)
               {
                   return false;
               }
           }
       });

       DS = new TomcatPoolAccessor(props);
   }

   private void setupC3P0()
   {
      try {
         ComboPooledDataSource cpds = new ComboPooledDataSource();
         cpds.setJdbcUrl(jdbcUrl);
         cpds.setUser("brettw");
         cpds.setPassword("");
         cpds.setAcquireIncrement(1);
         cpds.setInitialPoolSize(MIN_POOL_SIZE);
         cpds.setNumHelperThreads(2);
         cpds.setMinPoolSize(MIN_POOL_SIZE);
         cpds.setMaxPoolSize(MAX_POOL_SIZE);
         cpds.setCheckoutTimeout(8000);
         cpds.setLoginTimeout(8);
         cpds.setTestConnectionOnCheckout(true);

         try (Connection connection = cpds.getConnection()) {
            // acquire and close to poke the pool into action
         }

         DS = cpds;
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private void setupVibur()
   {
      ViburDBCPDataSource vibur = new ViburDBCPDataSource();
      vibur.setJdbcUrl(jdbcUrl);
      vibur.setUsername("brettw");
      vibur.setPassword("");
      vibur.setPoolFair(false);
      vibur.setPoolInitialSize(MIN_POOL_SIZE);
      vibur.setPoolMaxSize(MAX_POOL_SIZE);
      vibur.setDefaultAutoCommit(false);
      vibur.setResetDefaultsAfterUse(true);
      vibur.setConnectionIdleLimitInSeconds(30);

      vibur.setReducerTimeIntervalInSeconds((int) MINUTES.toSeconds(10));

      viburPool = new ViburPoolHooks();

      vibur.getConnHooks().addOnInit(viburPool.getInitHook());
      vibur.getConnHooks().addOnGet(viburPool.getGetHook());
      vibur.getConnHooks().addOnClose(viburPool.getCloseHook());

      vibur.start();

      DS = vibur;
   }

   private void shutdownPool(DataSource ds)
   {
      if (ds instanceof AutoCloseable) {
         try {
            ((AutoCloseable) ds).close();
         }
         catch (Exception e) {
         }
      }
      else if (ds instanceof ComboPooledDataSource) {
         ((ComboPooledDataSource) ds).close();
      }
   }

   private static HikariPool getHikariPool(DataSource ds)
   {
      try {
         Field field = ds.getClass().getDeclaredField("pool");
         field.setAccessible(true);
         return (HikariPool) field.get(ds);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private static class ViburPoolHooks
   {
      private AtomicInteger created = new AtomicInteger();
      private AtomicInteger active = new AtomicInteger();

      int getTotal()
      {
         return created.get();
      }

      int getActive()
      {
         return active.get();
      }

      int getIdle()
      {
         return created.get() - active.get();
      }

      InitHook getInitHook()
      {
         return new InitHook();
      }

      GetHook getGetHook()
      {
         return new GetHook();
      }

      CloseHook getCloseHook()
      {
         return new CloseHook();
      }

      private class InitHook implements InitConnection
      {
         @Override
         public void on(Connection rawConnection, long takenNanos) throws SQLException
         {
            created.incrementAndGet();
         }
      }

      private class GetHook implements GetConnection
      {
         @Override
         public void on(Connection rawConnection, long takenNanos) throws SQLException
         {
            active.incrementAndGet();
         }
      }

      private class CloseHook implements CloseConnection
      {
         @Override
         public void on(Connection rawConnection, long takenNanos) throws SQLException
         {
            active.decrementAndGet();
         }
      }
   }
}
