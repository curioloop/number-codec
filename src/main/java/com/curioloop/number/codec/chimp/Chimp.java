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
package com.curioloop.number.codec.chimp;


import com.curioloop.number.codec.CodecBuffer;
import com.curioloop.number.codec.CodecException;
import com.curioloop.number.codec.CodecSlice;
import com.curioloop.number.codec.gorilla.BitsReader;
import com.curioloop.number.codec.gorilla.BitsWriter;
import com.curioloop.number.codec.stream.DoubleGetter;
import com.curioloop.number.codec.stream.DoubleSetter;
import com.curioloop.number.codec.stream.FloatGetter;
import com.curioloop.number.codec.stream.FloatSetter;

/**
 * @author curioloops@gmail.com
 * @since 2024/5/2
 * @see <a href="https://github.com/panagiotisl/chimp/blob/main/src/main/java/gr/aueb/delorean/chimp/Chimp.java">Chimp</a>
 * @see <a href="https://github.com/panagiotisl/chimp/blob/main/src/main/java/gr/aueb/delorean/chimp/ChimpDecompressor.java">ChimpDecompressor</a>
 */
public class Chimp {

    static final int CTRL_FLAG_BITS = 2;
    static final int LEADING_COUNT_BITS = 3;
    static final int LEADING_COUNT_MASK = (1 << LEADING_COUNT_BITS) - 1;

    static final int DOUBLE_CENTER_BITS = 6;
    static final int DOUBLE_CENTER_MASK = ~(~0 << DOUBLE_CENTER_BITS);

    static final int FLOAT_CENTER_BITS = 5;
    static final int FLOAT_CENTER_MASK = ~(~0 << FLOAT_CENTER_BITS);

    static final int MAX_LOG2_64 = 6;
    static final int MAX_LOG2_32 = 5;

    static final short[] leadingRound = {
            0, 0, 0, 0, 0, 0, 0, 0,
            8, 8, 8, 8, 12, 12, 12, 12,
            16, 16, 18, 18, 20, 20, 22, 22,
            24, 24, 24, 24, 24, 24, 24, 24,
            24, 24, 24, 24, 24, 24, 24, 24,
            24, 24, 24, 24, 24, 24, 24, 24,
            24, 24, 24, 24, 24, 24, 24, 24,
            24, 24, 24, 24, 24, 24, 24, 24
    };

    static final short[] leadingEncode = {
            0, 0, 0, 0, 0, 0, 0, 0,
            1, 1, 1, 1, 2, 2, 2, 2,
            3, 3, 4, 4, 5, 5, 6, 6,
            7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7,
            7, 7, 7, 7, 7, 7, 7, 7
    };

    static final short[] leadingDecode = {0, 8, 12, 16, 18, 20, 22, 24};

    /**
     * Encodes an array of double values into a CodecBuffer using Chimp encoding.
     *
     * @param values The array of double values to encode.
     * @param length The number of values to encode.
     * @param buffer The CodecBuffer to write the encoded values to.
     * @return The CodecBuffer with the encoded values.
     * @throws CodecException If an error occurs during encoding.
     */
    public static CodecBuffer encode64(DoubleGetter values, final int length, CodecBuffer buffer) throws CodecException {
        CodecException.notAllow(length <= 0);
        BitsWriter writer = new BitsWriter(buffer);
        int prevLeading = 0;
        long previous = Double.doubleToLongBits(values.get(0));
        writer.writeBits(previous, Double.SIZE);
        for (int n = 1; n < length; n++) {
            long current = Double.doubleToLongBits(values.get(n));
            long xor = previous ^ current;
            if (xor == 0) { // values are identical
                writer.writeBits(0b00, CTRL_FLAG_BITS);
                prevLeading = Double.SIZE + 1;
            } else {
                int leadingZeros = leadingRound[Long.numberOfLeadingZeros(xor)];
                int trailingZeros = Long.numberOfTrailingZeros(xor);
                if (trailingZeros > MAX_LOG2_64) {
                    int significantBits = Double.SIZE - leadingZeros - trailingZeros;
                    writer.writeBits((0b01 << (LEADING_COUNT_BITS + DOUBLE_CENTER_BITS)) |
                            (leadingEncode[leadingZeros] << DOUBLE_CENTER_BITS) | // the length of the number of leading zeros (3 bits)
                            significantBits, // the length of the meaningful XORed value (6 bits)
                            CTRL_FLAG_BITS + LEADING_COUNT_BITS + DOUBLE_CENTER_BITS);
                    writer.writeBits(xor >>> trailingZeros, significantBits); // center bits
                    prevLeading = Double.SIZE + 1;
                } else if (leadingZeros == prevLeading) { // the number of leading zeros is exactly equal to the previous leading zeros
                    writer.writeBits(0b10, CTRL_FLAG_BITS);
                    writer.writeBits(xor, Double.SIZE - leadingZeros); // non-lead bits
                } else {
                    prevLeading = leadingZeros;
                    writer.writeBits((0b11 << LEADING_COUNT_BITS) |
                                    leadingEncode[leadingZeros], // the length of the number of leading zeros (3 bits)
                                CTRL_FLAG_BITS + LEADING_COUNT_BITS);
                    writer.writeBits(xor, Double.SIZE - leadingZeros); // non-lead bits
                }
            }
            previous = current;
        }
        writer.flushBits();
        return buffer;
    }

