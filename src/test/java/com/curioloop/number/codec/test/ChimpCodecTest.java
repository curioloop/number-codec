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
import com.curioloop.number.codec.CodecSlice;
import com.curioloop.number.codec.chimp.Chimp;
import com.curioloop.number.codec.chimp.ChimpCodec;
import com.curioloop.number.codec.chimp.ChimpN;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.curioloop.number.codec.test.TestCompressRate.*;
import static com.curioloop.number.codec.test.TestDataSample.*;

/**
 * @author curioloops@gmail.com
 * @since 2024/5/2
 */
public class ChimpCodecTest {

    @Test void basicTest() {
        CodecBuffer buffer = CodecBuffer.newBuffer(100);
        float[] values = new float[]{3.42f, 0.0f, 703.71f, 0.0f, 183.6f, 178.0f, 0.0f,
                                    15.5f, 14.0625f, 3.25f, -8.625f, 13.1f, 0, 25.3f};
        byte[] data = Chimp.encode32(i -> values[i], values.length, buffer).toArray();
        System.out.println(data.length * 8); // encoded data size after (bits)
        System.out.println(Arrays.toString(Arrays.copyOf(data, data.length - 1)));

        List<Float> stream = new ArrayList<>();
        Chimp.decode32(new CodecSlice().wrap(data), stream::add);
        System.out.println(stream); // decoded data size after (bits)
        Assertions.assertEquals(Arrays.toString(values), String.valueOf(stream));

        double[] values2 = new double[]{3.42, 0.0, 703.71, 0.0, 183.6, 178.0, 0.0,
                                        15.5f, 14.0625f, 3.25f, -8.625f, 13.1f, 0, 25.3f};
        byte[] data2 = Chimp.encode64(i -> values2[i], values2.length, buffer.forgetPos()).toArray();
        System.out.println(data2.length * 8); // encoded data size after (bits)
        System.out.println(Arrays.toString(Arrays.copyOf(data2, data2.length - 1)));

        List<Double> stream2 = new ArrayList<>();
        Chimp.decode64(new CodecSlice().wrap(data2), stream2::add);
        System.out.println(stream2); // decoded data size after (bits)
        Assertions.assertEquals(Arrays.toString(values2), String.valueOf(stream2));
    }


    @Test void chimpNTest() {
        CodecBuffer buffer = CodecBuffer.newBuffer(100);

        for (int N : new int[]{4, 8, 16, 32, 64, 128, 256}) {

            float[] values = new float[]{3.42f, 0.0f, 703.71f, 0.0f, 183.6f, 178.0f, 0.0f,
                    15.5f, 14.0625f, 3.25f, -8.625f, 13.1f, 0, 25.3f};
            byte[] data = ChimpN.encode32(i -> values[i], values.length, buffer.forgetPos(), N).toArray();
            System.out.println(Arrays.toString(Arrays.copyOf(data, data.length - 1)));

            List<Float> stream = new ArrayList<>();
            ChimpN.decode32(new CodecSlice().wrap(data), stream::add, N);
            Assertions.assertEquals(Arrays.toString(values), String.valueOf(stream));

            double[] values2 = new double[]{3.42, 0.0, 703.71, 0.0, 183.6, 178.0, 0.0,
                    15.5f, 14.0625f, 3.25f, -8.625f, 13.1f, 0, 25.3f};
            byte[] data2 = ChimpN.encode64(i -> values2[i], values2.length, buffer.forgetPos(), N).toArray();
            System.out.println(Arrays.toString(Arrays.copyOf(data2, data2.length - 1)));

            List<Double> stream2 = new ArrayList<>();
            ChimpN.decode64(new CodecSlice().wrap(data2), stream2::add, N);
            Assertions.assertEquals(Arrays.toString(values2), String.valueOf(stream2));
        }

    }

    @Test void testKline() {
        for (String file : KLINE_SAMPLES) {
            Map<String, List<TestDataSample.KLine>> sample = loadKline(file);
            for (Map.Entry<String, List<TestDataSample.KLine>> entry : sample.entrySet()) {
                List<TestDataSample.KLine> data = entry.getValue();

                List<Float> floats = data.stream().map(TestDataSample.KLine::getClose).collect(Collectors.toList());
                List<Double> doubles = data.stream().map(TestDataSample.KLine::getAmount).collect(Collectors.toList());

                CodecBuffer buffer = CodecBuffer.newBuffer(1000);
                CodecSlice slice = new CodecSlice();

                for (int N : new int[]{0, 4, 8, 16, 32, 64, 128, 256}) {

                    byte[] floatBits = ChimpCodec.encode32(floats::get, floats.size(), buffer.forgetPos(), N).toArray();
                    byte[] doubleBits = ChimpCodec.encode64(doubles::get, doubles.size(), buffer.forgetPos(), N).toArray();

                    System.out.printf("%s(%d)\t:\t%d -> %d (%.2f) \t %d -> %d (%.2f)\n", entry.getKey(), N,
                            doubles.size() * 8, doubleBits.length, (double) doubleBits.length / (doubles.size() * 8),
                            floats.size() * 4, floatBits.length, (double) floatBits.length / (floats.size() * 4));

                    List<Float> recover32 = new ArrayList<>();
                    ChimpCodec.decode32(slice.wrap(floatBits), (i, v) -> recover32.add(v));
                    Assertions.assertEquals(floats, recover32, entry.getKey());

                    List<Double> recover64 = new ArrayList<>();
                    ChimpCodec.decode64(slice.wrap(doubleBits), (i, v) -> recover64.add(v));
                    Assertions.assertEquals(doubles, recover64, entry.getKey());
                }
            }
        }
    }

    @Test void testTrade() {
        for (String file : TRADE_SAMPLES) {
            Map<String, List<Trade>> sample = loadTrade(file);
            for (Map.Entry<String, List<Trade>> entry : sample.entrySet()) {
                List<Trade> data = entry.getValue();
                List<Float> floats = data.stream().map(TestDataSample.Trade::getPrice).collect(Collectors.toList());
                List<Double> doubles = data.stream().mapToDouble(TestDataSample.Trade::getPrice).boxed().collect(Collectors.toList());

                CodecBuffer buffer = CodecBuffer.newBuffer(1000);
                CodecSlice slice = new CodecSlice();

                for (int N : new int[]{0, 4, 8, 16, 32, 64, 128, 256}) {

                    byte[] floatBits = ChimpCodec.encode32(floats::get, floats.size(), buffer.forgetPos(), N).toArray();
                    byte[] doubleBits = ChimpCodec.encode64(doubles::get, doubles.size(), buffer.forgetPos(), N).toArray();

                    System.out.printf("%s(%d)\t:\t%d -> %d (%.2f) \t %d -> %d (%.2f)\n", entry.getKey(), N,
                            doubles.size() * 8, doubleBits.length, (double) doubleBits.length / (doubles.size() * 8),
                            floats.size() * 4, floatBits.length, (double) floatBits.length / (floats.size() * 4));

                    List<Float> recover32 = new ArrayList<>();
                    ChimpCodec.decode32(slice.wrap(floatBits), (i, v) -> recover32.add(v));
                    Assertions.assertEquals(floats, recover32, entry.getKey());

                    List<Double> recover64 = new ArrayList<>();
                    ChimpCodec.decode64(slice.wrap(doubleBits), (i, v) -> recover64.add(v));
                    Assertions.assertEquals(doubles, recover64, entry.getKey());
                }
            }
        }
    }


}