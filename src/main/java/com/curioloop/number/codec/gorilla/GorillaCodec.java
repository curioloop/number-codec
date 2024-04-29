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
package com.curioloop.number.codec.gorilla;

import com.curioloop.number.codec.CodecBuffer;
import com.curioloop.number.codec.CodecException;
import com.curioloop.number.codec.stream.DoubleSetter;
import com.curioloop.number.codec.stream.FloatGetter;
import com.curioloop.number.codec.stream.FloatSetter;
import com.curioloop.number.codec.CodecSlice;
import com.curioloop.number.codec.stream.DoubleGetter;

/**
 * This class provides methods for encoding and decoding time series data using the Gorilla compression algorithm.
 * <p>
 * Gorilla is a lossless compression technique specifically designed for time series data where subsequent values are often similar.
 * The class offers functionalities for both double (64-bit) and float (32-bit) precision data.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/22
 * @see <a href="http://www.vldb.org/pvldb/vol8/p1816-teller.pdf">Gorilla: A Fast, Scalable, In-Memory Time Series Database</a>
 */
public final class GorillaCodec {

    private static final int MAX_LEADING_ZERO_BITS = 6;
    private static final int MAX_BLOCK_SIZE_BITS = 7;
    private static final int BLOCK_SIZE_MASK = ~(~0 << MAX_BLOCK_SIZE_BITS);

    /**
     * Encodes a block of data using Gorilla encoding.
     *
     * @param buffer     The BitsWriter to write encoded bits into.
     * @param prev       The metadata of previous block.
     * @param meta       The metadata of current block.
     * @param value      The data to be encoded.
     */
    private static void encodeBlock(BitsWriter buffer, int prev, int meta, long value) {
        if (value == 0) {
            buffer.writeBit(false);
        } else {
            boolean ctrlBit;
            buffer.writeBit(true);
            buffer.writeBit(ctrlBit = meta != prev);
            if (ctrlBit) {
                buffer.writeBits(meta, MAX_LEADING_ZERO_BITS + MAX_BLOCK_SIZE_BITS);
            }
            buffer.writeBits(value, BLOCK_SIZE_MASK & meta);
        }
    }

    /**
     * Encodes an array of double values into a CodecBuffer using Gorilla encoding.
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
        int prevBlock = 0;
        double previous = values.get(0);
        writer.writeBits(Double.doubleToLongBits(previous), Double.SIZE);
        for (int n = 1; n < length; n++) {

            double value = values.get(n);
            long a = Double.doubleToLongBits(previous);
            long b = Double.doubleToLongBits(value);
            long xor = a ^ b;
            int leadingZero = (short) Long.numberOfLeadingZeros(xor);
            int tailingZero = (short) Long.numberOfTrailingZeros(xor);
            long diffBits = xor >>> tailingZero;
            int diffSize = Long.signum(diffBits) * (Double.SIZE - leadingZero - tailingZero);
            CodecException.valueOverflow(diffSize >= (1 << MAX_BLOCK_SIZE_BITS));

            int currBlock = (leadingZero << MAX_BLOCK_SIZE_BITS) | diffSize;
            encodeBlock(writer, prevBlock, currBlock, diffBits);
            prevBlock = currBlock;
            previous = value;
        }
        writer.flushBits();
        return buffer;
    }

    /**
     * Decodes a CodecSlice containing Gorilla-encoded double values into a DoubleSetter stream.
     *
     * @param slice The CodecSlice to decode.
     * @param stream The DoubleSetter stream to write the decoded values to.
     * @throws CodecException If the CodecSlice is malformed.
     */
    public static void decode64(CodecSlice slice, DoubleSetter stream) throws CodecException {
        CodecException.notAllow(slice.length() < 2);
        BitsReader reader = new BitsReader(slice.value(), slice.offset(), slice.length());
        int tailingZero = 0, blockSize = 0, count = 0;
        long value = reader.readBits(Double.SIZE);
        stream.set(count++, Double.longBitsToDouble(value));
        while (reader.hasMore()) {
            long bits = 0;
            if (reader.readBit()) {
                if (reader.readBit()) {
                    int meta = (int) reader.readBits(MAX_LEADING_ZERO_BITS + MAX_BLOCK_SIZE_BITS);
                    blockSize = meta & BLOCK_SIZE_MASK;
                    tailingZero = (Double.SIZE - blockSize - (meta >>> MAX_BLOCK_SIZE_BITS));
                }
                CodecException.malformedData((blockSize | tailingZero) == 0);
                bits = reader.readBits(blockSize) << tailingZero;
            }

            value ^= bits;
            stream.set(count++, Double.longBitsToDouble(value));
        }
    }


    /**
     * Encodes an array of float values into a CodecBuffer using Gorilla encoding.
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
        int prevBlock = 0;
        float previous = values.get(0);
        writer.writeBits(Float.floatToIntBits(previous), Float.SIZE);
        for (int n = 1; n < length; n++) {

            float value = values.get(n);
            int a = Float.floatToIntBits(previous);
            int b = Float.floatToIntBits(value);
            int xor = a ^ b;
            int leadingZero = (short) Integer.numberOfLeadingZeros(xor);
            int tailingZero = (short) Integer.numberOfTrailingZeros(xor);
            int diffBits = xor >>> tailingZero;
            int diffSize = Long.signum(diffBits) * (Float.SIZE - leadingZero - tailingZero);
            CodecException.valueOverflow(diffSize >= (1 << MAX_BLOCK_SIZE_BITS));

            int currBlock = (leadingZero << MAX_BLOCK_SIZE_BITS) | diffSize;
            encodeBlock(writer, prevBlock, currBlock, diffBits);
            prevBlock = currBlock;
            previous = value;
        }
        writer.flushBits();
        return buffer;
    }

    /**
     * Decodes a CodecSlice containing Gorilla-encoded float values into a FloatSetter stream.
     *
     * @param slice The CodecSlice to decode.
     * @param stream The FloatSetter stream to write the decoded values to.
     * @throws CodecException If the CodecSlice is malformed.
     */
    public static void decode32(CodecSlice slice, FloatSetter stream) throws CodecException {
        CodecException.notAllow(slice.length() < 2);
        BitsReader reader = new BitsReader(slice.value(), slice.offset(), slice.length());
        int tailingZero = 0, blockSize = 0, count = 0;
        long value = reader.readBits(Float.SIZE);
        stream.set(count++, Float.intBitsToFloat((int) value));
        while (reader.hasMore()) {
            long bits = 0;
            if (reader.readBit()) {
                if (reader.readBit()) {
                    int meta = (int) reader.readBits(MAX_LEADING_ZERO_BITS + MAX_BLOCK_SIZE_BITS);
                    blockSize = meta & BLOCK_SIZE_MASK;
                    tailingZero = (Float.SIZE - blockSize - (meta >>> MAX_BLOCK_SIZE_BITS));
                }
                CodecException.malformedData((blockSize | tailingZero) == 0);
                bits = reader.readBits(blockSize) << tailingZero;
            }
            value ^= bits;
            stream.set(count++, Float.intBitsToFloat((int) value));
        }
    }

    public static String toBinaryString(byte[] values) {
        return toBinaryString(values, 0, values == null ? 0 : values.length);
    }

    public static String toBinaryString(byte[] values, int offset, int length) {
        if (values == null) return "null";
        if (length == 0 || offset >= values.length) return "[]";
        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            String binary = Integer.toBinaryString(Byte.toUnsignedInt(values[offset+i]));
            b.append("00000000", binary.length(), 8).append(binary);
            if (i == length - 1)
                return b.append(']').toString();
            b.append(", ");
        }
    }
}


