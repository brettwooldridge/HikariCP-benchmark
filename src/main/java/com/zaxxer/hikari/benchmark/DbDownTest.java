package com.zaxxer.hikari.benchmark;

import java.sql.Connection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vibur.dbcp.ViburDBCPDataSource;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DbDownTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DbDownTest.class);

    private static final int MIN_POOL_SIZE = 5;
    private int maxPoolSize = MIN_POOL_SIZE;

    private final DataSource hikariDS;
    private final DataSource boneDS;
    private final DataSource c3p0DS;
    private final DataSource tomcatDS;
    private final DataSource viburDS;

    public static void main(String[] args)
    {
        DbDownTest dbDownTest = new DbDownTest();
        dbDownTest.start();
    }

    private DbDownTest()
    {
        hikariDS = setupHikari();
        c3p0DS = setupC3P0();
        viburDS = setupVibur();
        boneDS = setupBone();
        tomcatDS = setupTomcat();
    }

    private void start()
    {
        class MyTask extends TimerTask
        {
            private DataSource ds;

            MyTask(DataSource ds)
            {
                this.ds = ds;
            }

            @Override
            public void run()
            {
                try (Connection c = ds.getConnection()) {
                    LOGGER.info(ds.getClass().getSimpleName() + " got a connection.");
                }
                catch (Exception e)
                {
                    LOGGER.warn("Exception getting connection", e);
                }
            }
        }

        new Timer(true).schedule(new MyTask(hikariDS), 1000, 2000);
        new Timer(true).schedule(new MyTask(c3p0DS), 1000, 2000);
        new Timer(true).schedule(new MyTask(viburDS), 1000, 2000);
        new Timer(true).schedule(new MyTask(boneDS), 1000, 2000);
        new Timer(true).schedule(new MyTask(tomcatDS), 1000, 2000);

        try
        {
            Thread.sleep(TimeUnit.SECONDS.toMillis(60));
        }
        catch (InterruptedException e)
        {
            return;
        }
    }

    protected DataSource setupTomcat()
    {
        PoolProperties props = new PoolProperties();
        props.setUrl("jdbc:mysql://192.168.0.114/test");
        props.setDriverClassName("com.mysql.jdbc.Driver");
        props.setUsername("root");
        props.setPassword("");
        props.setInitialSize(MIN_POOL_SIZE);
        props.setMinIdle(MIN_POOL_SIZE);
        props.setMaxIdle(maxPoolSize);
        props.setMaxActive(maxPoolSize);
        props.setMaxWait(5000);
        props.setDefaultAutoCommit(false);
        props.setRollbackOnReturn(true);
        props.setMinEvictableIdleTimeMillis((int) TimeUnit.MINUTES.toMillis(30));
        props.setTestOnBorrow(true);
        props.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        props.setValidationQuery("SELECT 1");
        props.setValidationQueryTimeout(5);
        props.setJdbcInterceptors("ConnectionState");

        return new org.apache.tomcat.jdbc.pool.DataSource(props);
    }

    protected DataSource setupBone()
    {
        BoneCPConfig config = new BoneCPConfig();
        config.setAcquireIncrement(1);
        config.setMinConnectionsPerPartition(MIN_POOL_SIZE);
        config.setMaxConnectionsPerPartition(maxPoolSize);
        config.setConnectionTimeoutInMs(5000);
        config.setIdleMaxAgeInMinutes(30);
        config.setConnectionTestStatement("SELECT 1");
        config.setCloseOpenStatements(true);
        config.setDisableConnectionTracking(true);
        config.setDefaultAutoCommit(false);
        config.setResetConnectionOnClose(true);
        config.setDefaultTransactionIsolation("READ_COMMITTED");
        config.setDisableJMX(true);
        config.setJdbcUrl("jdbc:mysql://192.168.0.114/test");
        config.setUsername("root");
        config.setPassword("");
        config.setPoolStrategy("CACHED");

        return new BoneCPDataSource(config);
    }

    protected DataSource setupHikari()
    {
        HikariConfig config = new HikariConfig();
        config.setInitializationFailFast(true);
        config.setMinimumIdle(MIN_POOL_SIZE);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(30));
        config.setJdbc4ConnectionTest(true);
        config.setAutoCommit(false);
        config.setUsername("root");
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        config.setJdbcUrl("jdbc:mysql://192.168.0.114/test");
        config.setDriverClassName("com.mysql.jdbc.Driver");

        return new HikariDataSource(config);
    }

    protected DataSource setupC3P0()
    {
        try
        {
            ComboPooledDataSource cpds = new ComboPooledDataSource();
            cpds.setJdbcUrl( "jdbc:mysql://192.168.0.114/test" );
            cpds.setUser("root");
            cpds.setAcquireIncrement(1);
            cpds.setInitialPoolSize(MIN_POOL_SIZE);
            cpds.setMinPoolSize(MIN_POOL_SIZE);
            cpds.setMaxPoolSize(maxPoolSize);
            cpds.setCheckoutTimeout(5000);
            cpds.setLoginTimeout(8);
            cpds.setTestConnectionOnCheckout(true);
            cpds.setPreferredTestQuery("SELECT 1");
    
            return cpds;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private DataSource setupVibur()
    {
        ViburDBCPDataSource vibur = new ViburDBCPDataSource();
        vibur.setJdbcUrl( "jdbc:mysql://192.168.0.114/test" );
        vibur.setUsername("root");
        vibur.setPassword("");
        vibur.setConnectionTimeoutInMs(5000);
        vibur.setPoolInitialSize(MIN_POOL_SIZE);
        vibur.setPoolMaxSize(maxPoolSize);
        vibur.setTestConnectionQuery("SELECT 1");
        vibur.setDefaultAutoCommit(false);
        vibur.setResetDefaultsAfterUse(true);
        vibur.setConnectionIdleLimitInSeconds(1);
        vibur.setDefaultTransactionIsolation("READ_COMMITTED");
        vibur.start();

        return vibur;
    }
}
