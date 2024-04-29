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
package com.curioloop.number.codec.simple8;

import com.curioloop.number.codec.CodecBuffer;
import com.curioloop.number.codec.CodecException;
import com.curioloop.number.codec.stream.LongGetter;
import com.curioloop.number.codec.stream.LongSetter;
import com.curioloop.number.codec.CodecSlice;

/**
 * An optimized <a href="https://arxiv.org/pdf/1209.2137.pdf">simple8b</a> implementation base on <a href="https://github.com/jwilder/encoding/blob/master/simple8b/encoding.go">jwilder/encoding</a>.
 * We improve the encoding phase with a FastLookup table, which reducing time complexity from O(n²) to O(n).
 *
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
public class Simple8Codec {

    static final Packing[] selector = {
        new Packing240(), new Packing120(), new Packing60(), new Packing30(),
        new Packing20(), new Packing15(), new Packing12(), new Packing10(),
        new Packing8(), new Packing7(), new Packing6(), new Packing5(),
        new Packing4(), new Packing3(), new Packing2(), new Packing1()
    };

    static final FastLookup selectorLookup = new FastLookup();

    /**
     * Encodes an array of long values into a CodecBuffer.
     *
     * @param values The array of long values to encode.
     * @param length The number of values to encode.
     * @param buf    The CodecBuffer to write the encoded values to.
     * @return       The CodecBuffer with the encoded values.
     * @throws CodecException If an error occurs during encoding.
     */
    public static CodecBuffer encode(LongGetter values, final int length, CodecBuffer buf) throws CodecException {
        int pos = 0;
        while (pos < length) {
            Packing packing = selectorLookup.lookupPacking(values, pos, length);
            CodecException.valueOverflow(packing == null);
            buf.putLong(packing.pack(values, pos));
            pos += packing.integersCoded;
        }
        return buf;
    }

    /**
     * Decodes a CodecSlice into a LongSetter stream.
     *
     * @param slice The CodecSlice to decode.
     * @param stream The LongSetter stream to write the decoded values to.
     * @return The number of values decoded.
     * @throws CodecException If the CodecSlice is malformed.
     */
    public static int decode(CodecSlice slice, LongSetter stream) {
        int count = 0;
        CodecException.malformedData(slice.length() % 8 > 0);
        final byte[] bytes = slice.value();
        final int beg = slice.offset(), end = slice.offset() + slice.length();
        for (int i = beg; i < end; i += 8) {
            long v = CodecSlice.getLong(bytes, i);
            Packing packing = selector[(int)(v >>> 60)];
            packing.unpack(v, stream, count);
            count += packing.integersCoded;
        }
        return count;
    }

}




