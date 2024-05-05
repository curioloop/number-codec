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

import static com.curioloop.number.codec.chimp.Chimp.*;

/**
 * @author curioloops@gmail.com
 * @since 2024/5/5
 * @see <a href="https://github.com/panagiotisl/chimp/blob/main/src/main/java/gr/aueb/delorean/chimp/ChimpN.java">ChimpN</a>
 * @see <a href="https://github.com/panagiotisl/chimp/blob/main/src/main/java/gr/aueb/delorean/chimp/ChimpNDecompressor.java">ChimpNDecompressor</a>
 */
public class ChimpN {

    /**
     * Encodes an array of double values into a CodecBuffer using Chimp encoding with N-value caching.
     *
     * @param values The array of double values to encode.
     * @param length The number of values to encode.
     * @param buffer The CodecBuffer to write the encoded values to.
     * @param N      The size of the caching ring buffer.
     * @return The CodecBuffer with the encoded values.
     * @throws CodecException If an error occurs during encoding.
     */
    public static CodecBuffer encode64(DoubleGetter values, final int length, CodecBuffer buffer, final int N) throws CodecException {
        // N must be 2ⁿ to ensure the index not overflow
        CodecException.notAllow(length <= 0 || N < 4 || N > 256 || Integer.bitCount(N) > 1);
        BitsWriter writer = new BitsWriter(buffer);

        final int log2N = 31 - Integer.numberOfLeadingZeros(N);
        final int threshold = MAX_LOG2_64 + log2N;
        // the number of bits needed to denote the previous value used (𝑙𝑜𝑔2(N))
        // the number of bits required to specify the number of meaningful bits (𝑙𝑜𝑔2(64))

        // a ring buffer cached most recent N values
        long[] previous = new long[N];

        // look up for the previous value with the same LSB in ring buffer
        int maskLSB = (1 << (threshold+1)) - 1; // 2^(threshold + 1) - 1
        int[] indices = new int[1 << (threshold+1)]; // 2^(threshold + 1)

        int index = 0;
        int current = 0;
        int prevLeading = 0;

        previous[current] = Double.doubleToLongBits(values.get(0));
        writer.writeBits(previous[current], Double.SIZE);
        indices[(int) previous[current] & maskLSB] = index;

        for (int n = 1; n < length; n++) {
            final long value = Double.doubleToLongBits(values.get(n));
            final int key = (int) value & maskLSB;

            long xor;
            int trailingZeros = 0;
            // currIndex point to a most recent previous value which may have same LSB
            int currIndex = indices[key], prevIndex;
            if ((index - currIndex) < N) {
                // When currIndex is no more than N from the index
                // try XOR with this previous value
                long tempXor = value ^ previous[currIndex % N];
                trailingZeros = Long.numberOfTrailingZeros(tempXor);
                if (trailingZeros > threshold) {
                    // If the resulting number of trailing zeros surpasses threshold
                    // then we make use of and actually store the previous value used
                    prevIndex = currIndex % N; // use buffered previous value
                    xor = tempXor;
                } else {
                    // Otherwise, the use of best previous value is not particularly useful
                    // we can use the immediately previous value instead, and avoid wasting additional bits to denote the previous value used
                    prevIndex = index % N; // use immediate previous value
                    xor = previous[prevIndex] ^ value;
                }
            } else {
                prevIndex = index % N;
                xor = previous[prevIndex] ^ value;
            }

            if (xor == 0) {
                writer.writeBits(prevIndex, CTRL_FLAG_BITS + log2N);
                prevLeading = Double.SIZE + 1;
            } else {
                int leadingZeros = leadingRound[Long.numberOfLeadingZeros(xor)];
                if (trailingZeros > threshold) {
                    int significantBits = Double.SIZE - leadingZeros - trailingZeros;
                    long controlMeta = 0b01;
                    controlMeta = (controlMeta << log2N) | prevIndex;
                    controlMeta = (controlMeta << LEADING_COUNT_BITS) | leadingEncode[leadingZeros];
                    controlMeta = (controlMeta << DOUBLE_CENTER_BITS) | significantBits;
                    writer.writeBits(controlMeta, CTRL_FLAG_BITS + log2N + LEADING_COUNT_BITS + DOUBLE_CENTER_BITS);
                    writer.writeBits(xor >>> trailingZeros, significantBits); // center bits
                    prevLeading = Double.SIZE + 1;
                } else if (leadingZeros == prevLeading) {
                    writer.writeBits(0b10, CTRL_FLAG_BITS);
                    writer.writeBits(xor, Double.SIZE - leadingZeros); // non-lead bits
                } else {
                    prevLeading = leadingZeros;
                    writer.writeBits(24 + leadingEncode[leadingZeros],
                                    CTRL_FLAG_BITS + LEADING_COUNT_BITS);
                    writer.writeBits(xor, Double.SIZE - leadingZeros); // non-lead bits
                }
            }
            current = (current + 1) % N;
            previous[current] = value; // record the value to ring buffer
            indices[key] = ++index; // update the biggest index for specific LSB
            // assert index % N == current;
        }
        writer.flushBits();
        return buffer;
    }

