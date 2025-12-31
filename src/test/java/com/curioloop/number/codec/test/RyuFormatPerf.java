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

import com.curioloop.number.codec.ryu.RyuFormatter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author curioloops@gmail.com
 * @since 2025/12/31
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RyuFormatPerf {

    static final int WARMUP = 2;
    static final int MEASURE = 3;

    static final float[] floats = new float[128];
    static final double[] doubles = new double[128];

    static {
        for (int i = 0; i< floats.length; i++) {
            floats[i] = ThreadLocalRandom.current().nextFloat();
        }
        for (int i = 0; i< doubles.length; i++) {
            doubles[i] = ThreadLocalRandom.current().nextDouble();
        }
    }

    @Benchmark
    @Warmup(iterations = WARMUP, time = 5)
    @Measurement(iterations = MEASURE, time = 5)
    public void testWithJdkToFloatString() {
        for (float sample : floats) {
            Float.toString(sample);
        }
    }

    @Benchmark
    @Warmup(iterations = WARMUP, time = 5)
    @Measurement(iterations = MEASURE, time = 5)
    public void testWithRyuToFloatString() {
        for (float sample : floats) {
            RyuFormatter.string(sample);
        }
    }

    @Benchmark
    @Warmup(iterations = WARMUP, time = 5)
    @Measurement(iterations = MEASURE, time = 5)
    public void testWithJdkToDoubleString() {
        for (double sample : doubles) {
            Double.toString(sample);
        }
    }

    @Benchmark
    @Warmup(iterations = WARMUP, time = 5)
    @Measurement(iterations = MEASURE, time = 5)
    public void testWithRyuToDoubleString() {
        for (double sample : doubles) {
            RyuFormatter.string(sample);
        }
    }

    @Benchmark
    @Warmup(iterations = WARMUP, time = 5)
    @Measurement(iterations = MEASURE, time = 5)
    public void testWithJdkToDecimal() {
        for (double sample : doubles) {
            BigDecimal.valueOf(sample);
        }
    }

    @Benchmark
    @Warmup(iterations = WARMUP, time = 5)
    @Measurement(iterations = MEASURE, time = 5)
    public void testWithRyuToDecimal() {
        for (double sample : doubles) {
            RyuFormatter.decimal(sample);
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(RyuFormatPerf.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(opt).run();
    }

}
