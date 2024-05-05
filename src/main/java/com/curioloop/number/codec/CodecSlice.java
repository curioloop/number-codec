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

import com.curioloop.number.codec.unsafe.Unsafe;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.ByteOrder.nativeOrder;

/**
 * A slice of bytes used for reducing array copy.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/24
 */
@Getter
@Accessors(fluent = true)
public class CodecSlice {

    private byte[] value;
    private int offset, length;

    public CodecSlice wrap(byte[] bytes) {
        return wrap(bytes, 0, bytes.length);
    }

    public CodecSlice wrap(byte[] value, int offset, int length) {
        CodecException.notAllow((offset | length) < 0 || offset + length > value.length);
        this.value = value;
        this.offset = offset;
        this.length = length;
        return this;
    }

    private interface GetIntFromBytes {
        int getInt(byte[] bytes, int position);
    }

    private interface GetLongFromBytes {
        long getLong(byte[] bytes, int position);
    }

    public static final int BYTES_OFFSET =
            Unsafe.ARRAY_OFFSET == null ? 0 :
            Unsafe.ARRAY_OFFSET.arrayBaseOffset(byte[].class);

    private static final GetIntFromBytes GET_INT_FROM_BYTES =
            Unsafe.GET_INT == null ? CodecSlice::getIntSafe : nativeOrder() == LITTLE_ENDIAN ?
                    CodecSlice::getIntUnsafeLE:
                    CodecSlice::getIntUnsafeBE;

    private static final GetLongFromBytes GET_LONG_FROM_BYTES =
            Unsafe.GET_LONG == null ? CodecSlice::getLongSafe : nativeOrder() == LITTLE_ENDIAN ?
                    CodecSlice::getLongUnsafeLE:
                    CodecSlice::getLongUnsafeBE;

    /**
     * Load an integer from the byte array in big-endian order.
     *
     * @param bytes    the byte array
     * @param position the position from which to retrieve the integer
     * @return the integer value
     */
    private static int getIntSafe(byte[] bytes, int position) {
        return ((bytes[position] & 0xFF) << 24) |
                ((bytes[position+1] & 0xFF) << 16) |
                ((bytes[position+2] & 0xFF) << 8) |
                (bytes[position+3] & 0xFF);
    }

    /**
     * Long a long from the byte array in big-endian order.
     *
     * @param bytes    the byte array
     * @param position the position from which to retrieve the long
     * @return the long value
     */
    private static long getLongSafe(byte[] bytes, int position) {
        return ((long)(bytes[position] & 0xFF) << 56) |
                ((long)(bytes[position+1] & 0xFF) << 48) |
                ((long)(bytes[position+2] & 0xFF) << 40) |
                ((long)(bytes[position+3] & 0xFF) << 32) |
                ((long)(bytes[position+4] & 0xFF) << 24) |
                ((long)(bytes[position+5] & 0xFF) << 16) |
                ((long)(bytes[position+6] & 0xFF) << 8) |
                ((long)(bytes[position+7] & 0xFF));
    }

    /**
     * Unsafely load an integer from the byte array in big-endian order on little-endian platform.
     *
     * @param bytes    the byte array
     * @param position the start position of loading
     * @return the integer value
     */
    private static int getIntUnsafeLE(byte[] bytes, int position) {
        return Integer.reverseBytes(Unsafe.GET_INT.getInt(bytes, BYTES_OFFSET + position));
    }

    /**
     * Unsafely load an integer from the byte array in big-endian order on big-endian platform.
     *
     * @param bytes    the byte array
     * @param position the start position of loading
     * @return the integer value
     */
    private static int getIntUnsafeBE(byte[] bytes, int position) {
        return Unsafe.GET_INT.getInt(bytes, BYTES_OFFSET + position);
    }

    /**
     * Unsafely load a long from the byte array in big-endian order on little-endian platform.
     *
     * @param bytes    the byte array
     * @param position the start position of loading
     * @return the long value
     */
    private static long getLongUnsafeLE(byte[] bytes, int position) {
        return Long.reverseBytes(Unsafe.GET_LONG.getLong(bytes, BYTES_OFFSET + position));
    }

    /**
     * Unsafely load a long from the byte array in big-endian order on big-endian platform.
     *
     * @param bytes    the byte array
     * @param position the start position of loading
     * @return the long value
     */
    private static long getLongUnsafeBE(byte[] bytes, int position) {
        return Unsafe.GET_LONG.getLong(bytes, BYTES_OFFSET + position);
    }

    /**
     * Load an integer from the specified position in the byte array.
     *
     * @param bytes    the byte array
     * @param position the start position of loading
     * @return the integer value
     */
    public static int getInt(byte[] bytes, int position) {
        return GET_INT_FROM_BYTES.getInt(bytes, position);
    }

    /**
     * Load a long from the specified position in the byte array.
     *
     * @param bytes    the byte array
     * @param position the start position of loading
     * @return the long value
     */
    public static long getLong(byte[] bytes, int position) {
        return GET_LONG_FROM_BYTES.getLong(bytes, position);
    }

    /**
     * Return byte array representing the bytes up to the current position.
     *
     * @return a complete copy of the current slice
     */
    public byte[] toArray() {
        return Arrays.copyOf(value, length);
    }

}
