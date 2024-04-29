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
package com.curioloop.number.codec.simple8;

import com.curioloop.number.codec.stream.LongGetter;
import com.curioloop.number.codec.CodecException;

import java.util.Arrays;

/**
 *
 * A class for fast lookup of the appropriate Packing object during encoding.
 * <p>
 * This class reduces the encoding complexity to O(n) using table lookup,
 * whereas the original backtracking algorithm had a complexity of O(n²).
 *
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
final class FastLookup {

    // pointer to packing object
    final byte[] stateSpace = new byte[261];
    {
        // 1 - 60 integers need to be compacted
        int beg = 0, end = 60;
        for (int i = 1; i <= 60; i++, beg = end) {
            end += i == 1 ? 0 : (60 / i);
            // pack is ready if significant bits in [n, end)
            for (int n = beg + 60 / (i + 1); n < end; n++) {
                stateSpace[n] = (byte) i;
            }
            // if significant bits in [0, n) search forward
            // if significant bits in (end, ∞) search backward
        }
    }

    static int significantBits(long i) {
        return i == 0 ? 1 : (64 - Long.numberOfLeadingZeros(i));
    }

    /**
     * Looks up the appropriate Packing object based on the input values.
     *
     * @param src   The array of long values.
     * @param pos   The starting index in the array.
     * @param len   The number of values to encode.
     * @return The appropriate Packing object, or null if none is found.
     */
    Packing lookupPacking(LongGetter src, int pos, int len) {
        CodecException.notAllow(pos >= len);
        final int remain = len - pos;
        final int num = Math.min(remain, 60);

        int pruning = 0;
        long indicator = src.get(pos); // check if all values are the same
        int beg = 0, end = 60, match = 1;
        for (int i = 1; i <= num; i++, beg = end) {
            end += i == 1 ? 0 : (60 / i);
            long value = i == 1 ? indicator: src.get(pos + i - 1);
            if (value < 0 || value >= (1L << 60)) {
                return null; // value overflow
            } else if (indicator != value) {
                indicator = -1; // found other value
            }
            // prune the search space
            // e.g: if the significant bit of first number is 7
            //      which mean each int should occupy at least (60 / 7 = 8) bits
            //      so we could only consider the Packing1/2/3/4/5/6/7
            //      and ignore the impossible Packing8/10/12/15/20 ...
            int n = pruning = Math.max(pruning, significantBits(value) - 1);
            if (beg + n >= end) { // significantBits in (end, ∞)
                return getPacking(match);
            }
            if (i < 60 && stateSpace[beg + n] > 0) { // significantBits in [n, end)
                return getPacking(stateSpace[beg + n]);
            }
            if (stateSpace[end - 1] > 0) { // significantBits in [n, end)
                match = stateSpace[end - 1];
            }
        }

        if (num < 60 || remain < 120 || indicator == -1) {
            return getPacking(match);
        }

        // unique value, try apply Packing120/240
        int i, j = Math.min(remain, 240);
        for (i = 60; i < j; i++) {
            if (src.get(pos + i) != indicator) {
                break;
            }
        }
        if (i == 240) return getPacking(240);
        if (i >= 120) return getPacking(120);
        return getPacking(match);
    }

    /**
     * Gets the appropriate Packing object based on the number of integers to pack.
     *
     * @param numOfInt The number of integers to pack.
     * @return The corresponding Packing object.
     * @throws IllegalArgumentException If the number of integers is invalid.
     */
    static Packing getPacking(int numOfInt) {
        if (numOfInt >= 1 && numOfInt <= 8) {
            return Simple8Codec.selector[16-numOfInt];
        }
        switch (numOfInt) {
            case 10: return Simple8Codec.selector[7];
            case 12: return Simple8Codec.selector[6];
            case 15: return Simple8Codec.selector[5];
            case 20: return Simple8Codec.selector[4];
            case 30: return Simple8Codec.selector[3];
            case 60: return Simple8Codec.selector[2];
            case 120: return Simple8Codec.selector[1];
            case 240: return Simple8Codec.selector[0];
        }
        throw new IllegalArgumentException(String.valueOf(numOfInt));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int beg = 0, end = 60;
        for (int i = 1; i <= 60; i++, beg = end) {
            end += i == 1 ? 0 : (60 / i);
            sb.append(String.format("%02d(%03d-%03d)", i, beg, end));
            sb.append(Arrays.toString(Arrays.copyOfRange(stateSpace, beg, end)));
            if (i != 60) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}

