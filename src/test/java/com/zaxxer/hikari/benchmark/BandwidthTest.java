package com.zaxxer.hikari.benchmark;

import static java.lang.System.out;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.DbcpPoolAccessor;
import org.apache.commons.dbcp2.TomcatPoolAccessor;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.PooledConnection;
import org.apache.tomcat.jdbc.pool.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vibur.dbcp.ViburDBCPDataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.ClockSource;

public class BandwidthTest
{
   private static final Logger LOGGER = LoggerFactory.getLogger(SpikeLoadTest.class);

   private static final String jdbcUrl = "jdbc:postgresql://localhost/test";

   private static final int MIN_POOL_SIZE = 1;
   private static final int MAX_POOL_SIZE = 1;

   private DataSource DS;
   private String pool;

   public static void main(String[] args) throws InterruptedException, SQLException
   {
      BandwidthTest test = new BandwidthTest();

      test.setup(args);

      out.println("Blackhole value " + test.start());
   }

   private void setup(String[] args) throws SQLException
   {
      pool = args[0];
      switch (pool) {
      case "hikari":
         setupHikari();
         break;
      case "dbcp2":
         setupDbcp2();
         break;
      case "c3p0":
         setupC3P0();
         break;
      case "tomcat":
         setupTomcat();
         break;
      case "vibur":
         setupVibur();
         break;
      default:
         throw new IllegalArgumentException("Unknown connection pool specified");
      }

      try (Connection connection = DS.getConnection()) {
         out.printf("Primed connection: %s.", pool);
         connection.setAutoCommit(true);
         Statement stmt = connection.createStatement();
         stmt.execute("DROP TABLE IF EXISTS test; CREATE TABLE test (value VARCHAR(255));");
         stmt.executeUpdate("INSERT INTO test (value) VALUES ('this is a test')");
         connection.setAutoCommit(false);
      }
   }

   private int start() throws InterruptedException, SQLException
   {
      int blackhole = 0;
      try (Scanner scanner = new Scanner(System.in)) {

         out.println("\n[Re]start iptop capture and press enter: sudo iftop -i lo0 -nf \"port 5432 and host localhost\"");
         scanner.nextLine();

         long start = ClockSource.currentTime();
         for (int i = 0; i < 10000; i++) {
            try (Connection connection = DS.getConnection()) {
               connection.setReadOnly(true);
               if (!connection.isReadOnly()) {
                  LOGGER.warn("Read-only was not reset to 'true'");
               }
               connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
               Statement stmt = connection.createStatement();
               ResultSet rs = stmt.executeQuery("SELECT * FROM test");
               if (rs.next()) {
                  blackhole += rs.getString(1).hashCode();
               }
            }
         }

         out.println("Elapsed runtime " + ClockSource.elapsedDisplayString(start, ClockSource.currentTime()));
         out.flush();

         try (Connection connection = DS.getConnection()) {
            if (connection.isReadOnly()) {
               LOGGER.warn("Read-only was not reset to 'false'");
            }

            if (connection.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED) {
               LOGGER.warn("Transaction isolation level was not reset to TRANSACTION_READ_COMMITTED");
            }
         }
      }
      finally {
         shutdownPool(DS);
      }

      return blackhole;
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
      ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
      ds.setDefaultReadOnly(false);

      ds.setRollbackOnReturn(true);
      ds.setEnableAutoCommitOnReturn(false);
      ds.setTestOnBorrow(true);
      ds.setCacheState(true);
      ds.setFastFailValidation(true);

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
      config.setReadOnly(false);
      config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

      DS = new HikariDataSource(config);
   }

   protected void setupTomcat()
   {
       PoolProperties props = new PoolProperties();
       props.setUrl(jdbcUrl);
       props.setUsername("brettw");
       props.setPassword("");
       props.setInitialSize(MIN_POOL_SIZE);
       props.setMinIdle(MIN_POOL_SIZE);
       props.setMaxIdle(MAX_POOL_SIZE);
       props.setMaxActive(MAX_POOL_SIZE);
       props.setMaxWait(8000);

       props.setDefaultAutoCommit(false);
       props.setDefaultReadOnly(false);
       props.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

       props.setRollbackOnReturn(true);
       props.setUseDisposableConnectionFacade(true);
       props.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
       props.setTestOnBorrow(true);
       props.setLogAbandoned(true);
       props.setSuspectTimeout(120);
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
      vibur.setConnectionIdleLimitInSeconds(1);
      vibur.setUseNetworkTimeout(true);
      vibur.setNetworkTimeoutExecutor(Executors.newFixedThreadPool(1));
      vibur.setClearSQLWarnings(true);
      vibur.setResetDefaultsAfterUse(true);

      vibur.setDefaultAutoCommit(false);
      vibur.setDefaultReadOnly(false);
      vibur.setDefaultTransactionIsolationValue(Connection.TRANSACTION_READ_COMMITTED);

      vibur.setReducerTimeIntervalInSeconds((int) MINUTES.toSeconds(10));

      vibur.start();

      DS = vibur;
   }
}
