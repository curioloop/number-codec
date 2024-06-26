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
package com.curioloop.number.codec;

import com.curioloop.number.codec.chimp.ChimpCodec;
import com.curioloop.number.codec.delta2.Delta2Codec;
import com.curioloop.number.codec.gorilla.GorillaCodec;
import com.curioloop.number.codec.simple8.Simple8Codec;
import com.curioloop.number.codec.stream.*;
import com.curioloop.number.codec.varint.VarIntCodec;
import com.curioloop.number.codec.varint.VarIntStream;

/**
 *  Provides common preset workflows for encoding and decoding various data types using different codecs.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
public class CodecHelper {

    /**
     * Specify the ring buffer size of Chimp
     */
    public static int DEFAULT_CHIMP_N = 32;

    /**
     * Encodes delta values with various codecs.
     *
     * @param values  the values to encode
     * @param length  the length of the values array
     * @param buffer  the CodecBuffer to store encoded data
     * @return a CodecResult object containing the encoded data and codec type
     * @throws CodecException if encoding fails due to invalid input or overflow
     */
    public static CodecResult encodeDelta2(LongGetter values, final int length, CodecBuffer buffer) throws CodecException {
        // codec delta2
        // 1. try delta2-simple8b (sorted number)
        // 2. store delta2-zigzag (unsorted number)
        CodecException.notAllow(values == null || length <= 0);
        try {
            Delta2Codec.encode(values, length, true, buffer.forgetPos());
            return CodecResult.of(buffer.toArray(), CodecResult.CODEC_DELTA2 | CodecResult.CODEC_SIMPLE8);
        } catch (CodecException.ValueOverflow overflow) {
            Delta2Codec.encode(values, length, false, buffer.forgetPos());
            return CodecResult.of(buffer.toArray(), CodecResult.CODEC_DELTA2 | CodecResult.CODEC_ZIGZAG);
        }
    }

    /**
     * Decodes delta values with specific codecs.
     *
     * @param slice   the CodecSlice containing the encoded data
     * @param codecs  the codec type used for encoding
     * @param stream  the LongSetter to receive the decoded values
     * @throws CodecException if decoding fails due to invalid input or codec mismatch
     */
    public static void decodeDelta2(CodecSlice slice, int codecs, LongSetter stream) throws CodecException {
        CodecException.notAllow(slice == null || stream == null);
        CodecException.notAllow((codecs & CodecResult.CODEC_DELTA2) == 0 || (codecs & (CodecResult.CODEC_SIMPLE8 | CodecResult.CODEC_ZIGZAG)) == 0);
        Delta2Codec.decode(slice, stream, (codecs & CodecResult.CODEC_ZIGZAG) == 0);
    }

    /**
     * Encodes float values with various codecs.
     *
     * @param values  the values to encode
     * @param length  the length of the values array
     * @param buffer  the CodecBuffer to store encoded data
     * @return a CodecResult object containing the encoded data and codec type
     * @throws CodecException if encoding fails due to invalid input or overflow
     */
    public static CodecResult encodeFloat(FloatGetter values, final int length, CodecBuffer buffer) throws CodecException {
        // codec float / double
        // 1. try gorilla
        // 2. try chimp32
        // 3. store raw data
        CodecException.notAllow(values == null || length <= 0);
        final int rawCodec = Float.BYTES * length;

        try {
            GorillaCodec.encode32(values, length, buffer.forgetPos());
            if (buffer.position() < rawCodec) {
                return CodecResult.of(buffer.toArray(), CodecResult.CODEC_GORILLA);
            }
        } catch (CodecException.ValueOverflow ignore) {}

        try {
            ChimpCodec.encode32(values, length, buffer.forgetPos(), DEFAULT_CHIMP_N);
            if (buffer.position() < rawCodec) {
                return CodecResult.of(buffer.toArray(), CodecResult.CODEC_CHIMP);
            }
        } catch (CodecException.ValueOverflow ignore) {}

        buffer = CodecBuffer.newBuffer(rawCodec);
        CodecSlice slice = encodeRaw(values, length, buffer).releaseBuf();
        return CodecResult.of(slice.value(), CodecResult.CODEC_RAW);
    }

    /**
     * Decodes float values with specific codecs.
     *
     * @param slice   the CodecSlice containing the encoded data
     * @param codecs  the codec type used for encoding
     * @param stream  the FloatSetter to receive the decoded values
     * @throws CodecException if decoding fails due to invalid input or codec mismatch
     */
    public static void decodeFloat(CodecSlice slice, int codecs, FloatSetter stream) throws CodecException {
        CodecException.notAllow(slice == null || stream == null);
        if (codecs == CodecResult.CODEC_GORILLA) {
            GorillaCodec.decode32(slice, stream);
        } else if (codecs == CodecResult.CODEC_CHIMP) {
            ChimpCodec.decode32(slice, stream);
        } else {
            CodecException.notAllow(codecs != CodecResult.CODEC_RAW);
            decodeRaw(slice.value(), slice.offset(), slice.length(), stream);
        }
    }

    /**
     * Encodes double values with various codecs.
     *
     * @param values  the values to encode
     * @param length  the length of the values array
     * @param buffer  the CodecBuffer to store encoded data
     * @return a CodecResult object containing the encoded data and codec type
     * @throws CodecException if encoding fails due to invalid input or overflow
     */
    public static CodecResult encodeDouble(DoubleGetter values, final int length, CodecBuffer buffer) throws CodecException {
        CodecException.notAllow(values == null || length <= 0);
        final int rawCodec = Double.BYTES * length;

        try {
            GorillaCodec.encode64(values, length, buffer.forgetPos());
            if (buffer.position() < rawCodec) {
                return CodecResult.of(buffer.toArray(), CodecResult.CODEC_GORILLA);
            }
        } catch (CodecException.ValueOverflow ignore) {}

        try {
            ChimpCodec.encode64(values, length, buffer.forgetPos(), DEFAULT_CHIMP_N);
            if (buffer.position() < rawCodec) {
                return CodecResult.of(buffer.toArray(), CodecResult.CODEC_CHIMP);
            }
        } catch (CodecException.ValueOverflow ignore) {}

        buffer = CodecBuffer.newBuffer(rawCodec);
        CodecSlice slice = encodeRaw(values, length, buffer).releaseBuf();
        return CodecResult.of(slice.value(), CodecResult.CODEC_RAW);
    }

    /**
     * Decodes double values with specific codecs.
     *
     * @param slice   the CodecSlice containing the encoded data
     * @param codecs  the codec type used for encoding
     * @param stream  the DoubleSetter to receive the decoded values
     * @throws CodecException if decoding fails due to invalid input or codec mismatch
     */
    public static void decodeDouble(CodecSlice slice, int codecs, DoubleSetter stream) throws CodecException {
        CodecException.notAllow(slice == null || stream == null);
        if (codecs == CodecResult.CODEC_GORILLA) {
            GorillaCodec.decode64(slice, stream);
        } else if (codecs == CodecResult.CODEC_CHIMP) {
            ChimpCodec.decode64(slice, stream);
        } else {
            CodecException.notAllow(codecs != CodecResult.CODEC_RAW);
            decodeRaw(slice.value(), slice.offset(), slice.length(), stream);
        }
    }

    /**
     * Encodes integer values with various codecs.
     *
     * @param values   the values to encode
     * @param length   the length of the values array
     * @param unsigned whether the values are unsigned
     * @param buffer  the CodecBuffer to store encoded data
     * @return a CodecResult object containing the encoded data and codec type
     * @throws CodecException if encoding fails due to invalid input or overflow
     */
    public static CodecResult encodeInt(IntGetter values, final int length, boolean unsigned, CodecBuffer buffer) throws CodecException {
        // codec integer / long
        // 1. try zigzag-simple8b
        // 2. try zigzag
        // 3. store raw data
        CodecException.notAllow(values == null || length <= 0);
        final int rawCodec = Integer.BYTES * length;
        VarIntStream.Encoder32 varCodec = VarIntStream.encode32(values, unsigned);
        try {
            Simple8Codec.encode(varCodec, length, buffer.forgetPos());
            if (buffer.position() < varCodec.bytes() && buffer.position() < rawCodec) {
                return CodecResult.of(buffer.toArray(), CodecResult.CODEC_SIMPLE8 | (unsigned ? CodecResult.CODEC_VAR_INT : CodecResult.CODEC_ZIGZAG));
            }
        } catch (CodecException.ValueOverflow ignore) {}

        if (varCodec.bytes() < rawCodec) {
            VarIntCodec.encode32(values, length, unsigned, buffer.forgetPos());
            return CodecResult.of(buffer.toArray(), unsigned ? CodecResult.CODEC_VAR_INT : CodecResult.CODEC_ZIGZAG);
        }

        buffer = CodecBuffer.newBuffer(rawCodec);
        CodecSlice slice = encodeRaw(values, length, buffer).releaseBuf();
        return CodecResult.of(slice.value(), CodecResult.CODEC_RAW);
    }

    /**
     * Decodes integer values with specific codecs.
     *
     * @param slice    the CodecSlice containing the encoded data
     * @param codecs   the codec type used for encoding
     * @param stream   the IntSetter to receive the decoded values
     * @throws CodecException if decoding fails due to invalid input or codec mismatch
     */
    public static void decodeInt(CodecSlice slice, int codecs, IntSetter stream) throws CodecException {
        CodecException.notAllow(slice == null || stream == null);
        if ((codecs & CodecResult.CODEC_SIMPLE8) != 0) {
            Simple8Codec.decode(slice, VarIntStream.decode32(stream, (codecs & CodecResult.CODEC_VAR_INT) != 0));
        } else if ((codecs & (CodecResult.CODEC_VAR_INT | CodecResult.CODEC_ZIGZAG)) != 0) {
            VarIntCodec.decode32(slice, stream, (codecs & CodecResult.CODEC_VAR_INT) != 0);
        } else {
            CodecException.notAllow(codecs != CodecResult.CODEC_RAW);
            decodeRaw(slice.value(), slice.offset(), slice.length(), stream);
        }
    }

    /**
     * Encodes long values with various codecs.
     *
     * @param values   the values to encode
     * @param length   the length of the values array
     * @param unsigned whether the values are unsigned
     * @param buffer   the CodecBuffer to store encoded data
     * @return a CodecResult object containing the encoded data and codec type
     * @throws CodecException if encoding fails due to invalid input or overflow
     */
    public static CodecResult encodeLong(LongGetter values, final int length, boolean unsigned, CodecBuffer buffer) throws CodecException {
        CodecException.notAllow(values == null || length <= 0);
        final int rawCodec = Long.BYTES * length;
        VarIntStream.Encoder64 varCodec = VarIntStream.encode64(values, unsigned);
        try {
            Simple8Codec.encode(varCodec, length, buffer.forgetPos());
            if (buffer.position() < varCodec.bytes() && buffer.position() < rawCodec) {
                return CodecResult.of(buffer.toArray(), CodecResult.CODEC_SIMPLE8 | (unsigned ? CodecResult.CODEC_VAR_INT : CodecResult.CODEC_ZIGZAG));
            }
        } catch (CodecException.ValueOverflow ignore) { }

        if (varCodec.bytes() < rawCodec) {
            VarIntCodec.encode64(values, length, unsigned, buffer.forgetPos());
            return CodecResult.of(buffer.toArray(), unsigned ? CodecResult.CODEC_VAR_INT : CodecResult.CODEC_ZIGZAG);
        }

        buffer = CodecBuffer.newBuffer(rawCodec);
        CodecSlice slice = encodeRaw(values, length, buffer).releaseBuf();
        return CodecResult.of(slice.value(), CodecResult.CODEC_RAW);
    }

    /**
     * Decodes long values with specific codecs.
     *
     * @param slice    the CodecSlice containing the encoded data
     * @param codecs   the codec type used for encoding
     * @param stream   the LongSetter to receive the decoded values
     * @throws CodecException if decoding fails due to invalid input or codec mismatch
     */
    public static void decodeLong(CodecSlice slice, int codecs, LongSetter stream) throws CodecException {
        CodecException.notAllow(slice == null || stream == null);
        if ((codecs & CodecResult.CODEC_SIMPLE8) != 0) {
            Simple8Codec.decode(slice, VarIntStream.decode64(stream, (codecs & CodecResult.CODEC_VAR_INT) != 0));
        } else if ((codecs & (CodecResult.CODEC_VAR_INT | CodecResult.CODEC_ZIGZAG)) != 0) {
            VarIntCodec.decode64(slice, stream, (codecs & CodecResult.CODEC_VAR_INT) != 0);
        } else {
            CodecException.notAllow(codecs != CodecResult.CODEC_RAW);
            decodeRaw(slice.value(), slice.offset(), slice.length(), stream);
        }
    }

    /**
     * Encodes integer data into a CodecBuffer.
     *
     * @param integers the integer values to encode
     * @param length   the length of the values array
     * @param buf      the CodecBuffer to store the encoded data
     * @return the CodecBuffer containing the encoded data
     */
    public static CodecBuffer encodeRaw(IntGetter integers, final int length, CodecBuffer buf) {
        for (int i = 0; i < length; i++) {
            buf.putInt(integers.get(i));
        }
        return buf;
    }

    /**
     * Encodes long data into a CodecBuffer.
     *
     * @param longs    the long values to encode
     * @param length   the length of the values array
     * @param buf      the CodecBuffer to store the encoded data
     * @return the CodecBuffer containing the encoded data
     */
    public static CodecBuffer encodeRaw(LongGetter longs, final int length, CodecBuffer buf) {
        for (int i = 0; i < length; i++) {
            buf.putLong(longs.get(i));
        }
        return buf;
    }

    /**
     * Encodes float data into a CodecBuffer.
     *
     * @param floats   the float values to encode
     * @param length   the length of the values array
     * @param buf      the CodecBuffer to store the encoded data
     * @return the CodecBuffer containing the encoded data
     */
    public static CodecBuffer encodeRaw(FloatGetter floats, final int length, CodecBuffer buf) {
        for (int i = 0; i < length; i++) {
            buf.putInt(Float.floatToRawIntBits(floats.get(i)));
        }
        return buf;
    }

    /**
     * Encodes double data into a CodecBuffer.
     *
     * @param doubles  the double values to encode
     * @param length   the length of the values array
     * @param buf      the CodecBuffer to store the encoded data
     * @return the CodecBuffer containing the encoded data
     */
    public static CodecBuffer encodeRaw(DoubleGetter doubles, final int length, CodecBuffer buf) {
        for (int i = 0; i < length; i++) {
            buf.putLong(Double.doubleToRawLongBits(doubles.get(i)));
        }
        return buf;
    }

    /**
     * Decodes raw data into integer values.
     *
     * @param bytes     the byte array containing the raw data
     * @param offset    the offset within the byte array to start decoding from
     * @param length    the length of the raw data
     * @param integers  the IntSetter to receive the decoded values
     * @throws CodecException if decoding fails due to malformed data
     */
    public static void decodeRaw(byte[] bytes, int offset, int length, IntSetter integers) {
        CodecException.malformedData(length % 4 != 0);
        for (int i = 0; i < length; i += 4) {
            integers.set(i / 4, CodecSlice.getInt(bytes, offset+i));
        }
    }

    /**
     * Decodes raw data into long values.
     *
     * @param bytes     the byte array containing the raw data
     * @param offset    the offset within the byte array to start decoding from
     * @param length    the length of the raw data
     * @param longs     the LongSetter to receive the decoded values
     * @throws CodecException if decoding fails due to malformed data
     */
    public static void decodeRaw(byte[] bytes, int offset, int length, LongSetter longs) {
        CodecException.malformedData(length % 8 != 0);
        for (int i = 0; i < length; i += 8) {
            longs.set(i / 8, CodecSlice.getLong(bytes, offset+i));
        }
    }

    /**
     * Decodes raw data into float values.
     *
     * @param bytes     the byte array containing the raw data
     * @param offset    the offset within the byte array to start decoding from
     * @param length    the length of the raw data
     * @param floats    the FloatSetter to receive the decoded values
     * @throws CodecException if decoding fails due to malformed data
     */
    public static void decodeRaw(byte[] bytes, int offset, int length, FloatSetter floats) {
        CodecException.malformedData(length % 4 != 0);
        for (int i = 0; i < length; i += 4) {
            float value = Float.intBitsToFloat(CodecSlice.getInt(bytes, offset+i));
            floats.set(i / 4, value);
        }
    }

    /**
     * Decodes raw data into double values.
     *
     * @param bytes     the byte array containing the raw data
     * @param offset    the offset within the byte array to start decoding from
     * @param length    the length of the raw data
     * @param doubles   the DoubleSetter to receive the decoded values
     * @throws CodecException if decoding fails due to malformed data
     */
    public static void decodeRaw(byte[] bytes, int offset, int length, DoubleSetter doubles) {
        CodecException.malformedData(length % 8 != 0);
        for (int i = 0; i < length; i += 8) {
            double value = Double.longBitsToDouble(CodecSlice.getLong(bytes, offset+i));
            doubles.set(i / 8, value);
        }
    }

}