    /**
     * Decodes a CodecSlice containing Chimp-encoded double values into a DoubleSetter stream with N-value caching.
     *
     * @param slice  The CodecSlice to decode.
     * @param stream The DoubleSetter stream to write the decoded values to.
     * @param N      The size of the caching ring buffer.
     * @throws CodecException If the CodecSlice is malformed.
     */
    public static void decode64(CodecSlice slice, DoubleSetter stream, final int N) throws CodecException {
        // N must be 2ⁿ to ensure the index not overflow
        CodecException.notAllow(slice.length() < 2 || N < 4 || N > 256 || Integer.bitCount(N) > 1);
        BitsReader reader = new BitsReader(slice.value(), slice.offset(), slice.length());

        long[] previous = new long[N];
        int log2N = 31 - Integer.numberOfLeadingZeros(N);
        int prevMask = (1 << log2N) - 1;

        int current = 0, count = 0;
        long value = reader.readBits(Double.SIZE);
        stream.set(count++, Double.longBitsToDouble(value));
        previous[current] = value;

        int prevLeading = 0;
        while (reader.hasMore()) {
            long bits = 0;
            switch ((int) reader.readBits(CTRL_FLAG_BITS)) {
                case 0b11:
                    prevLeading = leadingDecode[(int) reader.readBits(LEADING_COUNT_BITS)];
                case 0b10:
                    bits = reader.readBits(Double.SIZE - prevLeading);
                    break;
                case 0b01:
                    int meta = (int) reader.readBits(log2N + LEADING_COUNT_BITS + DOUBLE_CENTER_BITS);
                    int index = (meta >>> (LEADING_COUNT_BITS + DOUBLE_CENTER_BITS)) & prevMask;
                    prevLeading = leadingDecode[(meta >>> DOUBLE_CENTER_BITS) & LEADING_COUNT_MASK];
                    int significantBits = meta & DOUBLE_CENTER_MASK;
                    if (significantBits == 0) significantBits = Double.SIZE;
                    int trailingZeros = Double.SIZE - significantBits - prevLeading;
                    bits = reader.readBits(significantBits) << trailingZeros;
                    value = previous[index];
                    break;
                default:
                    value = previous[(int) reader.readBits(log2N)];
            }

            value ^= bits;
            stream.set(count++, Double.longBitsToDouble(value));

            current = (current + 1) % N;
            previous[current] = value;
        }
    }

