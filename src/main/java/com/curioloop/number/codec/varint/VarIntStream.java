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

import com.curioloop.number.codec.CodecException;
import com.curioloop.number.codec.stream.IntGetter;
import com.curioloop.number.codec.stream.IntSetter;
import com.curioloop.number.codec.stream.LongGetter;
import com.curioloop.number.codec.stream.LongSetter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Encoding and decoding integers and longs to variable-length format in streaming style.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/23
 */
@Accessors(fluent = true)
public class VarIntStream {

    /**
     * An encoder for small integer stream.
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public static abstract class Encoder32 implements LongGetter {

        final IntGetter integers;
        final byte[] buf = new byte[5];
        @Getter int bytes = 0;

        abstract int encodeInt(int n);

        private long encode(int value) {
            int len = encodeInt(value);
            long code = 0;
            switch (len) {
                case 5: code |= (buf[4] & 0xFFL) << 32;
                case 4: code |= (buf[3] & 0xFFL) << 24;
                case 3: code |= (buf[2] & 0xFFL) << 16;
                case 2: code |= (buf[1] & 0xFFL) << 8;
                case 1: code |= buf[0] & 0xFFL;
            }
            bytes += len;
            return code;
        }

        @Override
        public long get(int i) {
            return encode(integers.get(i));
        }

    }

    /**
     * An encoder for small long stream.
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public static abstract class Encoder64 implements LongGetter {

        final LongGetter longs;
        final byte[] buf = new byte[10];
        @Getter int bytes = 0;

        abstract int encodeLong(long n);

        private long encode(long value) {
            int len = encodeLong(value);
            CodecException.valueOverflow(len > 8);
            long code = 0;
            switch (len) {
                case 8: code |= (buf[7] & 0xFFL) << 56;
                case 7: code |= (buf[6] & 0xFFL) << 48;
                case 6: code |= (buf[5] & 0xFFL) << 40;
                case 5: code |= (buf[4] & 0xFFL) << 32;
                case 4: code |= (buf[3] & 0xFFL) << 24;
                case 3: code |= (buf[2] & 0xFFL) << 16;
                case 2: code |= (buf[1] & 0xFFL) << 8;
                case 1: code |= buf[0] & 0xFFL;
            }
            bytes += len;
            return code;
        }

        @Override
        public long get(int i) {
            return encode(longs.get(i));
        }

    }

    /**
     * A decoder for small integer stream.
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public static abstract class Decoder32 implements LongSetter {

        final IntSetter integers;
        final int[] cursor = {0};
        final byte[] buf = new byte[5];

        abstract int decodeInt();

        private int decode(long code) {
            cursor[0] = 0;
            buf[0] = (byte) (code & 0xFF);
            buf[1] = (byte) ((code >>> 8) & 0xFF);
            buf[2] = (byte) ((code >>> 16) & 0xFF);
            buf[3] = (byte) ((code >>> 24) & 0xFF);
            buf[4] = (byte) ((code >>> 32) & 0xFF);
            return decodeInt();
        }

        @Override
        public void set(int pos, long value) {
            integers.set(pos, decode(value));
        }
    }

    /**
     * A decoder for small long stream.
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public static abstract class Decoder64 implements LongSetter {

        final LongSetter longs;
        final int[] cursor = {0};
        final byte[] buf = new byte[10];

        abstract long decodeLong();

        private long decode(long code) {
            cursor[0] = 0;
            buf[0] = (byte) (code & 0xFF);
            buf[1] = (byte) ((code >>> 8) & 0xFF);
            buf[2] = (byte) ((code >>> 16) & 0xFF);
            buf[3] = (byte) ((code >>> 24) & 0xFF);
            buf[4] = (byte) ((code >>> 32) & 0xFF);
            buf[5] = (byte) ((code >>> 40) & 0xFF);
            buf[6] = (byte) ((code >>> 48) & 0xFF);
            buf[7] = (byte) ((code >>> 56) & 0xFF);
            long v = decodeLong();
            CodecException.malformedData(cursor[0] > 8);
            return v;
        }

        @Override
        public void set(int pos, long value) {
            longs.set(pos, decode(value));
        }
    }

    private static class ZigZag32Encoder32 extends Encoder32 {

        ZigZag32Encoder32(IntGetter integers) {
            super(integers);
        }

        @Override
        int encodeInt(int n) {
            return ZigZag.encodeInt(n, buf, 0);
        }
    }

    private static class ZigZag64Encoder64 extends Encoder64 {

        ZigZag64Encoder64(LongGetter longs) {
            super(longs);
        }

        @Override
        int encodeLong(long n) {
            return ZigZag.encodeLong(n, buf, 0);
        }
    }

    private static class ZigZag32Decoder32 extends Decoder32 {

        ZigZag32Decoder32(IntSetter integers) {
            super(integers);
        }

        @Override
        int decodeInt() {
            return ZigZag.decodeInt(buf, cursor);
        }
    }

    private static class ZigZag64Decoder64 extends Decoder64 {

        ZigZag64Decoder64(LongSetter longs) {
            super(longs);
        }

        @Override
        long decodeLong() {
            return ZigZag.decodeLong(buf, cursor);
        }
    }

    private static class VarInt32Encoder32 extends Encoder32 {

        VarInt32Encoder32(IntGetter integers) {
            super(integers);
        }

        @Override
        int encodeInt(int n) {
            return VarInt.encodeInt(n, buf, 0);
        }
    }

    private static class VarInt64Encoder64 extends Encoder64 {

        VarInt64Encoder64(LongGetter longs) {
            super(longs);
        }

        @Override
        int encodeLong(long n) {
            return VarInt.encodeLong(n, buf, 0);
        }
    }

    private static class VarInt32Decoder32 extends Decoder32 {

        VarInt32Decoder32(IntSetter integers) {
            super(integers);
        }

        @Override
        int decodeInt() {
            return VarInt.decodeInt(buf, cursor);
        }
    }

    private static class VarInt64Decoder64 extends Decoder64 {

        VarInt64Decoder64(LongSetter longs) {
            super(longs);
        }

        @Override
        long decodeLong() {
            return VarInt.decodeLong(buf, cursor);
        }
    }

    /**
     * Creates an encoder for small integer stream.
     *
     * @param integers the source of integers
     * @param unsigned flag indicating if the integers are unsigned
     * @return a small int encoder
     */
    public static Encoder32 encode32(IntGetter integers, boolean unsigned) {
        return unsigned ? new VarInt32Encoder32(integers) : new ZigZag32Encoder32(integers);
    }

    /**
     * Creates a decoder for small integer stream.
     *
     * @param integers the destination for integers
     * @param unsigned flag indicating if the integers are unsigned
     * @return a small int decoder
     */
    public static Decoder32 decode32(IntSetter integers, boolean unsigned) {
        return unsigned ? new VarInt32Decoder32(integers) : new ZigZag32Decoder32(integers);
    }

    /**
     * Creates an encoder for small long stream.
     *
     * @param longs    the source of longs
     * @param unsigned flag indicating if the longs are unsigned
     * @return a small long encoder
     */
    public static Encoder64 encode64(LongGetter longs, boolean unsigned) {
        return unsigned ? new VarInt64Encoder64(longs) : new ZigZag64Encoder64(longs);
    }

    /**
     * Creates a decoder for small long stream.
     *
     * @param longs    the destination for longs
     * @param unsigned flag indicating if the longs are unsigned
     * @return a small long decoder
     */
    public static Decoder64 decode64(LongSetter longs, boolean unsigned) {
        return unsigned ? new VarInt64Decoder64(longs) : new ZigZag64Decoder64(longs);
    }

}
