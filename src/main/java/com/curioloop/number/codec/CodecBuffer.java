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

import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.ByteOrder;
import java.util.Arrays;

import static com.curioloop.number.codec.CodecSlice.BYTES_OFFSET;
import static com.curioloop.number.codec.unsafe.Unsafe.*;
import static java.nio.ByteOrder.*;

/**
 * Represents a buffer for encoding data into bytes.
 * <p>
 * <strong> Note: This class is not thread-safe and uses Unsafe operations.
 * Abnormal memory access may occur in multi-threaded situations, causing the process to crash.</strong>
 *
 * @author curioloops@gmail.com
 * @since 2024/4/23
 */
public class CodecBuffer {

    @Getter
    @Accessors(fluent = true)
    protected int position;
    protected byte[] bytes;

    /**
     * Constructs a new CodecBuffer with the specified initial capacity.
     *
     * @param initialCapacity The initial capacity of the buffer.
     */
    public CodecBuffer(int initialCapacity) {
        this.bytes = new byte[initialCapacity];
        this.position = 0;
    }

    /**
     * Puts a byte value into the buffer.
     *
     * @param value The byte value to put into the buffer.
     */
    public void putByte(byte value) {
        ensureCapacity(Byte.BYTES);
        bytes[position] = value;
        position += Byte.BYTES;
    }

    /**
     * Puts an integer value into the buffer in big-endian order.
     *
     * @param value The integer value to put into the buffer.
     */
    public void putInt(int value) {
        ensureCapacity(Integer.BYTES);
        int pos = this.position;
        bytes[pos] = (byte) (value >>> 24);
        bytes[pos+1] = (byte) (value >>> 16);
        bytes[pos+2] = (byte) (value >>> 8);
        bytes[pos+3] = (byte) (value);
        position += Integer.BYTES;
    }

    /**
     * Puts a long value into the buffer in big-endian order.
     *
     * @param value The long value to put into the buffer.
     */
    public void putLong(long value) {
        ensureCapacity(Long.BYTES);
        int pos = this.position;
        bytes[pos] = (byte) (value >>> 56);
        bytes[pos+1] = (byte) (value >>> 48);
        bytes[pos+2] = (byte) (value >>> 40);
        bytes[pos+3] = (byte) (value >>> 32);
        bytes[pos+4] = (byte) (value >>> 24);
        bytes[pos+5] = (byte) (value >>> 16);
        bytes[pos+6] = (byte) (value >>> 8);
        bytes[pos+7] = (byte) (value);
        position += Long.BYTES;
    }

    /**
     * Puts a byte array into the buffer.
     *
     * @param value The byte array to put into the buffer.
     * @param offset The start offset in the data.
     * @param length The number of bytes to write.
     */
    public void putArray(byte[] value, int offset, int length) {
        ensureCapacity(length);
        System.arraycopy(value, offset, bytes, position, length);
        position += length;
    }

    /**
     * Return actual length of underlying byte array.
     *
     * @return The length of underlying byte array.
     */
    public int capacity() {
        return bytes.length;
    }

    /**
     * Return byte array representing the buffer up to the current position.
     *
     * @return The byte array representing the buffer.
     */
    public byte[] toArray() {
        return Arrays.copyOf(bytes, position);
    }

    /**
     * Reset position to reuse buffer.
     *
     * @return Current CodecBuffer instance.
     */
    public CodecBuffer forgetPos() {
        position = 0;
        return this;
    }

    /**
     * Release the buffer and detach the underlying byte array.
     * No further access to the buffer is allowed thereafter.
     *
     * @return A slice of the buffer.
     */
    public CodecSlice releaseBuf() {
        byte[] bytes = this.bytes;
        this.bytes = null;
        return new CodecSlice().wrap(bytes, 0, position);
    }

    /**
     * The byte order this buffer prefer.
     *
     * @return The byte order type associated current buffer.
     */
    public ByteOrder byteOrder() {
        return null;
    }

    protected void ensureCapacity(int requiredCapacity) {
        int remaining = bytes.length - position;
        if (remaining < requiredCapacity) {
            int newCapacity = bytes.length * 2; // Double the capacity
            bytes = Arrays.copyOf(bytes, Math.max(newCapacity, position + requiredCapacity));
        }
    }


    /**
     * CodecBuffer implementation using unsafe operations running on big-endian platform.
     */
    static class UnsafeBE extends CodecBuffer {

        public UnsafeBE(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        public void putInt(int value) {
            ensureCapacity(Integer.BYTES);
            PUT_INT.putInt(bytes, BYTES_OFFSET + position, value);
            position += Integer.BYTES;
        }

        @Override
        public void putLong(long value) {
            ensureCapacity(Long.BYTES);
            PUT_LONG.putLong(bytes, BYTES_OFFSET + position, value);
            position += Long.BYTES;
        }

        @Override
        public ByteOrder byteOrder() {
            return BIG_ENDIAN;
        }

    }

    /**
     * CodecBuffer implementation using unsafe operations running on little-endian platform.
     */
    static class UnsafeLE extends CodecBuffer {

        public UnsafeLE(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        public void putInt(int value) {
            ensureCapacity(Integer.BYTES);
            PUT_INT.putInt(bytes, BYTES_OFFSET + position, Integer.reverseBytes(value));
            position += Integer.BYTES;
        }

        @Override
        public void putLong(long value) {
            ensureCapacity(Long.BYTES);
            PUT_LONG.putLong(bytes, BYTES_OFFSET + position, Long.reverseBytes(value));
            position += Long.BYTES;
        }

        @Override
        public ByteOrder byteOrder() {
            return LITTLE_ENDIAN;
        }
    }

    /**
     * Creates a new CodecBuffer with the specified initial capacity.
     *
     * @param initialCapacity The initial capacity of the buffer.
     * @return A new CodecBuffer instance.
     */
    public static CodecBuffer newBuffer(int initialCapacity) {
        return newBuffer(initialCapacity, true);
    }

    /**
     * Creates a new CodecBuffer with the specified initial capacity and preference for unsafe operations.
     *
     * @param initialCapacity The initial capacity of the buffer.
     * @param preferUnsafe    Indicates whether to prefer unsafe operations.
     * @return A new CodecBuffer instance.
     */
    public static CodecBuffer newBuffer(int initialCapacity, boolean preferUnsafe) {
        if (UNSAFE == null || !preferUnsafe)
            return new CodecBuffer(initialCapacity);
        return nativeOrder() == LITTLE_ENDIAN ?
                new UnsafeLE(initialCapacity) : new UnsafeBE(initialCapacity);
    }

}
