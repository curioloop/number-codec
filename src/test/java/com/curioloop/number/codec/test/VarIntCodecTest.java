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
import com.curioloop.number.codec.CodecException;
import com.curioloop.number.codec.CodecSlice;
import com.curioloop.number.codec.varint.VarInt;
import com.curioloop.number.codec.varint.ZigZag;
import com.curioloop.number.codec.varint.VarIntCodec;
import com.curioloop.number.codec.varint.VarIntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class VarIntCodecTest {

    @Test void basicTest() {
        basicTest(true);
        basicTest(false);
    }

    void basicTest(boolean unsigned) {
        CodecBuffer buf = CodecBuffer.newBuffer(100);
        int[] value32 = new int[] { 0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
        long[] value64 = new long[] { 0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE};

        byte[] data32 = VarIntCodec.encode32(i -> value32[i], value32.length, unsigned, buf.forgetPos()).toArray();
        byte[] data64 = VarIntCodec.encode64(i -> value64[i], value64.length, unsigned, buf.forgetPos()).toArray();

        List<Integer> restore32 = new LinkedList<>();
        List<Long> restore64 = new LinkedList<>();

        CodecSlice slice = new CodecSlice();
        VarIntCodec.decode32(slice.wrap(data32), restore32::add, unsigned);
        VarIntCodec.decode64(slice.wrap(data64), restore64::add, unsigned);

        System.out.println(Arrays.toString(value32));
        System.out.println(restore32);
        Assertions.assertEquals(Arrays.toString(value32), restore32.toString());
        System.out.println(Arrays.toString(value64));
        System.out.println(restore64);
        Assertions.assertEquals(Arrays.toString(value64), restore64.toString());
    }

    @Test void streamTest() {
        Assertions.assertThrows(CodecException.ValueOverflow.class, () -> streamTest(true));
        streamTest(false);
    }

    void streamTest(boolean unsigned) {

        int[] value32 = new int[] { 0, 1, -1, 2, -2, -100000, 100000, Integer.MIN_VALUE, Integer.MAX_VALUE };
        long[] value64 = new long[] { 0, 1, -1, 2, -2, -100000, 100000, Integer.MIN_VALUE, Integer.MAX_VALUE };

        VarIntStream.Encoder32 fix32Encode = VarIntStream.encode32(i -> value32[i], unsigned);
        VarIntStream.Encoder64 fix64Encode = VarIntStream.encode64(i -> value64[i], unsigned);

        long[] zigzag32Fix = IntStream.range(0, value32.length).mapToLong(fix32Encode::get).toArray();
        long[] zigzag64Fix = IntStream.range(0, value64.length).mapToLong(fix64Encode::get).toArray();

        List<Integer> restore32 = new LinkedList<>();
        List<Long> restore64 = new LinkedList<>();

        VarIntStream.Decoder32 decoder32 = VarIntStream.decode32(restore32::add, unsigned);
        VarIntStream.Decoder64 decoder64 = VarIntStream.decode64(restore64::add, unsigned);
        IntStream.range(0, zigzag32Fix.length).forEach(i -> decoder32.set(i, zigzag32Fix[i]));
        IntStream.range(0, zigzag64Fix.length).forEach(i -> decoder64.set(i, zigzag64Fix[i]));

        System.out.println(Arrays.toString(value32));
        System.out.println(Arrays.toString(zigzag32Fix));
        Assertions.assertEquals(Arrays.toString(value32), restore32.toString());

        System.out.println(Arrays.toString(value64));
        System.out.println(Arrays.toString(zigzag64Fix));
        Assertions.assertEquals(Arrays.toString(value64), restore64.toString());
    }

    @Test void sizeDiffTest() {
        test32(IntStream.rangeClosed(0, 20).toArray(),
               IntStream.rangeClosed(-10, 10).toArray(),
               IntStream.rangeClosed(2097025, 2097025+20).toArray());
        test64(LongStream.rangeClosed(0, 20).toArray(),
               LongStream.rangeClosed(-10, 10).toArray(),
               LongStream.rangeClosed(34359738240L, 34359738240L+20).toArray());
    }

    void test32(int[] unsigned32, int[] signed32, int[] special32) {
        CodecBuffer buf = CodecBuffer.newBuffer(100);
        byte[] unsigned32VI = VarIntCodec.encode32(i -> unsigned32[i], unsigned32.length, true, buf.forgetPos()).toArray();
        byte[] unsigned32ZZ = VarIntCodec.encode32(i -> unsigned32[i], unsigned32.length, false, buf.forgetPos()).toArray();
        byte[] signed32VI = VarIntCodec.encode32(i -> signed32[i], signed32.length, true, buf.forgetPos()).toArray();
        byte[] signed32ZZ = VarIntCodec.encode32(i -> signed32[i], signed32.length, false, buf.forgetPos()).toArray();
        byte[] spec32VI = VarIntCodec.encode32(i -> special32[i], special32.length, true, buf.forgetPos()).toArray();
        byte[] spec32ZZ = VarIntCodec.encode32(i -> special32[i], special32.length, false, buf.forgetPos()).toArray();
        System.out.printf("VarInt(%d)\tZigZag32(%d)\t%s\n", unsigned32VI.length, unsigned32ZZ.length, Arrays.toString(unsigned32));
        System.out.printf("VarInt(%d)\tZigZag32(%d)\t%s\n", signed32VI.length, signed32ZZ.length, Arrays.toString(signed32));
        System.out.printf("VarInt(%d)\tZigZag32(%d)\t%s\n", spec32VI.length, spec32ZZ.length, Arrays.toString(special32));

        Assertions.assertEquals(unsigned32VI.length, unsigned32ZZ.length);
        Assertions.assertTrue(signed32VI.length > signed32ZZ.length);
        Assertions.assertTrue(spec32VI.length < spec32ZZ.length);

        CodecSlice slice = new CodecSlice();
        List<Integer> u32vi = new ArrayList<>(), u32zz = new ArrayList<>(), si32vi = new ArrayList<>(), si32zz = new ArrayList<>(), sp32vi = new ArrayList<>(), sp32zz = new ArrayList<>();
        VarIntCodec.decode32(slice.wrap(unsigned32VI), u32vi::add, true);
        VarIntCodec.decode32(slice.wrap(unsigned32ZZ), u32zz::add, false);
        VarIntCodec.decode32(slice.wrap(signed32VI), si32vi::add, true);
        VarIntCodec.decode32(slice.wrap(signed32ZZ), si32zz::add, false);
        VarIntCodec.decode32(slice.wrap(spec32VI), sp32vi::add, true);
        VarIntCodec.decode32(slice.wrap(spec32ZZ), sp32zz::add, false);

        Assertions.assertEquals(Arrays.toString(unsigned32), u32vi.toString());
        Assertions.assertEquals(Arrays.toString(unsigned32), u32zz.toString());
        Assertions.assertEquals(Arrays.toString(signed32), si32vi.toString());
        Assertions.assertEquals(Arrays.toString(signed32), si32zz.toString());
        Assertions.assertEquals(Arrays.toString(special32), sp32vi.toString());
        Assertions.assertEquals(Arrays.toString(special32), sp32zz.toString());
    }

    void test64(long[] unsigned64, long[] signed64, long[] special64) {
        CodecBuffer buf = CodecBuffer.newBuffer(100);
        byte[] unsigned64VI = VarIntCodec.encode64(i -> unsigned64[i], unsigned64.length, true, buf.forgetPos()).toArray();
        byte[] unsigned64ZZ = VarIntCodec.encode64(i -> unsigned64[i], unsigned64.length, false, buf.forgetPos()).toArray();
        byte[] signed64VI = VarIntCodec.encode64(i -> signed64[i], signed64.length, true, buf.forgetPos()).toArray();
        byte[] signed64ZZ = VarIntCodec.encode64(i -> signed64[i], signed64.length, false, buf.forgetPos()).toArray();
        byte[] spec64VI = VarIntCodec.encode64(i -> special64[i], special64.length, true, buf.forgetPos()).toArray();
        byte[] spec64ZZ = VarIntCodec.encode64(i -> special64[i], special64.length, false, buf.forgetPos()).toArray();
        System.out.printf("VarInt(%d)\tZigZag32(%d)\t%s\n", unsigned64VI.length, unsigned64ZZ.length, Arrays.toString(unsigned64));
        System.out.printf("VarInt(%d)\tZigZag32(%d)\t%s\n", signed64VI.length, signed64ZZ.length, Arrays.toString(signed64));
        System.out.printf("VarInt(%d)\tZigZag32(%d)\t%s\n", spec64VI.length, spec64ZZ.length, Arrays.toString(special64));

        Assertions.assertEquals(unsigned64VI.length, unsigned64ZZ.length);
        Assertions.assertTrue(signed64VI.length > signed64ZZ.length);
        Assertions.assertTrue(spec64VI.length < spec64ZZ.length);

        CodecSlice slice = new CodecSlice();
        List<Long> u64vi = new ArrayList<>(), u64zz = new ArrayList<>(), si64vi = new ArrayList<>(), si64zz = new ArrayList<>(), sp64vi = new ArrayList<>(), sp64zz = new ArrayList<>();
        VarIntCodec.decode64(slice.wrap(unsigned64VI), u64vi::add, true);
        VarIntCodec.decode64(slice.wrap(unsigned64ZZ), u64zz::add, false);
        VarIntCodec.decode64(slice.wrap(signed64VI), si64vi::add, true);
        VarIntCodec.decode64(slice.wrap(signed64ZZ), si64zz::add, false);
        VarIntCodec.decode64(slice.wrap(spec64VI), sp64vi::add, true);
        VarIntCodec.decode64(slice.wrap(spec64ZZ), sp64zz::add, false);

        Assertions.assertEquals(Arrays.toString(unsigned64), u64vi.toString());
        Assertions.assertEquals(Arrays.toString(unsigned64), u64zz.toString());
        Assertions.assertEquals(Arrays.toString(signed64), si64vi.toString());
        Assertions.assertEquals(Arrays.toString(signed64), si64zz.toString());
        Assertions.assertEquals(Arrays.toString(special64), sp64vi.toString());
        Assertions.assertEquals(Arrays.toString(special64), sp64zz.toString());
    }

    @Test void singleValTest() {
        byte[] buf32 = new byte[5];
        for (int n = 0; n < 10000000; n++) {
            int rand = ThreadLocalRandom.current().nextInt();
            int bytes = ZigZag.encodeInt(rand, buf32, 0);
            int[] cursor = {0};
            int result = ZigZag.decodeInt(buf32, cursor);
            Assertions.assertEquals(rand, result);
            Assertions.assertEquals(bytes, cursor[0]);
        }
        for (int n = 0; n < 10000000; n++) {
            int rand = ThreadLocalRandom.current().nextInt();
            int bytes = VarInt.encodeInt(rand, buf32, 0);
            int[] cursor = {0};
            int result = VarInt.decodeInt(buf32, cursor);
            Assertions.assertEquals(rand, result);
            Assertions.assertEquals(bytes, cursor[0]);
        }

        byte[] buf64 = new byte[10];
        for (int n = 0; n < 10000000; n++) {
            long rand = ThreadLocalRandom.current().nextLong();
            int bytes = ZigZag.encodeLong(rand, buf64, 0);
            int[] cursor = {0};
            long result = ZigZag.decodeLong(buf64, cursor);
            Assertions.assertEquals(rand, result);
            Assertions.assertEquals(bytes, cursor[0]);
        }
        for (int n = 0; n < 10000000; n++) {
            long rand = ThreadLocalRandom.current().nextLong();
            int bytes = VarInt.encodeLong(rand, buf64, 0);
            int[] cursor = {0};
            long result = VarInt.decodeLong(buf64, cursor);
            Assertions.assertEquals(rand, result);
            Assertions.assertEquals(bytes, cursor[0]);
        }
    }

}