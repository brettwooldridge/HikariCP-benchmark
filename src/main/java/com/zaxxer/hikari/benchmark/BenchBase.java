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
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@State(Scope.Benchmark)
public class BenchBase
{
    @Param({ "hikari", "bone", "tomcat", "c3p0" })
    public String pool;

    public static volatile DataSource DS;

    @Setup
    public void setup()
    {
        switch (pool)
        {
        case "hikari":
            setupHikari();
            break;
        case "bone":
            setupBone();
            break;
        case "tomcat":
            setupTomcat();
            break;
        case "c3p0":
            setupC3P0();
            break;
        }
    }

    @TearDown
    public void teardown()
    {
        switch (pool)
        {
        case "hikari":
            ((HikariDataSource) DS).shutdown();
            break;
        case "bone":
            ((BoneCPDataSource) DS).close();
            break;
        case "tomcat":
            ((org.apache.tomcat.jdbc.pool.DataSource) DS).close();
            break;
        case "c3p0":
            ((ComboPooledDataSource) DS).close();
            break;
        }
    }

    protected void setupTomcat()
    {
        PoolProperties props = new PoolProperties();
        props.setUrl("jdbc:stub");
        props.setDriverClassName("com.zaxxer.hikari.benchmark.stubs.StubDriver");
        props.setUsername("sa");
        props.setPassword("");
        props.setInitialSize(10);
        props.setMinIdle(10);
        props.setMaxIdle(60);
        props.setMaxActive(60);
        props.setMaxWait(8000);
        props.setDefaultAutoCommit(false);
        props.setRollbackOnReturn(true);
        props.setMinEvictableIdleTimeMillis((int) TimeUnit.MINUTES.toMillis(30));
        props.setTestOnBorrow(true);
        props.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        props.setValidationQuery("VALUES 1");
        props.setJdbcInterceptors("ConnectionState;StatementFinalizer;");

        DS = new org.apache.tomcat.jdbc.pool.DataSource(props);
    }

    protected void setupBone()
    {
        try
        {
            Class.forName("com.zaxxer.hikari.benchmark.stubs.StubDriver");
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }

        BoneCPConfig config = new BoneCPConfig();
        config.setAcquireIncrement(5);
        config.setMinConnectionsPerPartition(10);
        config.setMaxConnectionsPerPartition(60);
        config.setConnectionTimeoutInMs(8000);
        config.setIdleMaxAgeInMinutes(30);
        config.setConnectionTestStatement("VALUES 1");
        config.setCloseOpenStatements(true);
        config.setDisableConnectionTracking(true);
        config.setDefaultAutoCommit(false);
        config.setResetConnectionOnClose(true);
        config.setDefaultTransactionIsolation("TRANSACTION_READ_COMMITTED");
        config.setDisableJMX(true);
        config.setJdbcUrl("jdbc:stub");
        config.setUsername("nobody");
        config.setPassword("nopass");

        DS = new BoneCPDataSource(config);
    }

    protected void setupHikari()
    {
        HikariConfig config = new HikariConfig();
        config.setAcquireIncrement(5);
        config.setConnectionTimeout(8000);
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(30));
        config.setJdbc4ConnectionTest(true);
        config.setAutoCommit(false);
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        config.setDataSourceClassName("com.zaxxer.hikari.benchmark.stubs.StubDataSource");

        DS = new HikariDataSource(config);
    }

    protected void setupC3P0()
    {
        try
        {
            ComboPooledDataSource cpds = new ComboPooledDataSource();
            cpds.setDriverClass( "com.zaxxer.hikari.benchmark.stubs.StubDriver" );            
            cpds.setJdbcUrl( "jdbc:stub" );
            cpds.setInitialPoolSize(10);
            cpds.setMinPoolSize(10);
            cpds.setMaxPoolSize(60);
            cpds.setCheckoutTimeout(8000);
            cpds.setLoginTimeout(8);
            cpds.setTestConnectionOnCheckout(true);
            cpds.setPreferredTestQuery("VALUES 1");
    
            DS = cpds;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
