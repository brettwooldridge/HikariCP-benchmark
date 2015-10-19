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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

//@State(Scope.Benchmark)
//@Warmup(iterations=3, batchSize=1_000_000)
//@Measurement(iterations=8, batchSize=1_000_000)
//@BenchmarkMode(Mode.SingleShotTime)
//@OutputTimeUnit(TimeUnit.NANOSECONDS)

@Warmup(iterations=3)
@Measurement(iterations=8)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)

//@Warmup(iterations=3)
//@Measurement(iterations=8)
//@BenchmarkMode(Mode.SampleTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ConnectionBench extends BenchBase
{
    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public static Connection cycleCnnection() throws SQLException
    {
        Connection connection = DS.getConnection();
        connection.close();
        return connection;
    }
}
