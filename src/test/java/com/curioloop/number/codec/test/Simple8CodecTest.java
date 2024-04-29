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
import com.curioloop.number.codec.CodecSlice;
import com.curioloop.number.codec.simple8.Simple8Codec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
public class Simple8CodecTest {

    @Test void basicTest() {
        long[] onesA = new long[120 + 10];
        Arrays.fill(onesA, 1);
        long[] onesB = new long[240 + 10];
        Arrays.fill(onesB, 1);
        long[] onesC = new long[240 + 120 + 10];
        Arrays.fill(onesC, 0);
        for (int i=0; i<120; i++) {
            onesC[240+i] = (1L << 60) - 1;
        }
        for (int i=0; i<10; i++) {
            onesA[120+i] = (i + 1) * 10L;
            onesB[240+i] = (i + 1) * 10L;
            onesC[240+120+i] = (i + 1) * 10L;
        }

        CodecBuffer buf = CodecBuffer.newBuffer(100);
        CodecSlice slice = new CodecSlice();

        byte[] simpleA = Simple8Codec.encode(i -> onesA[i], onesA.length, buf.forgetPos()).toArray();
        byte[] simpleB = Simple8Codec.encode(i -> onesB[i], onesB.length, buf.forgetPos()).toArray();
        byte[] simpleC = Simple8Codec.encode(i -> onesC[i], onesC.length, buf.forgetPos()).toArray();

        List<Long> restoreA = new ArrayList<>();
        List<Long> restoreB = new ArrayList<>();
        List<Long> restoreC = new ArrayList<>();
        Simple8Codec.decode(slice.wrap(simpleA), restoreA::add);
        Simple8Codec.decode(slice.wrap(simpleB), restoreB::add);
        Simple8Codec.decode(slice.wrap(simpleC), restoreC::add);

        System.out.println(simpleA.length);
        System.out.println(Arrays.toString(onesA));
        System.out.println(restoreA);

        Assertions.assertEquals(LongStream.of(onesA).boxed().collect(Collectors.toList()), restoreA);

        System.out.println(simpleB.length);
        System.out.println(Arrays.toString(onesB));
        System.out.println(restoreB);
        Assertions.assertEquals(LongStream.of(onesB).boxed().collect(Collectors.toList()), restoreB);

        System.out.println(simpleC.length);
        System.out.println(Arrays.toString(onesC));
        System.out.println(restoreC);
        Assertions.assertEquals(LongStream.of(onesC).boxed().collect(Collectors.toList()), restoreC);
    }

    @Test void timestampTest() {
        long[] times = {1632490437839L, 1632490437839L, 1632490437839L, 1632490437839L, 1632490437839L, 1632490437839L, 1632490437839L, 1632490437839L, 1632490437839L, 1632490437839L, 1632490437846L, 1632490437846L, 1632490437846L, 1632490437846L, 1632490437846L, 1632490437846L, 1632490437846L, 1632490437846L, 1632490437846L, 1632490437846L, 1632490437846L, 1632490437846L, 1632490437846L, 1632490437846L, 1632490437853L, 1632490437876L, 1632490437876L, 1632490437899L, 1632490437948L, 1632490437980L, 1632490437980L, 1632490438185L, 1632490438551L, 1632490438850L, 1632490439327L, 1632490439552L, 1632490440252L, 1632490440282L, 1632490440490L, 1632490440573L, 1632490440665L, 1632490440728L, 1632490440788L, 1632490440959L, 1632490441043L, 1632490441325L, 1632490441447L, 1632490441533L, 1632490441599L, 1632490441786L, 1632490441917L, 1632490442079L, 1632490442230L, 1632490442338L, 1632490442586L, 1632490442586L, 1632490442586L, 1632490442737L, 1632490442737L, 1632490443217L, 1632490443217L, 1632490443756L, 1632490443821L, 1632490444044L, 1632490444135L, 1632490444216L, 1632490444284L, 1632490444519L, 1632490444531L, 1632490444626L, 1632490444723L, 1632490445028L, 1632490445366L, 1632490445366L, 1632490445366L, 1632490445366L, 1632490445366L, 1632490445366L, 1632490445366L, 1632490445366L, 1632490445366L, 1632490445366L, 1632490445366L, 1632490445366L, 1632490445366L, 1632490445366L, 1632490445412L, 1632490445743L, 1632490446329L, 1632490446452L, 1632490446838L, 1632490446945L, 1632490447061L, 1632490447357L, 1632490447367L, 1632490447399L, 1632490447468L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447474L, 1632490447517L, 1632490447682L, 1632490447682L, 1632490447820L, 1632490447942L, 1632490448174L, 1632490448202L, 1632490448234L, 1632490448255L, 1632490448683L, 1632490448715L, 1632490448881L, 1632490449251L, 1632490449259L, 1632490449259L, 1632490449649L, 1632490449649L, 1632490449658L, 1632490449658L, 1632490449669L, 1632490449669L, 1632490449669L, 1632490449669L, 1632490449669L, 1632490449669L, 1632490449730L, 1632490449795L, 1632490449897L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450028L, 1632490450034L, 1632490450034L, 1632490450034L, 1632490450034L, 1632490450047L, 1632490450168L, 1632490450168L, 1632490450183L, 1632490450202L, 1632490450225L, 1632490450242L, 1632490450251L, 1632490450263L, 1632490450396L, 1632490450396L, 1632490450417L, 1632490450554L, 1632490450679L, 1632490450808L, 1632490451226L, 1632490451472L, 1632490451492L, 1632490451492L, 1632490451533L, 1632490451533L, 1632490451533L, 1632490451564L, 1632490451564L, 1632490451564L, 1632490451564L, 1632490451626L, 1632490451626L, 1632490451637L, 1632490452063L, 1632490452063L, 1632490452082L, 1632490452315L, 1632490452345L, 1632490452435L, 1632490452564L, 1632490452596L};
        for (int i=times.length-1; i>0; i--) {
            times[i] -= times[i-1];
        }
        List<Long> timestamp = LongStream.of(times).boxed().collect(Collectors.toList());

        CodecBuffer buf = new CodecBuffer(100);
        byte[] encode = Simple8Codec.encode(i -> times[i], times.length, buf).toArray();

        List<Long> restore = new LinkedList<>();
        Simple8Codec.decode(new CodecSlice().wrap(encode), restore::add);

        System.out.println(timestamp);
        System.out.println(restore);
        Assertions.assertEquals(timestamp, restore);
    }

}