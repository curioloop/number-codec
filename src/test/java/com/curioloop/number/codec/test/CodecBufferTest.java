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
package com.curioloop.number.codec.test;

import com.curioloop.number.codec.CodecBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CodecBufferTest {

    @Test void testPutByte() {
        CodecBuffer buffer = CodecBuffer.newBuffer(1);
        buffer.putByte((byte) 42);
        buffer.putByte((byte) 76);
        buffer.putByte((byte) 95);
        byte[] result = buffer.toArray();
        assertEquals(3, result.length);
        assertEquals(42, result[0]);
        assertEquals(76, result[1]);
        assertEquals(95, result[2]);
    }

    @Test void testPutInt() {
        CodecBuffer buf1 = CodecBuffer.newBuffer(2, false);
        CodecBuffer buf2 = CodecBuffer.newBuffer(2, true);
        assertNull(buf1.byteOrder());
        assertNotNull(buf2.byteOrder());

        buf1.putInt(123456789);
        buf2.putInt(123456789);

        byte[] result1 = buf1.toArray();
        byte[] result2 = buf2.toArray();
        assertArrayEquals(result1, result2);

        assertEquals(4, result1.length);
        assertEquals(7, result1[0]);
        assertEquals((byte) 91, result1[1]);
        assertEquals((byte) 205, result1[2]);
        assertEquals((byte) 21, result1[3]);
    }

    @Test void testPutLong() {
        CodecBuffer buf1 = CodecBuffer.newBuffer(2, false);
        CodecBuffer buf2 = CodecBuffer.newBuffer(2, true);
        assertNull(buf1.byteOrder());
        assertNotNull(buf2.byteOrder());

        buf1.putLong(123456789012345678L);
        buf2.putLong(123456789012345678L);

        byte[] result1 = buf1.toArray();
        byte[] result2 = buf2.toArray();
        assertArrayEquals(result1, result2);

        assertEquals(8, result1.length);
        assertEquals(1, result1[0]);
        assertEquals((byte) 182, result1[1]);
        assertEquals((byte) 155, result1[2]);
        assertEquals((byte) 75, result1[3]);
        assertEquals((byte) 166, result1[4]);
        assertEquals((byte) 48, result1[5]);
        assertEquals((byte) 243, result1[6]);
        assertEquals((byte) 78, result1[7]);
    }

    @Test void testPutArray() {
        byte[] value = {0,1,2,3,4,5,6,7,8,9};
        CodecBuffer buf = CodecBuffer.newBuffer(2);
        int expectedLen = 0;
        for (int i=0; i<=value.length; i++) {
            int length = value.length - i;
            buf.putArray(value, i, length);
            expectedLen += length;
            assertEquals(expectedLen, buf.toArray().length);
        }
    }

    @Test void testGrowing() {
        CodecBuffer buf1 = CodecBuffer.newBuffer(0);
        CodecBuffer buf2 = CodecBuffer.newBuffer(0);
        CodecBuffer buf3 = CodecBuffer.newBuffer(0);
        for (int i = 1; i <= 300; i++) {
            buf1.putByte((byte) i);
            buf2.putInt(i);
            buf3.putLong(i);
            assertEquals(i, buf1.toArray().length);
            assertEquals(i*Integer.BYTES, buf2.toArray().length);
            assertEquals(i*Long.BYTES, buf3.toArray().length);
        }
    }

    @Test void testReleased() {
        CodecBuffer buf = CodecBuffer.newBuffer(0, true);
        assertNotNull(buf.byteOrder());
        assertEquals(0, buf.releaseBuf().length());

        // the JVM won`t crash event the buffer is null
        assertThrows(NullPointerException.class, () -> buf.putInt(0));
        assertThrows(NullPointerException.class, () -> buf.putLong(0));
    }

}
