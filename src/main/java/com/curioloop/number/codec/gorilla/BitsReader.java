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

import com.curioloop.number.codec.CodecException;

/**
 * BitsReader is a utility class for reading bits from a byte array.
 * It allows sequential reading of individual bits or groups of bits from the provided byte array.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
public class BitsReader {

    private final byte[] data;
    private final int totalBits;
    private int bitCursor; // point to first unread bit

    /**
     * Constructs a BitsReader instance with the provided byte array.
     * The entire byte array will be used for reading bits.
     *
     * @param data The byte array from which to read bits.
     */
    public BitsReader(byte[] data) {
        this(data, 0, data.length);
    }

    /**
     * Constructs a BitsReader instance with the provided byte array, start index, and length.
     *
     * @param data   The byte array from which to read bits.
     * @param start  The starting index (in bytes) from which to begin reading.
     * @param length The length (in bytes) of the data to read.
     */
    public BitsReader(byte[] data, int start, int length) {
        this.data = data;
        this.bitCursor = start * 8;
        this.totalBits = bitCursor + ((length - 1) * 8) - data[start + length - 1];
    }

    /**
     * Checks if there are more unread bits in the byte array.
     *
     * @return true if there are more unread bits, false otherwise.
     */
    public boolean hasMore() {
        return bitCursor < totalBits;
    }

    private int nextBits(int numOfBits) {
        byte chunk = data[bitCursor / 8];
        int rest = 8 - bitCursor % 8;
        CodecException.malformedData(rest < numOfBits);
        int mask = 0xFF >>> (8 - numOfBits);
        int offset = rest - numOfBits;
        bitCursor += numOfBits;
        return (chunk >>> offset) & mask;
    }

    /**
     * Reads the next bit from the byte array.
     *
     * @return The value of the next bit (true for 1, false for 0).
     * @throws CodecException if there are no more bits to read.
     */
    boolean readBit() {
        CodecException.malformedData(!hasMore());
        int nextPos = bitCursor++;
        byte bits = data[nextPos / 8];
        return (bits >>> (7 - (nextPos % 8)) & 0x1) > 0;
    }

    /**
     * Reads a specified number of bits from the byte array.
     *
     * @param numOfBits The number of bits to read.
     * @return The value of the read bits as a long integer.
     * @throws CodecException if numOfBits is invalid or if there are no more bits to read.
     */
    public long readBits(int numOfBits) {
        CodecException.malformedData(numOfBits <= 0 || numOfBits > 64 || bitCursor + numOfBits > totalBits);
        int rest = 8 - bitCursor % 8;
        if (numOfBits <= rest) {
            return nextBits(numOfBits);
        }
        long bits = nextBits(rest);
        numOfBits -= rest;
        while (numOfBits > 0) {
            int n = Math.min(numOfBits, 8);
            bits <<= n;
            bits |= nextBits(n);
            numOfBits -= n;
        }
        return bits;
    }
}

