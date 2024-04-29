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

import com.curioloop.number.codec.CodecBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class CodecBufferPerf {

    @Benchmark
    @Warmup(time = 3, iterations = 3)
    @Measurement(time = 5, iterations = 3)
    public void encodeInt() {
        CodecBuffer buffer = CodecBuffer.newBuffer(401, false);
        buffer.putByte((byte) 0); // now we could assert it unaligned
        for (int i=0; i<100; i++) {
            buffer.putInt(i);
        }
    }

    @Benchmark
    @Warmup(time = 3, iterations = 3)
    @Measurement(time = 5, iterations = 3)
    public void encodeIntUnsafe()  {
        CodecBuffer buffer = CodecBuffer.newBuffer(401, true);
        buffer.putByte((byte) 0); // now we could assert it unaligned
        for (int i=0; i<100; i++) {
            buffer.putInt(i);
        }
    }

    @Benchmark
    @Warmup(time = 3, iterations = 3)
    @Measurement(time = 5, iterations = 3)
    public void encodeLong() {
        CodecBuffer buffer = CodecBuffer.newBuffer(801, false);
        buffer.putByte((byte) 0); // now we could assert it unaligned
        for (int i=0; i<100; i++) {
            buffer.putLong(i);
        }
    }

    @Benchmark
    @Warmup(time = 3, iterations = 3)
    @Measurement(time = 5, iterations = 3)
    public void encodeLongUnsafe()  {
        CodecBuffer buffer = CodecBuffer.newBuffer(801, true);
        buffer.putByte((byte) 0); // now we could assert it unaligned
        for (int i=0; i<100; i++) {
            buffer.putLong(i);
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CodecBufferPerf.class.getSimpleName())
                .timeUnit(TimeUnit.NANOSECONDS)
                .forks(1)
                .build();
        new Runner(opt).run();
    }

}
