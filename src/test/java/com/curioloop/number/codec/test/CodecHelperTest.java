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

        CodecResult cr1 = CodecHelper.encodeDelta2(i -> i, 10000);
        Assertions.assertEquals(cr1.codecs(), CODEC_DELTA2 | CODEC_SIMPLE8);
        CodecHelper.decodeDelta2(s.wrap(cr1.data()), cr1.codecs(), Assertions::assertEquals);

        CodecResult cr2 = CodecHelper.encodeDelta2(i -> 10000 - i, 10000);
        Assertions.assertEquals(cr2.codecs(), CODEC_DELTA2 | CODEC_ZIGZAG);
        CodecHelper.decodeDelta2(s.wrap(cr2.data()), cr2.codecs(), (i, v) -> Assertions.assertEquals( 10000 - i, v));
    }

    @Test void testInt() {

        CodecSlice slice = new CodecSlice();

        // Encode and decode with delta2
        CodecResult cr1 = CodecHelper.encodeDelta2(i -> i, 10000);
        CodecHelper.decodeDelta2(slice.wrap(cr1.data()), cr1.codecs(), Assertions::assertEquals);

        // Encode and decode integers
        CodecResult cr2 = CodecHelper.encodeInt(i -> i, 10000, true);
        CodecHelper.decodeInt(slice.wrap(cr2.data()), cr2.codecs(), Assertions::assertEquals);

        // Encode and decode longs
        CodecResult cr3 = CodecHelper.encodeLong(i -> i, 10000, true);
        CodecHelper.decodeLong(slice.wrap(cr3.data()), cr3.codecs(), Assertions::assertEquals);

        // Encode and decode floats
        CodecResult cr4 = CodecHelper.encodeFloat(i -> i, 10000);
        CodecHelper.decodeFloat(slice.wrap(cr4.data()), cr4.codecs(), Assertions::assertEquals);

        // Encode and decode doubles
        CodecResult cr5 = CodecHelper.encodeDouble(i -> i, 10000);
        CodecHelper.decodeDouble(slice.wrap(cr5.data()), cr5.codecs(), Assertions::assertEquals);

    }

    @Test void testLong() {
        CodecSlice s = new CodecSlice();

        CodecResult cr1 = CodecHelper.encodeLong(i -> i, 10000, true);
        Assertions.assertEquals(cr1.codecs(), CODEC_SIMPLE8 | CODEC_VAR_INT);
        CodecHelper.decodeLong(s.wrap(cr1.data()), cr1.codecs(), Assertions::assertEquals);

        CodecResult cr2 = CodecHelper.encodeLong(i -> 10000 - i, 10000, false);
        Assertions.assertEquals(cr2.codecs(), CODEC_SIMPLE8 | CODEC_ZIGZAG);
        CodecHelper.decodeLong(s.wrap(cr2.data()), cr2.codecs(), (i, v) -> Assertions.assertEquals( 10000 - i, v));

    }

    @Test void testFloat() {
        CodecSlice s = new CodecSlice();

        CodecResult cr1 = CodecHelper.encodeFloat(i -> 5000 - i, 10000);
        Assertions.assertEquals(cr1.codecs(), CODEC_GORILLA);
        CodecHelper.decodeFloat(s.wrap(cr1.data()), cr1.codecs(), (i, v) -> Assertions.assertEquals(5000 - i, v));

    }

    @Test void testDouble() {
        CodecSlice s = new CodecSlice();

        CodecResult cr1 = CodecHelper.encodeDouble(i -> 5000 - i, 10000);
        Assertions.assertEquals(cr1.codecs(), CODEC_GORILLA);
        CodecHelper.decodeDouble(s.wrap(cr1.data()), cr1.codecs(), (i, v) -> Assertions.assertEquals(5000 - i, v));

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
