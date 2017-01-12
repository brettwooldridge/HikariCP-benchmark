package com.zaxxer.hikari.benchmark;

import static com.zaxxer.hikari.util.UtilityElf.quietlySleep;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.String.valueOf;
import static java.lang.System.nanoTime;
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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.DbcpPoolAccessor;
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

public class SpikeLoadTest
{
   private static final Logger LOGGER = LoggerFactory.getLogger(SpikeLoadTest.class);

   public static final String jdbcUrl = "jdbc:stub";

   private static final int MIN_POOL_SIZE = 5;
   private static final int MAX_POOL_SIZE = 50;

   private DataSource DS;

   private int requestCount;

   private CyclicBarrier cyclicBarrier;

   private String pool;

   private HikariPool hikariPool;

   private DbcpPoolAccessor dbcpPool;

   private ViburPoolHooks viburPool;

   public static void main(String[] args)
   {
      SpikeLoadTest test = new SpikeLoadTest();

      test.setup(args);

      test.start();
   }

   private void setup(String[] args)
   {
      try {
         Class.forName("com.zaxxer.hikari.benchmark.stubs.StubDriver");
         StubDriver driver = (StubDriver) DriverManager.getDriver(jdbcUrl);
         System.err.printf("Using driver (%s): %s", jdbcUrl, driver);

         StubDriver.setConnectDelayMs(parseLong(args[0]));
         StubStatement.setExecuteDelayMs(0L);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }

      pool = args[1];
      switch (pool) {
      case "hikari":
         setupHikari();
         hikariPool = getHikariPool(DS);
         break;
      case "dbcp2":
         setupDbcp2();
         dbcpPool = (DbcpPoolAccessor) DS;
         break;
      case "c3p0":
         setupC3P0();
         break;
      case "vibur":
         setupVibur();
         break;
      default:
         throw new IllegalArgumentException("Unknown connection pool specified");
      }

      this.requestCount = parseInt(args[2]);
   }

   private void start()
   {
      List<RequestThread> list = new ArrayList<>();
      for (int i = 0; i < requestCount; i++) {
         RequestThread rt = new RequestThread();
         list.add(rt);
      }

      quietlySleep(SECONDS.toMillis(5));

      cyclicBarrier = new CyclicBarrier(requestCount + 1);

      ThreadGroup tg = new ThreadGroup("SpikeLoadTest");
      for (int i = 0; i < requestCount; i++) {
         Thread t = new Thread(tg, list.get(i), valueOf(i));
         t.start();
      }

      List<PoolStatistics> statsList = new ArrayList<>();
      try {
         LOGGER.info("SpikeLoadTest starting.");
         cyclicBarrier.await();

         final long startTime = nanoTime();

         do {
            statsList.add(getPoolStatistics(startTime));

            final long spinStart = nanoTime();
            do {
               // spin
            } while (nanoTime() - spinStart < 100_000 /* 0.1ms */);
         }
         while (tg.activeCount() > 0);

         long endTime = nanoTime();

         LOGGER.info("SpikeLoadTest completed in {}ms", MILLISECONDS.convert(endTime - startTime, NANOSECONDS));

         dumpStats(statsList, list);
      }
      catch (InterruptedException | BrokenBarrierException e) {
         throw new RuntimeException(e);
      }
   }

   private void dumpStats(List<PoolStatistics> statsList, List<RequestThread> list)
   {
      System.out.println(String.join("\t", "Time", "Total", "Active", "Idle", "Wait"));
      for (PoolStatistics stats : statsList) {
         System.out.println(stats);
      }

      for (RequestThread req : list) {
         System.out.println(req);
      }
      
   }

   private class RequestThread implements Runnable
   {
      String name;
      long startTime;
      long endTime;
      @SuppressWarnings("unused")
      Exception exception;

      @Override
      public void run()
      {
         name = currentThread().getName();
         try {
            cyclicBarrier.await();
         }
         catch (InterruptedException | BrokenBarrierException e1) {
            exception = e1;
            return;
         }

         startTime = nanoTime();
         try (Connection connection = DS.getConnection();
               Statement statement = connection.createStatement();
               ResultSet resultSet = statement.executeQuery("SELECT x FROM faux")) {
         }
         catch (SQLException e) {
            exception = e;
         }
         finally {
            endTime = nanoTime();
         }
      }

      @Override
      public String toString()
      {
         return String.format("%d\t%s", NANOSECONDS.toMicros(endTime - startTime), name);
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
         return String.format("%d\t%d\t%d\t%d\t%d", NANOSECONDS.toMicros(timestamp), totalConnections, activeConnections, idleConnections, pendingThreads);
      }
   }

   private PoolStatistics getPoolStatistics(final long baseTime)
   {
      PoolStatistics stats = new PoolStatistics(baseTime);

      switch (pool) {
      case "hikari":
         stats.activeConnections = hikariPool.getActiveConnections();
         stats.idleConnections = hikariPool.getIdleConnections();
         stats.pendingThreads = hikariPool.getThreadsAwaitingConnection();
         stats.totalConnections = hikariPool.getTotalConnections();
         break;
      case "dbcp2":
         stats.activeConnections = dbcpPool.getNumActive();
         stats.idleConnections = dbcpPool.getNumIdle();
         stats.pendingThreads = dbcpPool.getNumWaiters();
         stats.totalConnections = dbcpPool.getNumTotal();
         break;
      case "c3p0":
         setupC3P0();
         break;
      case "vibur":
         stats.activeConnections = viburPool.getActive();
         stats.idleConnections = viburPool.getIdle();
         stats.pendingThreads = 0;
         stats.totalConnections = viburPool.getTotal();
         break;
      }

      return stats;
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

      DS = ds;

      // forces internal pool creation
      try {
         ds.getLogWriter();
      }
      catch (SQLException e) {
         throw new RuntimeException(e);
      }
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

   private void setupC3P0()
   {
      try {
         ComboPooledDataSource cpds = new ComboPooledDataSource();
         cpds.setJdbcUrl(jdbcUrl);
         cpds.setUser("brettw");
         cpds.setPassword("");
         cpds.setAcquireIncrement(1);
         cpds.setInitialPoolSize(MIN_POOL_SIZE);
         cpds.setMinPoolSize(MIN_POOL_SIZE);
         cpds.setMaxPoolSize(MAX_POOL_SIZE);
         cpds.setCheckoutTimeout(8000);
         cpds.setLoginTimeout(8);
         cpds.setTestConnectionOnCheckout(true);
         // cpds.setPreferredTestQuery("VALUES 1");

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
