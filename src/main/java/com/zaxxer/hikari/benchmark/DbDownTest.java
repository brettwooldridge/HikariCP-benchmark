package com.zaxxer.hikari.benchmark;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    private static final String JDBC_URL = "jdbc:mysql://192.168.0.114/test";

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
            public ResultSet resultSet;

            MyTask(DataSource ds)
            {
                this.ds = ds;
            }

            @Override
            public void run()
            {
                try (Connection c = ds.getConnection()) {
                    LOGGER.info(ds.getClass().getSimpleName() + " got a connection.");
                    try (Statement stmt = c.createStatement()) {
                        LOGGER.debug(ds.getClass().getSimpleName() + " Statement " + System.identityHashCode(stmt));
                        stmt.setQueryTimeout(1);
                        resultSet = stmt.executeQuery("SELECT id FROM test");
                        if (resultSet.next()) {
                            LOGGER.debug("Ran query got " + resultSet.getInt(1));
                        }
                        else {
                            LOGGER.warn(ds.getClass().getSimpleName() + " Query executed, got no results.");
                        }
                    }
                    catch (SQLException e) {
                        LOGGER.error(ds.getClass().getSimpleName() + " Exception executing query, got a bad connection from the pool" + e.getMessage());
                    }
                }
                catch (Throwable t)
                {
                    LOGGER.error(ds.getClass().getSimpleName() + " Exception getting connection: " + t.getMessage());
                }
            }
        }

        new Timer(true).schedule(new MyTask(hikariDS), 5000, 2000);
        new Timer(true).schedule(new MyTask(c3p0DS), 5000, 2000);
        new Timer(true).schedule(new MyTask(viburDS), 5000, 2000);
        new Timer(true).schedule(new MyTask(boneDS), 5000, 2000);
        new Timer(true).schedule(new MyTask(tomcatDS), 5000, 2000);

        try
        {
            Thread.sleep(TimeUnit.SECONDS.toMillis(150));
        }
        catch (InterruptedException e)
        {
            return;
        }
    }

    protected DataSource setupTomcat()
    {
        PoolProperties props = new PoolProperties();
        props.setUrl(JDBC_URL);
        props.setDriverClassName("com.mysql.jdbc.Driver");
        props.setUsername("root");
        props.setPassword("");
        props.setMaxWait(5000);
        props.setValidationQueryTimeout(5);
        props.setTestOnBorrow(true);
        props.setInitialSize(MIN_POOL_SIZE);
        props.setMinIdle(MIN_POOL_SIZE);
        props.setMaxIdle(maxPoolSize);
        props.setMaxActive(maxPoolSize);
        props.setValidationQuery("SELECT 1");

        return new org.apache.tomcat.jdbc.pool.DataSource(props);
    }

    protected DataSource setupBone()
    {
        BoneCPConfig config = new BoneCPConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername("root");
        config.setPassword("");
        config.setConnectionTimeoutInMs(5000);
        config.setAcquireIncrement(1);
        config.setAcquireRetryAttempts(3);
        config.setAcquireRetryDelayInMs(5000);
        config.setMinConnectionsPerPartition(MIN_POOL_SIZE);
        config.setMaxConnectionsPerPartition(maxPoolSize);
        config.setConnectionTestStatement("SELECT 1");

        return new BoneCPDataSource(config);
    }

    protected DataSource setupHikari()
    {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setUsername("root");
        config.setConnectionTimeout(5000);
        config.setMinimumIdle(MIN_POOL_SIZE);
        config.setMaximumPoolSize(maxPoolSize);
        config.setInitializationFailFast(true);
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    protected DataSource setupC3P0()
    {
        try
        {
            ComboPooledDataSource cpds = new ComboPooledDataSource();
            cpds.setJdbcUrl( JDBC_URL );
            cpds.setUser("root");
            cpds.setCheckoutTimeout(5000);
            cpds.setTestConnectionOnCheckout(true);
            cpds.setAcquireIncrement(1);
            cpds.setAcquireRetryAttempts(3);
            cpds.setAcquireRetryDelay(5000);
            cpds.setBreakAfterAcquireFailure(true);
            cpds.setInitialPoolSize(MIN_POOL_SIZE);
            cpds.setMinPoolSize(MIN_POOL_SIZE);
            cpds.setMaxPoolSize(maxPoolSize);
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
        vibur.setJdbcUrl( JDBC_URL );
        vibur.setUsername("root");
        vibur.setPassword("");
        vibur.setConnectionIdleLimitInSeconds(0);
        vibur.setConnectionTimeoutInMs(5000);
        vibur.setLoginTimeoutInSeconds(5);
        vibur.setPoolEnableConnectionTracking(true);
        vibur.setResetDefaultsAfterUse(true);
        vibur.setPoolInitialSize(MIN_POOL_SIZE);
        vibur.setPoolMaxSize(maxPoolSize);
        // vibur.setTestConnectionQuery("SELECT 1");
        vibur.start();

        return vibur;
    }
}
