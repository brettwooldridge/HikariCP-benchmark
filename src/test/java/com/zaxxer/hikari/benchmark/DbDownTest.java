package com.zaxxer.hikari.benchmark;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
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
    private static final String JDBC_URL = "jdbc:mysql://172.16.207.207/test";

    private static final Logger LOGGER = LoggerFactory.getLogger(DbDownTest.class);

    private static final int MIN_POOL_SIZE = 5;
    private int maxPoolSize = MIN_POOL_SIZE;

    private DataSource hikariDS;
    private DataSource c3p0DS;
    private DataSource dbcp2DS;
    private DataSource viburDS;

    public static void main(String[] args)
    {
        DbDownTest dbDownTest = new DbDownTest();
        dbDownTest.start();
    }

    private DbDownTest()
    {
        hikariDS = setupHikari();
        viburDS = setupVibur(); 
        c3p0DS = setupC3P0();
        dbcp2DS = setupDbcp2();
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
                    LOGGER.info("{} got a connection.", ds.getClass().getSimpleName(), c);
                    try (Statement stmt = c.createStatement()) {
                        LOGGER.debug("{} Statement ({})", ds.getClass().getSimpleName(), System.identityHashCode(stmt));
                        stmt.setQueryTimeout(1);
                        resultSet = stmt.executeQuery("SELECT id FROM test");
                        if (resultSet.next()) {
                            LOGGER.debug("Ran query got {}", resultSet.getInt(1));
                        }
                        else {
                            LOGGER.warn("{} Query executed, got no results.", ds.getClass().getSimpleName());
                        }
                    }
                    catch (SQLException e) {
                        LOGGER.error("{} Exception executing query, got a bad connection from the pool: {}", ds.getClass().getSimpleName(), e.getMessage());
                    }
                }
                catch (Throwable t)
                {
                    LOGGER.error("{} Exception getting connection: {}", ds.getClass().getSimpleName(), t.getMessage());
                }
            }
        }

        new Timer(true).schedule(new MyTask(hikariDS), 5000, 2000);
        new Timer(true).schedule(new MyTask(viburDS), 5000, 2000);
        new Timer(true).schedule(new MyTask(c3p0DS), 5000, 2000);
        new Timer(true).schedule(new MyTask(dbcp2DS), 5000, 2000);

        try
        {
            Thread.sleep(TimeUnit.SECONDS.toMillis(300));
        }
        catch (InterruptedException e)
        {
            return;
        }
    }

    protected DataSource setupDbcp2()
    {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(JDBC_URL);
        ds.setUsername("root");
        ds.setPassword("");
        ds.setInitialSize(MIN_POOL_SIZE);
        ds.setMinIdle(MIN_POOL_SIZE);
        ds.setMaxIdle(maxPoolSize);
        ds.setMaxTotal(maxPoolSize);
        ds.setMaxWaitMillis(5000);

        ds.setDefaultAutoCommit(false);
        ds.setRollbackOnReturn(true);
        ds.setEnableAutoCommitOnReturn(false);
        ds.setTestOnBorrow(true);
        ds.setCacheState(true);
        ds.setFastFailValidation(true);

        return ds;
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
        // config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setUsername("root");
        config.setConnectionTimeout(5000);
        config.setMinimumIdle(MIN_POOL_SIZE);
        config.setMaximumPoolSize(maxPoolSize);
        config.setInitializationFailTimeout(0L);
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
        vibur.setConnectionTimeoutInMs(5000);
        vibur.setValidateTimeoutInSeconds(3);
        vibur.setLoginTimeoutInSeconds(2);
        vibur.setPoolInitialSize(MIN_POOL_SIZE);
        vibur.setPoolMaxSize(maxPoolSize);
        vibur.setConnectionIdleLimitInSeconds(1);
        vibur.setAcquireRetryAttempts(0);
        vibur.setReducerTimeIntervalInSeconds(0);
        vibur.setUseNetworkTimeout(true);
        vibur.setNetworkTimeoutExecutor(Executors.newCachedThreadPool());
        vibur.setClearSQLWarnings(true);
        vibur.setResetDefaultsAfterUse(true);
        vibur.setTestConnectionQuery("isValid"); // this is the default option, can be left commented out
        vibur.start();
        return vibur;
    }
}
