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
import com.curioloop.number.codec.CodecHelper;
import com.curioloop.number.codec.CodecResult;
import com.curioloop.number.codec.CodecSlice;
import com.curioloop.number.codec.stream.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.curioloop.number.codec.CodecResult.*;

public class CodecHelperTest {

    @Test void testDelta2() {
        CodecSlice s = new CodecSlice();
        CodecBuffer b = new CodecBuffer(64);

        for (int n : new int[]{1, 10, 100, 1000, 10000}) {
            CodecResult cr1 = CodecHelper.encodeDelta2(i -> i, n, b.forgetPos());
            Assertions.assertEquals(cr1.codecs(), CODEC_DELTA2 | CODEC_SIMPLE8);
            CodecHelper.decodeDelta2(s.wrap(cr1.data()), cr1.codecs(), Assertions::assertEquals);

            CodecResult cr2 = CodecHelper.encodeDelta2(i -> -n + i, n, b.forgetPos());
            Assertions.assertEquals(cr2.codecs(), CODEC_DELTA2 | CODEC_SIMPLE8);
            CodecHelper.decodeDelta2(s.wrap(cr2.data()), cr2.codecs(), (i, v) -> Assertions.assertEquals( -n + i, v));

            if (n == 1) continue;

            CodecResult cr3 = CodecHelper.encodeDelta2(i -> n - i, n, b.forgetPos());
            Assertions.assertEquals(cr3.codecs(), CODEC_DELTA2 | CODEC_ZIGZAG);
            CodecHelper.decodeDelta2(s.wrap(cr3.data()), cr3.codecs(), (i, v) -> Assertions.assertEquals( n - i, v));
        }
    }

    @Test void testInt() {

        CodecSlice slice = new CodecSlice();
        CodecBuffer buffer = new CodecBuffer(64);

        for (int n : new int[]{1, 10, 100, 1000, 10000}) {
            // Encode and decode with delta2
            CodecResult cr1 = CodecHelper.encodeDelta2(i -> i, n, buffer);
            CodecHelper.decodeDelta2(slice.wrap(cr1.data()), cr1.codecs(), Assertions::assertEquals);

            // Encode and decode integers
            CodecResult cr2 = CodecHelper.encodeInt(i -> i, n, true, buffer);
            CodecHelper.decodeInt(slice.wrap(cr2.data()), cr2.codecs(), Assertions::assertEquals);

            // Encode and decode longs
            CodecResult cr3 = CodecHelper.encodeLong(i -> i, n, true, buffer);
            CodecHelper.decodeLong(slice.wrap(cr3.data()), cr3.codecs(), Assertions::assertEquals);

            // Encode and decode floats
            CodecResult cr4 = CodecHelper.encodeFloat(i -> i, n, buffer);
            CodecHelper.decodeFloat(slice.wrap(cr4.data()), cr4.codecs(), Assertions::assertEquals);

            // Encode and decode doubles
            CodecResult cr5 = CodecHelper.encodeDouble(i -> i, n, buffer);
            CodecHelper.decodeDouble(slice.wrap(cr5.data()), cr5.codecs(), Assertions::assertEquals);
        }

    }

    @Test void testLong() {
        CodecSlice s = new CodecSlice();
        CodecBuffer b = new CodecBuffer(128);

        for (int n : new int[]{1, 10, 100, 1000, 10000}) {

            int simple8Flag = n == 1 ? 0 : CODEC_SIMPLE8;

            CodecResult cr1 = CodecHelper.encodeLong(i -> i, n, true, b);
            Assertions.assertEquals(cr1.codecs(), simple8Flag | CODEC_VAR_INT);
            CodecHelper.decodeLong(s.wrap(cr1.data()), cr1.codecs(), Assertions::assertEquals);

            CodecResult cr2 = CodecHelper.encodeLong(i -> n - i, n, false, b);
            Assertions.assertEquals(cr2.codecs(), simple8Flag | CODEC_ZIGZAG);
            CodecHelper.decodeLong(s.wrap(cr2.data()), cr2.codecs(), (i, v) -> Assertions.assertEquals( n - i, v));

        }

    }

