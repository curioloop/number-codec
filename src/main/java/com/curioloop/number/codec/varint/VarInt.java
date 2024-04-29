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

/**
 * Encoding and decoding unsigned integers and longs in a variable-length format.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/22
 * @see <a href="https://wiki.vg/VarInt_And_VarLong">VarInt And VarLong</a>
 * @see <a href="https://github.com/apache/avro/blob/main/lang/java/avro/src/main/java/org/apache/avro/io/BinaryData.java">AvroBinaryData</a>
 * @see <a href="https://github.com/apache/avro/blob/main/lang/java/avro/src/main/java/org/apache/avro/io/BinaryDecoder.java">AvroBinaryDecoder</a>
 */
public class VarInt {

    /**
     * Encodes an integer into a variable-length format.
     *
     * @param n   the integer to encode
     * @param buf the byte array to store the encoded integer
     * @param pos the starting position in the byte array
     * @return the number of bytes written to the byte array
     */
    public static int encodeInt(int n, byte[] buf, int pos) {
        int start = pos;
        if ((n & ~0x7F) != 0) {
            buf[pos++] = (byte) ((n | 0x80) & 0xFF);
            n >>>= 7;
            if (n > 0x7F) {
                buf[pos++] = (byte) ((n | 0x80) & 0xFF);
                n >>>= 7;
                if (n > 0x7F) {
                    buf[pos++] = (byte) ((n | 0x80) & 0xFF);
                    n >>>= 7;
                    if (n > 0x7F) {
                        buf[pos++] = (byte) ((n | 0x80) & 0xFF);
                        n >>>= 7;
                    }
                }
            }
        }
        buf[pos++] = (byte) n;
        return pos - start;
    }

    /**
     * Encodes a long into a variable-length format.
     *
     * @param n   the long to encode
     * @param buf the byte array to store the encoded long
     * @param pos the starting position in the byte array
     * @return the number of bytes written to the byte array
     */
    public static int encodeLong(long n, byte[] buf, int pos) {
        int start = pos;
        if ((n & ~0x7FL) != 0) {
            buf[pos++] = (byte) ((n | 0x80) & 0xFF);
            n >>>= 7;
            if (n > 0x7F) {
                buf[pos++] = (byte) ((n | 0x80) & 0xFF);
                n >>>= 7;
                if (n > 0x7F) {
                    buf[pos++] = (byte) ((n | 0x80) & 0xFF);
                    n >>>= 7;
                    if (n > 0x7F) {
                        buf[pos++] = (byte) ((n | 0x80) & 0xFF);
                        n >>>= 7;
                        if (n > 0x7F) {
                            buf[pos++] = (byte) ((n | 0x80) & 0xFF);
                            n >>>= 7;
                            if (n > 0x7F) {
                                buf[pos++] = (byte) ((n | 0x80) & 0xFF);
                                n >>>= 7;
                                if (n > 0x7F) {
                                    buf[pos++] = (byte) ((n | 0x80) & 0xFF);
                                    n >>>= 7;
                                    if (n > 0x7F) {
                                        buf[pos++] = (byte) ((n | 0x80) & 0xFF);
                                        n >>>= 7;
                                        if (n > 0x7F) {
                                            buf[pos++] = (byte) ((n | 0x80) & 0xFF);
                                            n >>>= 7;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        buf[pos++] = (byte) n;
        return pos - start;
    }

    /**
     * Decodes an integer from a variable-length format.
     *
     * @param buf    the byte array containing the encoded integer
     * @param cursor an array containing the cursor position in the byte array
     * @return the decoded integer
     */
    public static int decodeInt(byte[] buf, int[] cursor) {
        int pos = cursor[0];
        int len = 1;
        int b = buf[pos] & 0xff;
        int n = b & 0x7f;
        if (b > 0x7f) {
            b = buf[pos + len++] & 0xff;
            n ^= (b & 0x7f) << 7;
            if (b > 0x7f) {
                b = buf[pos + len++] & 0xff;
                n ^= (b & 0x7f) << 14;
                if (b > 0x7f) {
                    b = buf[pos + len++] & 0xff;
                    n ^= (b & 0x7f) << 21;
                    if (b > 0x7f) {
                        b = buf[pos + len++] & 0xff;
                        n ^= (b & 0x7f) << 28;
                        CodecException.malformedData(b > 0x7f);
                    }
                }
            }
        }
        cursor[0] += len;
        return n;
    }

    /**
     * Decodes a long from a variable-length format.
     *
     * @param buf    the byte array containing the encoded long
     * @param cursor an array containing the cursor position in the byte array
     * @return the decoded long
     */
    public static long decodeLong(byte[] buf, int[] cursor) {
        int pos = cursor[0];
        int b = buf[pos++] & 0xff;
        int n = b & 0x7f;
        long l;
        if (b > 0x7f) {
            b = buf[pos++] & 0xff;
            n ^= (b & 0x7f) << 7;
            if (b > 0x7f) {
                b = buf[pos++] & 0xff;
                n ^= (b & 0x7f) << 14;
                if (b > 0x7f) {
                    b = buf[pos++] & 0xff;
                    n ^= (b & 0x7f) << 21;
                    if (b > 0x7f) {
                        // only the low 28 bits can be set, so this won't carry
                        // the sign bit to the long
                        cursor[0] = pos;
                        l = innerLongDecode(n, buf, cursor);
                        pos = cursor[0];
                    } else {
                        l = n;
                    }
                } else {
                    l = n;
                }
            } else {
                l = n;
            }
        } else {
            l = n;
        }
        cursor[0] = pos;
        return l;
    }

    // splitting readLong up makes it faster because of the JVM does more
    // optimizations on small methods
    static long innerLongDecode(long l, byte[] buf, int[] cursor) {
        int len = 1;
        int pos = cursor[0];
        int b = buf[pos] & 0xff;
        l ^= (b & 0x7fL) << 28;
        if (b > 0x7f) {
            b = buf[pos + len++] & 0xff;
            l ^= (b & 0x7fL) << 35;
            if (b > 0x7f) {
                b = buf[pos + len++] & 0xff;
                l ^= (b & 0x7fL) << 42;
                if (b > 0x7f) {
                    b = buf[pos + len++] & 0xff;
                    l ^= (b & 0x7fL) << 49;
                    if (b > 0x7f) {
                        b = buf[pos + len++] & 0xff;
                        l ^= (b & 0x7fL) << 56;
                        if (b > 0x7f) {
                            b = buf[pos + len++] & 0xff;
                            l ^= (b & 0x7fL) << 63;
                            CodecException.malformedData(b > 0x7f);
                        }
                    }
                }
            }
        }
        cursor[0] += len;
        return l;
    }
}
