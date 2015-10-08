/*
 * Copyright (C) 2014 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zaxxer.hikari.benchmark;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.PooledConnection;
import org.apache.tomcat.jdbc.pool.Validator;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.vibur.dbcp.ViburDBCPDataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@State(Scope.Benchmark)
public class BenchBase
{
    protected static final int MIN_POOL_SIZE = 0;

    @Param({ "hikari", "dbcp", "dbcp2", "tomcat", "c3p0", "vibur" })
    public String pool;

    @Param({ "32" })
    public int maxPoolSize;

    @Param({ "jdbc:stub" })
    public String jdbcUrl;

    public static volatile DataSource DS;

    @Setup
    public void setup(BenchmarkParams params)
    {
        try
        {
            Class.forName("com.zaxxer.hikari.benchmark.stubs.StubDriver");
            System.err.printf("Using driver (%s): %s", jdbcUrl, DriverManager.getDriver(jdbcUrl));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        if (this.getClass().getName().contains("Statement")) {
            System.err.println("# Overriding maxPoolSize paramter for StatementBench: maxPoolSize=" + params.getThreads());
            maxPoolSize = params.getThreads();
        }

        switch (pool)
        {
        case "hikari":
            setupHikari();
            break;
        case "tomcat":
            setupTomcat();
            break;
        case "dbcp":
            setupDbcp();
            break;
        case "dbcp2":
            setupDbcp2();
            break;
        case "c3p0":
            setupC3P0();
            break;
        case "vibur":
            setupVibur();
            break;
        }
    }

    @TearDown
    public void teardown() throws SQLException
    {
        switch (pool)
        {
        case "hikari":
            ((HikariDataSource) DS).close();
            break;
        case "tomcat":
            ((org.apache.tomcat.jdbc.pool.DataSource) DS).close();
            break;
        case "dbcp":
            ((org.apache.commons.dbcp.BasicDataSource) DS).close();
            break;
        case "dbcp2":
            ((BasicDataSource) DS).close();
            break;
        case "c3p0":
            ((ComboPooledDataSource) DS).close();
            break;
        case "vibur":
            ((ViburDBCPDataSource) DS).terminate();
            break;
        }
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
        props.setMaxIdle(maxPoolSize);
        props.setMaxActive(maxPoolSize);
        props.setMaxWait(8000);

        props.setDefaultAutoCommit(false);

        props.setRollbackOnReturn(true);
        props.setUseDisposableConnectionFacade(true);
        props.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
        props.setTestOnBorrow(true);
        props.setValidationInterval(1000);
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

        DS = new org.apache.tomcat.jdbc.pool.DataSource(props);
    }

    protected void setupDbcp()
    {
        org.apache.commons.dbcp.BasicDataSource ds = new org.apache.commons.dbcp.BasicDataSource();
        ds.setUrl(jdbcUrl);
        ds.setUsername("brettw");
        ds.setPassword("");
        ds.setInitialSize(MIN_POOL_SIZE);
        ds.setMinIdle(MIN_POOL_SIZE);
        ds.setMaxIdle(maxPoolSize);
        ds.setMaxActive(maxPoolSize);

        ds.setDefaultAutoCommit(false);
        ds.setTestOnBorrow(true);
        ds.setValidationQuery("SELECT 1");

        DS = ds;
    }

    protected void setupDbcp2()
    {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(jdbcUrl);
        ds.setUsername("brettw");
        ds.setPassword("");
        ds.setInitialSize(MIN_POOL_SIZE);
        ds.setMinIdle(MIN_POOL_SIZE);
        ds.setMaxIdle(maxPoolSize);
        ds.setMaxTotal(maxPoolSize);
        ds.setMaxWaitMillis(8000);

        ds.setDefaultAutoCommit(false);
        ds.setRollbackOnReturn(true);
        ds.setEnableAutoCommitOnReturn(false);
        ds.setTestOnBorrow(true);
        ds.setCacheState(true);
        ds.setFastFailValidation(true);

        DS = ds;
    }

    protected void setupHikari()
    {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername("brettw");
        config.setPassword("");
        config.setMinimumIdle(MIN_POOL_SIZE);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(8000);
        config.setAutoCommit(false);

        DS = new HikariDataSource(config);
    }

    protected void setupC3P0()
    {
        try
        {
            ComboPooledDataSource cpds = new ComboPooledDataSource();
            cpds.setJdbcUrl( jdbcUrl );
            cpds.setUser("brettw");
            cpds.setPassword("");
            cpds.setAcquireIncrement(1);
            cpds.setInitialPoolSize(MIN_POOL_SIZE);
            cpds.setMinPoolSize(MIN_POOL_SIZE);
            cpds.setMaxPoolSize(maxPoolSize);
            cpds.setCheckoutTimeout(8000);
            cpds.setLoginTimeout(8);
            cpds.setTestConnectionOnCheckout(true);
            // cpds.setPreferredTestQuery("VALUES 1");
    
            DS = cpds;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void setupVibur()
    {
        ViburDBCPDataSource vibur = new ViburDBCPDataSource();
        vibur.setJdbcUrl( jdbcUrl );
        vibur.setUsername("brettw");
        vibur.setPassword("");
        vibur.setPoolFair(false);
        vibur.setPoolInitialSize(MIN_POOL_SIZE);
        vibur.setPoolMaxSize(maxPoolSize);
        vibur.setDefaultAutoCommit(false);
        vibur.setResetDefaultsAfterUse(true);
        vibur.setConnectionIdleLimitInSeconds(30);
        vibur.start();

        DS = vibur;
    }
}
