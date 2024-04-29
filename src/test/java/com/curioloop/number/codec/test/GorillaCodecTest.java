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
import com.curioloop.number.codec.gorilla.BitsReader;
import com.curioloop.number.codec.gorilla.BitsWriter;
import com.curioloop.number.codec.gorilla.GorillaCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
public class GorillaCodecTest {

    @Test void basicTest() {
        CodecBuffer buffer = CodecBuffer.newBuffer(100);
        float[] values = new float[]{15.5f, 14.0625f, 3.25f, -8.625f, 13.1f, 0, 25.3f};
        byte[] data = GorillaCodec.encode32(i -> values[i], values.length, buffer).toArray();
        System.out.println(data.length * 8); // encoded data size after (bits)
        System.out.println(Arrays.toString(Arrays.copyOf(data, data.length - 1)));

        List<Float> stream = new ArrayList<>();
        GorillaCodec.decode32(new CodecSlice().wrap(data), stream::add);
        System.out.println(stream); // decoded data size after (bits)
        Assertions.assertEquals(Arrays.toString(values), String.valueOf(stream));

        double[] values2 = new double[]{15.5f, 14.0625f, 3.25f, -8.625f, 13.1f, 0, 25.3f};
        byte[] data2 = GorillaCodec.encode64(i -> values2[i], values2.length, buffer.forgetPos()).toArray();
        System.out.println(data2.length * 8); // encoded data size after (bits)
        System.out.println(Arrays.toString(Arrays.copyOf(data2, data2.length - 1)));

        List<Double> stream2 = new ArrayList<>();
        GorillaCodec.decode64(new CodecSlice().wrap(data2), stream2::add);
        System.out.println(stream2); // decoded data size after (bits)
        Assertions.assertEquals(Arrays.toString(values2), String.valueOf(stream2));
    }

    @Test void bitsCodecTest() {
        CodecBuffer buf = CodecBuffer.newBuffer(12);
        BitsWriter out = new BitsWriter(buf);
        out.writeBits(123, 64);
        out.writeBits(120, 8);
        out.writeBits(110, 32);
        out.writeBits(115, 32);
        out.writeBit(true);
        out.writeBits(63, 6);
        out.writeBits(121, 7);
        out.writeBits(5, 31);
        out.writeBits(3, 3);
        out.writeBit(false);
        out.flushBits();

        byte[] bytes = buf.toArray();
        System.out.println(Arrays.toString(bytes));
        System.out.println("totalBits: " + out.totalBits());
        System.out.println("binaryStr: " + GorillaCodec.toBinaryString(bytes));

        byte[] offset = new byte[bytes.length + 3];
        System.arraycopy(bytes, 0, offset, 3, bytes.length);
        BitsReader in = new BitsReader(offset, 3, bytes.length);

        Assertions.assertEquals(123, in.readBits(64));
        Assertions.assertEquals(120, in.readBits(8));
        Assertions.assertEquals(110, in.readBits(32));
        Assertions.assertEquals(115, in.readBits(32));
        Assertions.assertEquals(1, in.readBits(1));
        Assertions.assertEquals(63, in.readBits(6));
        Assertions.assertEquals(121, in.readBits(7));
        Assertions.assertEquals(5, in.readBits(31));
        Assertions.assertEquals(3, in.readBits(3));
        Assertions.assertEquals(0, in.readBits(1));
        Assertions.assertFalse(in.hasMore());
    }

}