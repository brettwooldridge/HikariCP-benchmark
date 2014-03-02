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
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class StatementBench extends BenchBase
{
    @GenerateMicroBenchmark
    public boolean cycleStatement(CycleState state) throws SQLException
    {
        Statement statement = state.connection.createStatement();
        boolean bool = statement.execute("INSERT INTO test (column) VALUES (?)");
        statement.close();
        return bool;
    }

    @GenerateMicroBenchmark
    public Statement prepareStatement(PrepareState state) throws SQLException
    {
        state.statement = state.connection.prepareStatement("INSERT INTO test (column) VALUES (?)");
        return state.statement;
    }

    @GenerateMicroBenchmark
    public Statement closeStatement(CloseState state) throws SQLException
    {
        Statement statement = state.statement;
        statement.close();
        return statement;
    }

    @GenerateMicroBenchmark
    public Statement[] abandonStatement(AbandonState state) throws SQLException
    {
        state.connection.close();
        return state.statement;
    }

    @State(Scope.Thread)
    public static class CycleState
    {
        Connection connection;

        @Setup(Level.Invocation)
        public void setup() throws SQLException
        {
            connection = DS.getConnection();
        }

        @TearDown(Level.Invocation)
        public void teardown() throws SQLException
        {
            connection.close();
        }
    }

    @State(Scope.Thread)
    public static class PrepareState
    {
        Connection connection;
        Statement statement;

        @Setup(Level.Invocation)
        public void setup() throws SQLException
        {
            connection = DS.getConnection();
        }

        @TearDown(Level.Invocation)
        public void teardown() throws SQLException
        {
            statement.close();
            connection.close();
        }
    }

    @State(Scope.Thread)
    public static class CloseState
    {
        Connection connection;
        Statement statement;

        @Setup(Level.Invocation)
        public void setup() throws SQLException
        {
            connection = DS.getConnection();
            statement = connection.prepareStatement("INSERT INTO test (column) VALUES (?)");
        }

        @TearDown(Level.Invocation)
        public void teardown() throws SQLException
        {
            connection.close();
        }
    }

    @State(Scope.Thread)
    public static class AbandonState
    {
        Connection connection;
        Statement[] statement = new Statement[5];

        @Setup(Level.Invocation)
        public void setup() throws SQLException
        {
            connection = DS.getConnection();
            statement[0] = connection.prepareStatement("INSERT INTO test (column) VALUES (?)");
            statement[1] = connection.prepareStatement("INSERT INTO test (column) VALUES (?)");
            statement[2] = connection.prepareStatement("INSERT INTO test (column) VALUES (?)");
            statement[3] = connection.prepareStatement("INSERT INTO test (column) VALUES (?)");
            statement[4] = connection.prepareStatement("INSERT INTO test (column) VALUES (?)");
        }
    }
}