    @Test void testFloat() {
        CodecSlice s = new CodecSlice();
        CodecBuffer b = new CodecBuffer(128);

        CodecResult cr0 = CodecHelper.encodeFloat(i -> i, 1, b);
        Assertions.assertEquals(cr0.codecs(), CODEC_RAW);
        CodecHelper.decodeFloat(s.wrap(cr0.data()), cr0.codecs(), Assertions::assertEquals);

        CodecResult cr1 = CodecHelper.encodeFloat(i -> 5000 - i, 10000, b);
        Assertions.assertEquals(cr1.codecs(), CODEC_GORILLA);
        CodecHelper.decodeFloat(s.wrap(cr1.data()), cr1.codecs(), (i, v) -> Assertions.assertEquals(5000 - i, v));

    }

    @Test void testDouble() {
        CodecSlice s = new CodecSlice();
        CodecBuffer b = new CodecBuffer(128);

        CodecResult cr0 = CodecHelper.encodeDouble(i -> i, 1, b);
        Assertions.assertEquals(cr0.codecs(), CODEC_RAW);
        CodecHelper.decodeDouble(s.wrap(cr0.data()), cr0.codecs(), Assertions::assertEquals);

        CodecResult cr1 = CodecHelper.encodeDouble(i -> 5000 - i, 10000, b);
        Assertions.assertEquals(cr1.codecs(), CODEC_GORILLA);
        CodecHelper.decodeDouble(s.wrap(cr1.data()), cr1.codecs(), (i, v) -> Assertions.assertEquals(5000 - i, v));

        double[] random = {470.3954297588686, 0.767862549332054, 2134.4581560362985, 0.7615757469862584, 7489629.29818493, 2085746393651.9005, 0.9139528881461843, 595871533556.732, 0.1469336564098932, 57.54422472855859};
        CodecResult cr2 = CodecHelper.encodeDouble(i -> random[i], random.length, b);
        Assertions.assertEquals(cr2.codecs(), CODEC_RAW);
        CodecHelper.decodeDouble(s.wrap(cr2.data()), cr2.codecs(), (i, v) -> Assertions.assertEquals(v, random[i]));

        double[] period = {470.3954297588686, 0.767862549332054, 2134.4581560362985, 470.3954297588686, 0.767862549332054, 2134.4581560362985, 470.3954297588686, 0.767862549332054, 2134.4581560362985, 470.3954297588686, 0.767862549332054, 2134.4581560362985};
        CodecResult cr3 = CodecHelper.encodeDouble(i -> period[i], period.length, b);
        Assertions.assertEquals(cr3.codecs(), CODEC_CHIMP);
        CodecHelper.decodeDouble(s.wrap(cr3.data()), cr3.codecs(), (i, v) -> Assertions.assertEquals(v, period[i]));
    }

    @Test void testRaw() {
        CodecBuffer buf = CodecBuffer.newBuffer(10000);

        CodecHelper.encodeRaw((IntGetter) i -> i, 10000, buf.forgetPos());
        CodecHelper.decodeRaw(buf.toArray(), 0, buf.position(), (IntSetter) Assertions::assertEquals);

        CodecHelper.encodeRaw((LongGetter) i -> i, 10000, buf.forgetPos());
        CodecHelper.decodeRaw(buf.toArray(), 0, buf.position(), (LongSetter) Assertions::assertEquals);

        CodecHelper.encodeRaw((FloatGetter) i -> i, 10000, buf.forgetPos());
        CodecHelper.decodeRaw(buf.toArray(), 0, buf.position(), (FloatSetter) Assertions::assertEquals);

        CodecHelper.encodeRaw((DoubleGetter) i -> i, 10000, buf.forgetPos());
        CodecHelper.decodeRaw(buf.toArray(), 0, buf.position(), (DoubleSetter) Assertions::assertEquals);

    }

}
