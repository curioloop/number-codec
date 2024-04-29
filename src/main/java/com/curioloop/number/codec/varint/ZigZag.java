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

/**
 * ZigZag encoding is a simple technique to efficiently encode signed integers into a variable-length format
 * by moving the sign bit to the least significant bit, and flipping all other bits if the number is negative.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/24
 * @see <a href="https://wiki.vg/VarInt_And_VarLong">VarInt And VarLong</a>
 */
public class ZigZag {

    /**
     * Encodes an integer using ZigZag encoding and writes it into a byte array.
     *
     * @param n   the integer to encode
     * @param buf the byte array to store the encoded integer
     * @param pos the starting position in the byte array
     * @return the number of bytes written to the byte array
     */
    public static int encodeInt(int n, byte[] buf, int pos) {
        // move sign to low-order bit, and flip others if negative
        n = (n << 1) ^ (n >> 31);
        return VarInt.encodeInt(n, buf, pos);
    }

    /**
     * Encodes a long using ZigZag encoding and writes it into a byte array.
     *
     * @param n   the long to encode
     * @param buf the byte array to store the encoded long
     * @param pos the starting position in the byte array
     * @return the number of bytes written to the byte array
     */
    public static int encodeLong(long n, byte[] buf, int pos) {
        // move sign to low-order bit, and flip others if negative
        n = (n << 1) ^ (n >> 63);
        return VarInt.encodeLong(n, buf, pos);
    }

    /**
     * Decodes an integer from a byte array encoded using ZigZag encoding.
     *
     * @param buf    the byte array containing the encoded integer
     * @param cursor an array containing the cursor position in the byte array
     * @return the decoded integer
     */
    public static int decodeInt(byte[] buf, int[] cursor) {
        int n = VarInt.decodeInt(buf, cursor);
        return (n >>> 1) ^ -(n & 1); // back to two's-complement
    }

    /**
     * Decodes a long from a byte array encoded using ZigZag encoding.
     *
     * @param buf    the byte array containing the encoded long
     * @param cursor an array containing the cursor position in the byte array
     * @return the decoded long
     */
    public static long decodeLong(byte[] buf, int[] cursor) {
        long l = VarInt.decodeLong(buf, cursor);
        return (l >>> 1) ^ -(l & 1); // back to two's-complement
    }

}
