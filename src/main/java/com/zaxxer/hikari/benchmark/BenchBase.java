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
import java.sql.SQLException;


import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.vibur.dbcp.ViburDBCPDataSource;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.pool2.PooledObjectFactory;

@State(Scope.Benchmark)
public class BenchBase
{
    protected static final int MIN_POOL_SIZE = 0;

    @Param({ "hikari", "bone", "tomcat", "c3p0", "vibur", "dbcp" })
    public String pool;

    @Param({ "1", "2", "4", "8", "16", "32" })
    public int maxPoolSize;

    public static volatile DataSource DS;

//    private String jdbcUrl = "jdbc:stub";
//    private String dbDriver = "com.zaxxer.hikari.benchmark.stubs.StubDriver";
//
    //private String jdbcUrl= "jdbc:mariadb://localhost/employees";
    //private String dbDriver =  "org.mariadb.jdbc.Driver";


    private String jdbcUrl= "jdbc:mysql://node2:3306/employees?useConfigs=maxPerformance&useServerPrepStmts=true&useLocalTransactionState=true";
    private String dbDriver =  "com.mysql.jdbc.Driver";
    private String user = "connj";
    private String password = "test";

    @Setup
    public void setup() throws SQLException
    {
        try
        {
            Class.forName(dbDriver);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }

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
        case "c3p0-ht6":
            setupC3P0ht6();
            break;
        case "c3p0":
            setupC3P0();
            break;
        case "vibur":
            setupVibur();
            break;
        case "dbcp":
            setupDBCP();
            break;
        }
    }

    @TearDown
    public void teardown() throws SQLException
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
        case "c3p0-ht6":
            ((ComboPooledDataSource) DS).close();
            break;
        case "vibur":
            ((ViburDBCPDataSource) DS).terminate();
            break;
        case "dbcp":
            ((BasicDataSource) DS).close();
            break;
        }
    }

    protected void setupDBCP() throws SQLException
    {
	BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(dbDriver);
        ds.setUrl(jdbcUrl);
        ds.setUsername(user);
        ds.setPassword(password);
        ds.setInitialSize(MIN_POOL_SIZE);
        ds.setMinIdle(MIN_POOL_SIZE);
        ds.setMaxIdle(maxPoolSize);
        ds.setMaxTotal(maxPoolSize);
        ds.setMaxWaitMillis(8000);
        //props.setDefaultAutoCommit(false);
        ds.setRollbackOnReturn(false);
        ds.setEnableAutoCommitOnReturn(false);
        ds.setMinEvictableIdleTimeMillis((int) TimeUnit.MINUTES.toMillis(30));
        ds.setTestOnBorrow(false);
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        //props.setForceIgnoreUnresolvedTransactions(true);
        //props.setValidationQuery("VALUES 1");
        //props.setJdbcInterceptors("ConnectionState");

        DS = ds;
    }

    protected void setupTomcat()
    {
        PoolProperties props = new PoolProperties();
        props.setUrl(jdbcUrl);
        props.setDriverClassName(dbDriver);
        props.setUsername(user);
        props.setPassword(password);
        props.setInitialSize(MIN_POOL_SIZE);
        props.setMinIdle(MIN_POOL_SIZE);
        props.setMaxIdle(maxPoolSize);
        props.setMaxActive(maxPoolSize);
        props.setMaxWait(8000);
//        props.setDefaultAutoCommit(false);
//        props.setRollbackOnReturn(true);
        props.setMinEvictableIdleTimeMillis((int) TimeUnit.MINUTES.toMillis(30));
//        props.setTestOnBorrow(true);
        props.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
//        props.setValidationQuery("VALUES 1");
//        props.setJdbcInterceptors("ConnectionState");

        DS = new org.apache.tomcat.jdbc.pool.DataSource(props);
    }

    protected void setupBone()
    {
        BoneCPConfig config = new BoneCPConfig();
        config.setAcquireIncrement(1);
        config.setMinConnectionsPerPartition(MIN_POOL_SIZE);
        config.setMaxConnectionsPerPartition(maxPoolSize);
        config.setConnectionTimeoutInMs(8000);
        config.setIdleMaxAgeInMinutes(30);
        config.setConnectionTestStatement("VALUES 1");
        config.setCloseOpenStatements(true);
        config.setDisableConnectionTracking(true);
        config.setDefaultAutoCommit(false);
        config.setResetConnectionOnClose(true);
        config.setDefaultTransactionIsolation("READ_COMMITTED");
        config.setDisableJMX(true);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setPoolStrategy("CACHED");

        DS = new BoneCPDataSource(config);
    }

    protected void setupHikari()
    {
        HikariConfig config = new HikariConfig();
        config.setMinimumIdle(MIN_POOL_SIZE);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(8000);
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(30));
        config.setJdbc4ConnectionTest(true);
        config.setAutoCommit(false);
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        config.setDriverClassName( dbDriver );            
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        //config.setDataSourceClassName("com.zaxxer.hikari.benchmark.stubs.StubDataSource");

        DS = new HikariDataSource(config);
    }

    protected void setupC3P0ht6()
    {
        try
        {
            ComboPooledDataSource cpds = new ComboPooledDataSource();
            cpds.setDriverClass( dbDriver );            
            cpds.setJdbcUrl( jdbcUrl );
            cpds.setInitialPoolSize(MIN_POOL_SIZE);
            cpds.setMinPoolSize(MIN_POOL_SIZE);
            cpds.setMaxPoolSize(maxPoolSize);
            cpds.setCheckoutTimeout(8000);
            cpds.setLoginTimeout(8);
            cpds.setForceIgnoreUnresolvedTransactions(true);
            cpds.setNumHelperThreads(6);
            //cpds.setTestConnectionOnCheckout(true);
            //cpds.setPreferredTestQuery("VALUES 1");
            cpds.setUser(user);
            cpds.setPassword(password);
   
            DS = cpds;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void setupC3P0()
    {
        try
        {
            ComboPooledDataSource cpds = new ComboPooledDataSource();
            cpds.setDriverClass( dbDriver );            
            cpds.setJdbcUrl( jdbcUrl );
            cpds.setInitialPoolSize(MIN_POOL_SIZE);
            cpds.setMinPoolSize(MIN_POOL_SIZE);
            cpds.setMaxPoolSize(maxPoolSize);
            cpds.setCheckoutTimeout(8000);
            cpds.setLoginTimeout(8);
            cpds.setForceIgnoreUnresolvedTransactions(true);
            //cpds.setTestConnectionOnCheckout(true);
            //cpds.setPreferredTestQuery("VALUES 1");
            cpds.setUser(user);
            cpds.setPassword(password);
   
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
        vibur.setPoolInitialSize(MIN_POOL_SIZE);
        vibur.setPoolMaxSize(maxPoolSize);
        vibur.setTestConnectionQuery("VALUES 1");
        vibur.setDefaultAutoCommit(false);
        vibur.setResetDefaultsAfterUse(true);
        vibur.setConnectionIdleLimitInSeconds(1);
        vibur.setDefaultTransactionIsolation("READ_COMMITTED");
        vibur.start();

        DS = vibur;
    }
}
