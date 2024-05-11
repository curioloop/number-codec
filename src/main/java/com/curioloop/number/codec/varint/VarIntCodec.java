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
package com.curioloop.number.codec.varint;

import com.curioloop.number.codec.CodecBuffer;
import com.curioloop.number.codec.CodecException;
import com.curioloop.number.codec.stream.IntGetter;
import com.curioloop.number.codec.stream.IntSetter;
import com.curioloop.number.codec.stream.LongGetter;
import com.curioloop.number.codec.stream.LongSetter;
import com.curioloop.number.codec.CodecSlice;

/**
 * Provides methods for encoding and decoding 32-bit integers and 64-bit longs using the ZigZag or VarInt encoding scheme.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
public class VarIntCodec {

    /**
     * Encodes 32-bit integers using the ZigZag or VarInt encoding scheme.
     *
     * @param values   the values to encode
     * @param length   the length of the values array
     * @param unsigned whether the values are unsigned
     * @param buffer   the CodecBuffer to store the encoded data
     * @return the CodecBuffer containing the encoded data
     * @throws CodecException if encoding fails due to invalid input or buffer overflow
     */
    public static CodecBuffer encode32(IntGetter values, final int length, boolean unsigned, CodecBuffer buffer) throws CodecException {
        byte[] buf = {0, 0, 0, 0, 0};
        if (unsigned) {
            for (int i=0; i<length; i++) {
                int len = VarInt.encodeInt(values.get(i), buf, 0);
                buffer.putArray(buf, 0, len);
            }
        } else {
            for (int i=0; i<length; i++) {
                int len = ZigZag.encodeInt(values.get(i), buf, 0);
                buffer.putArray(buf, 0, len);
            }
        }
        return buffer;
    }

    /**
     * Encodes 64-bit longs using the ZigZag or VarInt encoding scheme.
     *
     * @param values   the values to encode
     * @param length   the length of the values array
     * @param unsigned whether the values are unsigned
     * @param buffer   the CodecBuffer to store the encoded data
     * @return the CodecBuffer containing the encoded data
     * @throws CodecException if encoding fails due to invalid input or buffer overflow
     */
    public static CodecBuffer encode64(LongGetter values, final int length, boolean unsigned, CodecBuffer buffer) throws CodecException {
        byte[] buf = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        if (unsigned) {
            for (int i=0; i<length; i++) {
                int len = VarInt.encodeLong(values.get(i), buf, 0);
                buffer.putArray(buf, 0, len);
            }
        } else {
            for (int i=0; i<length; i++) {
                int len = ZigZag.encodeLong(values.get(i), buf, 0);
                buffer.putArray(buf, 0, len);
            }
        }
        return buffer;
    }

    /**
     * Decodes 32-bit integers encoded using the ZigZag or VarInt encoding scheme.
     *
     * @param slice    the CodecSlice containing the encoded data
     * @param stream   the IntSetter to receive the decoded values
     * @param unsigned whether the values were encoded as unsigned
     * @throws CodecException if decoding fails due to invalid input or buffer overflow
     */
    public static void decode32(CodecSlice slice, IntSetter stream, boolean unsigned) throws CodecException {
        int count = 0, offset = slice.offset(), length = slice.length();
        int[] cursor = {offset};
        if (unsigned) {
            while (cursor[0] < offset + length) {
                int value = VarInt.decodeInt(slice.value(), cursor);
                stream.set(count++, value);
            }
        } else {
            while (cursor[0] < offset + length) {
                int value = ZigZag.decodeInt(slice.value(), cursor);
                stream.set(count++, value);
            }
        }
    }

    /**
     * Decodes 64-bit longs encoded using the ZigZag or VarInt encoding scheme.
     *
     * @param slice    the CodecSlice containing the encoded data
     * @param stream   the LongSetter to receive the decoded values
     * @param unsigned whether the values were encoded as unsigned
     * @throws CodecException if decoding fails due to invalid input or buffer overflow
     */
    public static void decode64(CodecSlice slice, LongSetter stream, boolean unsigned) throws CodecException {
        int count = 0, offset = slice.offset(), length = slice.length();
        int[] cursor = {offset};
        if (unsigned) {
            while (cursor[0] < offset + length) {
                long value = VarInt.decodeLong(slice.value(), cursor);
                stream.set(count++, value);
            }
        } else {
            while (cursor[0] < offset + length) {
                long value = ZigZag.decodeLong(slice.value(), cursor);
                stream.set(count++, value);
            }
        }
    }

}
