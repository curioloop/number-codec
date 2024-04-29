/*
 * Copyright © 2024 CurioLoop (curioloops@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.curioloop.number.codec.test;

import com.curioloop.number.codec.unsafe.GetInt;
import com.curioloop.number.codec.unsafe.PutInt;
import com.curioloop.number.codec.unsafe.Unsafe;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class UnsafeAccessPerf {

    static final PutInt PUT_INT = Unsafe.PUT_INT;
    static final GetInt GET_INT = Unsafe.GET_INT;

    static final int OFFSET = Unsafe.ARRAY_OFFSET.arrayBaseOffset(byte[].class);
    static final byte[] bytes = new byte[100];
    static int summary;

    @Benchmark
    @Warmup(time = 3, iterations = 3)
    @Measurement(time = 5, iterations = 3)
    public void putIntWithUnsafeBytes() {
        for (int i=0; i<25; i++) {
            PUT_INT.putInt(bytes, OFFSET + i * 4, Integer.reverseBytes(i));
        }
    }

    @Benchmark
    @Warmup(time = 3, iterations = 3)
    @Measurement(time = 5, iterations = 3)
    public void putIntNormal()  {
        byte[] local = bytes;
        for (int i=0; i<25; i++) {
            int pos = i * 4;
            local[pos] = (byte)(i >>> 24);
            local[pos+1] = (byte)(i >>> 16);
            local[pos+2] = (byte)(i >>> 8);
            local[pos+3] = (byte)i;
        }
    }

    @Benchmark
    @Warmup(time = 3, iterations = 3)
    @Measurement(time = 5, iterations = 3)
    public void getIntWithUnsafeBytes() {
        int sum = 0;
        for (int i=0; i<25; i++) {
            sum += Integer.reverseBytes(GET_INT.getInt(bytes, OFFSET + i * 4));
        }
        summary = sum;
    }

    @Benchmark
    @Warmup(time = 3, iterations = 3)
    @Measurement(time = 5, iterations = 3)
    public void getIntNormal()  {
        int sum = 0;
        byte[] local = bytes;
        for (int i=0; i<25; i++) {
            int pos = i * 4;
            int v = ((local[pos] & 0xFF) << 24) |
                    ((local[pos+1] & 0xFF) << 16) |
                    ((local[pos+2] & 0xFF) << 8) |
                    ((local[pos+3] & 0xFF));
            sum += v;
        }
        summary = sum;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(UnsafeAccessPerf.class.getSimpleName())
                .timeUnit(TimeUnit.NANOSECONDS)
                .forks(1)
                .build();
        new Runner(opt).run();
    }

}
