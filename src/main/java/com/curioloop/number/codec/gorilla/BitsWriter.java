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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * BitsWriter is a utility class for writing bits to a byte array.
 * It extends CodecBuffer to provide additional methods for writing individual bits or groups of bits.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
@RequiredArgsConstructor
public class BitsWriter {

    @Getter
    @Accessors(fluent = true)
    private int totalBits;
    private int bufBits;
    private long buffer;
    private final CodecBuffer codecBuf;

    private void flushBuf() {
        CodecException.notAllow(bufBits < 0);
        int bufBytes = bufBits / 8 + Integer.signum(bufBits % 8);
        if (bufBytes == Long.BYTES) {
            codecBuf.putLong(buffer);
        } else {
            for (int i=0; i<bufBytes; i++) {
                int shift = (7 - i) * 8;
                codecBuf.putByte((byte) ((buffer >>> shift) & 0xFF));
            }
        }
        bufBits = 0;
        buffer = 0;
    }

    private int currentPos() {
        return (64 - bufBits) - 1;
    }

    /**
     * Writes a single bit to the buffer.
     *
     * @param bit The bit value to write (true for 1, false for 0).
     */
    public void writeBit(boolean bit) {
        if (bit) {
            buffer |= (1L << currentPos());
        }
        bufBits++;
        totalBits++;
        if (currentPos() < 0) {
            flushBuf();
        }
    }

    /**
     * Writes a specified number of bits from a long value to the buffer.
     *
     * @param bits The long value containing the bits to write.
     * @param num  The number of bits to write.
     * @throws CodecException if num is invalid.
     */
    public void writeBits(long bits, int num) {
        CodecException.valueOverflow(num <= 0 || num > 64);
        int capacity = currentPos() + 1;
        if (capacity < num) {
            writeBits(bits >>> (num - capacity), capacity);
            writeBits(bits, num - capacity);
        } else {
            long mask = ~0L >>> (64 - num);
            buffer |= (bits & mask) << (capacity - num);
            bufBits += num;
            totalBits += num;
            if (currentPos() < 0) {
                flushBuf();
            }
        }
    }

    /**
     * Flushes any remaining bits in the buffer and returns the resulting byte array.
     */
    public void flushBits() {
        flushBuf();
        bufBits = -1; // only once
        codecBuf.putByte((byte) ((8 - (totalBits % 8)) % 8)); // record padding bits
    }
}
