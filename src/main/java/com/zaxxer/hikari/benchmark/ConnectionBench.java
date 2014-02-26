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
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ConnectionBench
{
    @Param({ "hikari", "bone", "tomcat" })
    public String pool;

    private DataSource DS;

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
        }

    }

    @GenerateMicroBenchmark
    public Connection testConnection()
    {
        try
        {
            Connection connection = DS.getConnection();
            connection.close();
            return connection;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void setupTomcat()
    {
        PoolProperties p = new PoolProperties();
        p.setUrl("jdbc:stub");
        p.setDriverClassName("com.zaxxer.hikari.benchmark.StubDriver");
        p.setUsername("sa");
        p.setPassword("");
        p.setInitialSize(10);
        p.setMinIdle(10);
        p.setMaxIdle(60);
        p.setMaxActive(60);
        p.setMaxWait(8000);
        p.setMinEvictableIdleTimeMillis((int) TimeUnit.MINUTES.toMillis(30));
        p.setTestOnBorrow(true);
        p.setValidationQuery("VALUES 1");
        // p.setJdbcInterceptors("StatementFinalizer");

        DS = new org.apache.tomcat.jdbc.pool.DataSource(p);
    }

    private void setupBone()
    {
        try
        {
            Class.forName("com.zaxxer.hikari.benchmark.StubDriver");
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }

        BoneCPConfig bconfig = new BoneCPConfig();
        bconfig.setAcquireIncrement(5);
        bconfig.setMinConnectionsPerPartition(10);
        bconfig.setMaxConnectionsPerPartition(60);
        bconfig.setConnectionTimeoutInMs(8000);
        bconfig.setIdleMaxAgeInMinutes(30);
        bconfig.setConnectionTestStatement("VALUES 1");
        bconfig.setCloseOpenStatements(true);
        bconfig.setDisableConnectionTracking(true);
        bconfig.setJdbcUrl("jdbc:stub");
        bconfig.setUsername("nobody");
        bconfig.setPassword("nopass");

        DS = new BoneCPDataSource(bconfig);
    }

    private void setupHikari()
    {
        HikariConfig config = new HikariConfig();
        config.setAcquireIncrement(5);
        config.setConnectionTimeout(8000);
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(30));
        config.setJdbc4ConnectionTest(true);
        config.setDataSourceClassName("com.zaxxer.hikari.benchmark.StubDataSource");
        config.setUseInstrumentation(true);

        DS = new HikariDataSource(config);
    }
}
