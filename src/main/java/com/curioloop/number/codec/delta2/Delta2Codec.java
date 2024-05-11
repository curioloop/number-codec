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
package com.curioloop.number.codec.delta2;

import com.curioloop.number.codec.CodecBuffer;
import com.curioloop.number.codec.stream.LongGetter;
import com.curioloop.number.codec.stream.LongSetter;
import com.curioloop.number.codec.CodecException;
import com.curioloop.number.codec.CodecSlice;
import com.curioloop.number.codec.simple8.Simple8Codec;
import com.curioloop.number.codec.varint.VarIntCodec;

/**
 * Provides methods for encoding and decoding delta values using the Delta2 encoding scheme.
 * This scheme efficiently encodes differences between consecutive int/long value.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
public class Delta2Codec {

    /**
     * Encodes an array of long values into a CodecBuffer using the Delta2 encoding scheme.
     *
     * @param values The array of long values to encode.
     * @param length The number of values to encode.
     * @param ordered Specifies whether the values are ordered or not.
     * @param buffer The CodecBuffer to write the encoded values to.
     * @return The CodecBuffer with the encoded values.
     * @throws CodecException If an error occurs during encoding.
     */
    public static CodecBuffer encode(LongGetter values, final int length, boolean ordered, CodecBuffer buffer) throws CodecException {
        if (ordered) { // the delta of ordered value is always positive
            final long base = values.get(0);
            buffer.putLong(base);
            Simple8Codec.encode(new Delta2Getter( true, values, base, 1), length - 1, buffer);
        } else {
            VarIntCodec.encode64(new Delta2Getter(false, values, 0, 0), length, false, buffer);
        }
        return buffer;
    }

    /**
     * Decodes a CodecSlice containing Delta2-encoded long values into a LongSetter stream.
     *
     * @param slice The CodecSlice to decode.
     * @param stream The LongSetter stream to write the decoded values to.
     * @param ordered Specifies whether the values are ordered or not.
     * @throws CodecException If the CodecSlice is malformed.
     */
    public static void decode(CodecSlice slice, LongSetter stream, boolean ordered) throws CodecException {
        if (ordered) {
            final byte[] value = slice.value();
            final int offset = slice.offset(), length = slice.length();
            slice = slice.wrap(value, offset + 8, length - 8);
            final long base = CodecSlice.getLong(value, offset);
            stream.set(0, base);
            try {
                Simple8Codec.decode(slice.wrap(value, offset + 8, length - 8), new Delta2Setter(stream, base, 1));
            } finally {
                slice.wrap(value, offset, length);
            }
        } else {
            VarIntCodec.decode64(slice, new Delta2Setter(stream, 0, 0), false);
        }
    }

}
