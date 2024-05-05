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
import com.curioloop.number.codec.stream.DoubleGetter;
import com.curioloop.number.codec.stream.DoubleSetter;
import com.curioloop.number.codec.stream.FloatGetter;
import com.curioloop.number.codec.stream.FloatSetter;

/**
 * This class provides methods for encoding and decoding time series data using the Chimp compression algorithm.
 * <p>
 * Chimp is a lossless compression technique like Gorilla.
 * It provides a adaptive approach to detect the period pattern in the data to achieve better compression ratio.
 *
 * @author curioloops@gmail.com
 * @since 2024/5/5
 * @see <a href="https://www.vldb.org/pvldb/vol15/p3058-liakos.pdf">Chimp: Efficient Lossless Floating Point Compression for Time Series Databases</a>
 */
public class ChimpCodec {

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
        CodecException.notAllow((N != 0 && N < 4) || N > 256 || Integer.bitCount(N) > 1);
        buffer.putByte((byte) (N == 0 ? 0 : Integer.numberOfTrailingZeros(N)));
        if (N == 0)
            return Chimp.encode64(values, length, buffer);
        else
            return ChimpN.encode64(values, length, buffer, N);
    }

    /**
     * Decodes a CodecSlice containing Chimp-encoded double values into a DoubleSetter stream with N-value caching.
     *
     * @param slice  The CodecSlice to decode.
     * @param stream The DoubleSetter stream to write the decoded values to.
     * @throws CodecException If the CodecSlice is malformed.
     */
    public static void decode64(CodecSlice slice, DoubleSetter stream) throws CodecException {
        final byte[] value = slice.value();
        final int offset = slice.offset(), length = slice.length();
        final int N = value[offset] == 0 ? 0 : (1 << value[0]);
        CodecException.notAllow((N != 0 && N < 4) || N > 256);
        try {
            slice.wrap(value, offset + 1, length - 1);
            if (N == 0)
                Chimp.decode64(slice, stream);
            else
                ChimpN.decode64(slice, stream, N);
        } finally {
            slice.wrap(value, offset, length);
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
        CodecException.notAllow((N != 0 && N < 4) || N > 256 || Integer.bitCount(N) > 1);
        buffer.putByte((byte) (N == 0 ? 0 : Integer.numberOfTrailingZeros(N)));
        if (N == 0)
            return Chimp.encode32(values, length, buffer);
        else
            return ChimpN.encode32(values, length, buffer, N);
    }

    /**
     * Decodes a CodecSlice containing Chimp-encoded float values into a FloatSetter stream with N-value caching.
     *
     * @param slice  The CodecSlice to decode.
     * @param stream The FloatSetter stream to write the decoded values to.
     * @throws CodecException If the CodecSlice is malformed.
     */
    public static void decode32(CodecSlice slice, FloatSetter stream) throws CodecException {
        final byte[] value = slice.value();
        final int offset = slice.offset(), length = slice.length();
        final int N = value[offset] == 0 ? 0 : (1 << value[offset]);
        CodecException.notAllow((N != 0 && N < 4) || N > 256);
        try {
            slice.wrap(value, offset + 1, length - 1);
            if (N == 0)
                Chimp.decode32(slice, stream);
            else
                ChimpN.decode32(slice, stream, N);
        } finally {
            slice.wrap(value, offset, length);
        }
    }

}