    /**
     * Encodes an array of float values into a CodecBuffer using Chimp encoding with N-value caching.
     *
     * @param values The array of float values to encode.
     * @param length The number of values to encode.
     * @param buffer The CodecBuffer to write the encoded values to.
     * @param N      The size of the caching ring buffer.
     * @return The CodecBuffer with the encoded values.
     * @throws CodecException If an error occurs during encoding.
     */
    public static CodecBuffer encode32(FloatGetter values, final int length, CodecBuffer buffer, final int N) throws CodecException {
        // N must be 2ⁿ to ensure the index not overflow
        CodecException.notAllow(length <= 0 || N < 4 || N > 256 || Integer.bitCount(N) > 1);
        BitsWriter writer = new BitsWriter(buffer);

        final int log2N = 31 - Integer.numberOfLeadingZeros(N);
        final int threshold = MAX_LOG2_32 + log2N;
        // the number of bits needed to denote the previous value used (𝑙𝑜𝑔2(N))
        // the number of bits required to specify the number of meaningful bits (𝑙𝑜𝑔2(32))

        // a ring buffer cached most recent N values
        int[] previous = new int[N];

        // look up for the previous value with the same LSB in ring buffer
        int maskLSB = (1 << (threshold+1)) - 1; // 2^(threshold + 1) - 1
        int[] indices = new int[1 << (threshold+1)]; // 2^(threshold + 1)

        int index = 0;
        int current = 0;
        int prevLeading = 0;

        previous[current] = Float.floatToIntBits(values.get(0));
        writer.writeBits(previous[current], Float.SIZE);
        indices[previous[current] & maskLSB] = index;

        for (int n = 1; n < length; n++) {
            final int value = Float.floatToIntBits(values.get(n));
            final int key = value & maskLSB;

            int xor;
            int trailingZeros = 0;
            // currIndex point to a most recent previous value which may have same LSB
            int currIndex = indices[key], prevIndex;
            if ((index - currIndex) < N) {
                // When currIndex is no more than N from the index
                // try XOR with this previous value
                int tempXor = value ^ previous[currIndex % N];
                trailingZeros = Integer.numberOfTrailingZeros(tempXor);
                if (trailingZeros > threshold) {
                    // If the resulting number of trailing zeros surpasses threshold
                    // then we make use of and actually store the previous value used
                    prevIndex = currIndex % N; // use buffered previous value
                    xor = tempXor;
                } else {
                    // Otherwise, the use of best previous value is not particularly useful
                    // we can use the immediately previous value instead, and avoid wasting additional bits to denote the previous value used
                    prevIndex = index % N; // use immediate previous value
                    xor = previous[prevIndex] ^ value;
                }
            } else {
                prevIndex = index % N;
                xor = previous[prevIndex] ^ value;
            }

            if (xor == 0) {
                writer.writeBits(prevIndex, CTRL_FLAG_BITS + log2N);
                prevLeading = Float.SIZE + 1;
            } else {
                int leadingZeros = leadingRound[Integer.numberOfLeadingZeros(xor)];
                if (trailingZeros > threshold) {
                    int significantBits = Float.SIZE - leadingZeros - trailingZeros;
                    long significantMask = (1L << significantBits) - 1;
                    long controlMeta = 0b01;
                    controlMeta = (controlMeta << log2N) | prevIndex;
                    controlMeta = (controlMeta << LEADING_COUNT_BITS) | leadingEncode[leadingZeros];
                    controlMeta = (controlMeta << FLOAT_CENTER_BITS) | significantBits;
                    writer.writeBits((controlMeta << significantBits) | ((xor >>> trailingZeros) & significantMask),
                            CTRL_FLAG_BITS + log2N + LEADING_COUNT_BITS + FLOAT_CENTER_BITS + significantBits); // center bits
                    prevLeading = Float.SIZE + 1;
                } else if (leadingZeros == prevLeading) {
                    int significantBits = Float.SIZE - leadingZeros;
                    long significantMask = (1L << significantBits) - 1;
                    writer.writeBits((0b10L << significantBits) | (xor & significantMask), CTRL_FLAG_BITS + significantBits); // non-lead bits
                } else {
                    prevLeading = leadingZeros;
                    int significantBits = Float.SIZE - leadingZeros;
                    long significantMask = (1L << significantBits) - 1;
                    long controlMeta = 24 + leadingEncode[leadingZeros];
                    writer.writeBits((controlMeta << significantBits) | (xor & significantMask), CTRL_FLAG_BITS + LEADING_COUNT_BITS + significantBits); // non-lead bits
                }
            }
            current = (current + 1) % N;
            previous[current] = value; // record the value to ring buffer
            indices[key] = ++index; // update the biggest index for specific LSB
            // assert index % N == current;
        }
        writer.flushBits();
        return buffer;
    }

    /**
     * Decodes a CodecSlice containing Chimp-encoded float values into a FloatSetter stream with N-value caching.
     *
     * @param slice  The CodecSlice to decode.
     * @param stream The FloatSetter stream to write the decoded values to.
     * @param N      The size of the caching ring buffer.
     * @throws CodecException If the CodecSlice is malformed.
     */
    public static void decode32(CodecSlice slice, FloatSetter stream, final int N) throws CodecException {
        // N must be 2ⁿ to ensure the index not overflow
        CodecException.notAllow(slice.length() < 2 || N < 4 || N > 256 || Integer.bitCount(N) > 1);
        BitsReader reader = new BitsReader(slice.value(), slice.offset(), slice.length());

        int[] previous = new int[N];
        int log2N = 31 - Integer.numberOfLeadingZeros(N);
        int prevMask = (1 << log2N) - 1;

        int current = 0, count = 0;
        int value = (int) reader.readBits(Float.SIZE);
        stream.set(count++, Float.intBitsToFloat(value));
        previous[current] = value;

        int prevLeading = 0;
        while (reader.hasMore()) {
            int bits = 0;
            switch ((int) reader.readBits(CTRL_FLAG_BITS)) {
                case 0b11:
                    prevLeading = leadingDecode[(int) reader.readBits(LEADING_COUNT_BITS)];
                case 0b10:
                    bits = (int) reader.readBits(Float.SIZE - prevLeading);
                    break;
                case 0b01:
                    int meta = (int) reader.readBits(log2N + LEADING_COUNT_BITS + FLOAT_CENTER_BITS);
                    int index = (meta >>> (LEADING_COUNT_BITS + FLOAT_CENTER_BITS)) & prevMask;
                    prevLeading = leadingDecode[(meta >>> FLOAT_CENTER_BITS) & LEADING_COUNT_MASK];
                    int significantBits = meta & FLOAT_CENTER_MASK;
                    if (significantBits == 0) significantBits = 32;
                    int trailingZeros = 32 - significantBits - prevLeading;
                    bits = (int) (reader.readBits(significantBits) << trailingZeros);
                    value = previous[index];
                    break;
                default:
                    value = previous[(int) reader.readBits(log2N)];
            }

            value ^= bits;
            stream.set(count++, Float.intBitsToFloat(value));

            current = (current + 1) % N;
            previous[current] = value;
        }
    }

}