    /**
     * Decodes a CodecSlice containing Chimp-encoded double values into a DoubleSetter stream.
     *
     * @param slice The CodecSlice to decode.
     * @param stream The DoubleSetter stream to write the decoded values to.
     * @throws CodecException If the CodecSlice is malformed.
     */
    public static void decode64(CodecSlice slice, DoubleSetter stream) throws CodecException {
        CodecException.notAllow(slice.length() < 2);
        BitsReader reader = new BitsReader(slice.value(), slice.offset(), slice.length());
        int prevLeading = 0, count = 0;
        long value = reader.readBits(Double.SIZE);
        stream.set(count++, Double.longBitsToDouble(value));
        while (reader.hasMore()) {
            long bits = 0;
            switch ((int) reader.readBits(CTRL_FLAG_BITS)) {
                case 0b11:
                    prevLeading = leadingDecode[(int) reader.readBits(LEADING_COUNT_BITS)];
                case 0b10:
                    bits = reader.readBits(Double.SIZE - prevLeading);
                    break;
                case 0b01:
                    int meta = (int) reader.readBits(LEADING_COUNT_BITS + DOUBLE_CENTER_BITS);
                    prevLeading = leadingDecode[meta >>> DOUBLE_CENTER_BITS];
                    int significantBits = meta & DOUBLE_CENTER_MASK;
                    int trailingZeros = Double.SIZE - significantBits - prevLeading;
                    bits = reader.readBits(Double.SIZE - prevLeading - trailingZeros);
                    bits <<= trailingZeros;
                    break;
            }
            value ^= bits;
            stream.set(count++, Double.longBitsToDouble(value));
        }
    }


    /**
     * Encodes an array of float values into a CodecBuffer using Chimp encoding.
     *
     * @param values The array of float values to encode.
     * @param length The number of values to encode.
     * @param buffer The CodecBuffer to write the encoded values to.
     * @return The CodecBuffer with the encoded values.
     * @throws CodecException If an error occurs during encoding.
     */
    public static CodecBuffer encode32(FloatGetter values, final int length, CodecBuffer buffer) throws CodecException {
        CodecException.notAllow(length <= 0);
        BitsWriter writer = new BitsWriter(buffer);
        int prevLeading = 0;
        int previous = Float.floatToIntBits(values.get(0));
        writer.writeBits(previous, Float.SIZE);
        for (int n = 1; n < length; n++) {

            int current = Float.floatToIntBits(values.get(n));
            int xor = previous ^ current;

            if (xor == 0) {
                writer.writeBits(0b00, CTRL_FLAG_BITS);
                prevLeading = Float.SIZE + 1;
            } else {
                int leadingZeros = leadingRound[Integer.numberOfLeadingZeros(xor)];
                int trailingZeros = Integer.numberOfTrailingZeros(xor);
                if (trailingZeros > MAX_LOG2_32) {
                    int significantBits = Float.SIZE - leadingZeros - trailingZeros;
                    long significantMask = (1L << significantBits) - 1;
                    long controlMeta = (0b01 << (LEADING_COUNT_BITS + FLOAT_CENTER_BITS)) |
                                       (leadingEncode[leadingZeros] << FLOAT_CENTER_BITS) | significantBits;
                    writer.writeBits((controlMeta << significantBits) | ((xor >>> trailingZeros) & significantMask), CTRL_FLAG_BITS + LEADING_COUNT_BITS + FLOAT_CENTER_BITS + significantBits); // center bits
                    prevLeading = Float.SIZE + 1;
                } else if (leadingZeros == prevLeading) {
                    int significantBits = Float.SIZE - leadingZeros;
                    long significantMask = (1L << significantBits) - 1;
                    writer.writeBits((0b10L << significantBits) | (xor & significantMask), CTRL_FLAG_BITS + significantBits); // non-lead bits
                } else {
                    prevLeading = leadingZeros;
                    int significantBits = Float.SIZE - leadingZeros;
                    long significantMask = (1L << significantBits) - 1;
                    long controlMeta = (0b11 << LEADING_COUNT_BITS) | leadingEncode[leadingZeros];
                    writer.writeBits((controlMeta << significantBits) | (xor & significantMask), CTRL_FLAG_BITS + LEADING_COUNT_BITS + significantBits); // non-lead bits
                }
            }
            previous = current;
        }
        writer.flushBits();
        return buffer;
    }

    /**
     * Decodes a CodecSlice containing Chimp-encoded float values into a FloatSetter stream.
     *
     * @param slice The CodecSlice to decode.
     * @param stream The FloatSetter stream to write the decoded values to.
     * @throws CodecException If the CodecSlice is malformed.
     */
    public static void decode32(CodecSlice slice, FloatSetter stream) throws CodecException {
        CodecException.notAllow(slice.length() < 2);
        BitsReader reader = new BitsReader(slice.value(), slice.offset(), slice.length());
        int prevLeading = 0, count = 0;
        int value = (int) reader.readBits(Float.SIZE);
        stream.set(count++, Float.intBitsToFloat(value));
        while (reader.hasMore()) {
            int bits = 0;
            switch ((int) reader.readBits(CTRL_FLAG_BITS)) {
                case 0b11:
                    prevLeading = leadingDecode[(int) reader.readBits(LEADING_COUNT_BITS)];
                case 0b10:
                    bits = (int) reader.readBits(Float.SIZE - prevLeading);
                    break;
                case 0b01:
                    int meta = (int) reader.readBits(LEADING_COUNT_BITS + FLOAT_CENTER_BITS);
                    prevLeading = leadingDecode[meta >>> FLOAT_CENTER_BITS];
                    int significantBits = meta & FLOAT_CENTER_MASK;
                    int trailingZeros = Float.SIZE - significantBits - prevLeading;
                    bits = (int) reader.readBits(significantBits);
                    bits <<= trailingZeros;
                    break;
            }
            value ^= bits;
            stream.set(count++, Float.intBitsToFloat(value));
        }
    }

}
