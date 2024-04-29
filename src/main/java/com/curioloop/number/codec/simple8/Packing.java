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
import com.curioloop.number.codec.stream.LongSetter;

/**
 * An abstract class representing a packing scheme for encoding and decoding long values.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
abstract class Packing {

    final int integersCoded;
    final int bitsPerInteger;

    /**
     * Constructs a Packing object with the specified parameters.
     *
     * @param integersCoded   The number of integers to be packed.
     * @param bitsPerInteger The number of bits per integer.
     */
    Packing(int integersCoded, int bitsPerInteger) {
        this.integersCoded = integersCoded;
        this.bitsPerInteger = bitsPerInteger;
    }

    /**
     * Packs a portion of the source array into a long value.
     *
     * @param src The source array of long values.
     * @param pos The starting index in the source array.
     * @return The packed long value.
     */
    long pack(LongGetter src, int pos) {
        long pack = ((long) bitsPerInteger + 1) << 60;
        for (int i=0,j=0; i<integersCoded; i++,j+=bitsPerInteger) {
            pack |= src.get(pos + i) << j;
        }
        return pack;
    }

    /**
     * Unpacks a long value into the destination LongConsumer.
     *
     * @param pack The packed long value.
     * @param dst  The LongConsumer to consume the unpacked values.
     */
    void unpack(long pack, LongSetter dst, int pos) {
        long mask = ~0L >>> (64 - bitsPerInteger);
        for (int i=0,j=0; i<integersCoded; i++,j+=bitsPerInteger) {
            dst.set(pos+i, (pack >>> j) & mask);
        }
    }

}

/**
 * A Packing implementation for packing 240 integers.
 */
final class Packing240 extends Packing {

    Packing240() { super(240, 0); }

    long pack(LongGetter src, int pos) {
        return src.get(pos);
    }

    void unpack(long pack, LongSetter dst, int pos) {
        for (int i=0; i++<240; dst.set(pos++, pack & 1));
    }
}

final class Packing120 extends Packing {

    Packing120() { super(120, 0); }

    long pack(LongGetter src, int pos) {
        return (1L << 60) | src.get(pos);
    }

    void unpack(long pack, LongSetter dst, int pos) {
        for (int i=0; i++<120; dst.set(pos++, pack & 1));
    }
}

final class Packing60 extends Packing {
    Packing60() { super(60, 1); }
}

final class Packing30 extends Packing {
    Packing30() { super(30, 2); }
}

final class Packing20 extends Packing {
    Packing20() { super(20, 3); }
}

final class Packing15 extends Packing {
    Packing15() { super(15, 4); }
}

final class Packing12 extends Packing {

    Packing12() { super(12, 5); }

    long pack(LongGetter src, int pos) {
        long pack = 6L << 60;
        pack |= src.get(pos) | src.get(pos+1)<<5 |
                src.get(pos+2)<<10 | src.get(pos+3)<<15 |
                src.get(pos+4)<<20 | src.get(pos+5)<<25 |
                src.get(pos+6)<<30 | src.get(pos+7)<<35 |
                src.get(pos+8)<<40 | src.get(pos+9)<<45 |
                src.get(pos+10)<<50 | src.get(pos+11)<<55;
        return pack;
    }

    void unpack(long pack, LongSetter dst, int pos) {
        dst.set(pos, pack & 31);
        dst.set(pos+1, (pack >>> 5) & 31);
        dst.set(pos+2, (pack >>> 10) & 31);
        dst.set(pos+3, (pack >>> 15) & 31);
        dst.set(pos+4, (pack >>> 20) & 31);
        dst.set(pos+5, (pack >>> 25) & 31);
        dst.set(pos+6, (pack >>> 30) & 31);
        dst.set(pos+7, (pack >>> 35) & 31);
        dst.set(pos+8, (pack >>> 40) & 31);
        dst.set(pos+9, (pack >>> 45) & 31);
        dst.set(pos+10, (pack >>> 50) & 31);
        dst.set(pos+11, (pack >>> 55) & 31);
    }
}

final class Packing10 extends Packing {

    Packing10() { super(10, 6); }

    long pack(LongGetter src, int pos) {
        long pack = 7L << 60;
        pack |= src.get(pos) | src.get(pos+1)<<6 |
                src.get(pos+2)<<12 | src.get(pos+3)<<18 |
                src.get(pos+4)<<24 | src.get(pos+5)<<30 |
                src.get(pos+6)<<36 | src.get(pos+7)<<42 |
                src.get(pos+8)<<48 | src.get(pos+9)<<54;
        return pack;
    }

    void unpack(long pack, LongSetter dst, int pos) {
        dst.set(pos, pack & 63);
        dst.set(pos+1, (pack >>> 6) & 63);
        dst.set(pos+2, (pack >>> 12) & 63);
        dst.set(pos+3, (pack >>> 18) & 63);
        dst.set(pos+4, (pack >>> 24) & 63);
        dst.set(pos+5, (pack >>> 30) & 63);
        dst.set(pos+6, (pack >>> 36) & 63);
        dst.set(pos+7, (pack >>> 42) & 63);
        dst.set(pos+8, (pack >>> 48) & 63);
        dst.set(pos+9, (pack >>> 54) & 63);
    }
}

final class Packing8 extends Packing {

    Packing8() { super(8, 7); }

    long pack(LongGetter src, int pos) {
        long pack = 8L << 60;
        pack |= src.get(pos) | src.get(pos+1)<<7 |
                src.get(pos+2)<<14 | src.get(pos+3)<<21 |
                src.get(pos+4)<<28 | src.get(pos+5)<<35 |
                src.get(pos+6)<<42 | src.get(pos+7)<<49;
        return pack;
    }

    void unpack(long pack, LongSetter dst, int pos) {
        dst.set(pos, pack & 127);
        dst.set(pos+1, (pack >>> 7) & 127);
        dst.set(pos+2, (pack >>> 14) & 127);
        dst.set(pos+3, (pack >>> 21) & 127);
        dst.set(pos+4, (pack >>> 28) & 127);
        dst.set(pos+5, (pack >>> 35) & 127);
        dst.set(pos+6, (pack >>> 42) & 127);
        dst.set(pos+7, (pack >>> 49) & 127);
    }
}

final class Packing7 extends Packing {

    Packing7() { super(7, 8); }

    long pack(LongGetter src, int pos) {
        long pack = 9L << 60;
        pack |= src.get(pos) | src.get(pos+1)<<8 |
                src.get(pos+2)<<16 | src.get(pos+3)<<24 |
                src.get(pos+4)<<32 | src.get(pos+5)<<40 |
                src.get(pos+6)<<48;
        return pack;
    }

    void unpack(long pack, LongSetter dst, int pos) {
        dst.set(pos, pack & 255);
        dst.set(pos+1, (pack >>> 8) & 255);
        dst.set(pos+2, (pack >>> 16) & 255);
        dst.set(pos+3, (pack >>> 24) & 255);
        dst.set(pos+4, (pack >>> 32) & 255);
        dst.set(pos+5, (pack >>> 40) & 255);
        dst.set(pos+6, (pack >>> 48) & 255);
    }
}

final class Packing6 extends Packing {

    Packing6() { super(6, 10); }

    long pack(LongGetter src, int pos) {
        long pack = 10L << 60;
        pack |= src.get(pos) | src.get(pos+1)<<10 |
                src.get(pos+2)<<20 | src.get(pos+3)<<30 |
                src.get(pos+4)<<40 | src.get(pos+5)<<50;
        return pack;
    }

    void unpack(long pack, LongSetter dst, int pos) {
        dst.set(pos, pack & 1023);
        dst.set(pos+1, (pack >>> 10) & 1023);
        dst.set(pos+2, (pack >>> 20) & 1023);
        dst.set(pos+3, (pack >>> 30) & 1023);
        dst.set(pos+4, (pack >>> 40) & 1023);
        dst.set(pos+5, (pack >>> 50) & 1023);
    }
}

final class Packing5 extends Packing {

    Packing5() { super(5, 12); }

    long pack(LongGetter src, int pos) {
        return (11L << 60) | src.get(pos) | src.get(pos+1)<<12 | src.get(pos+2)<<24 | src.get(pos+3)<<36 | src.get(pos+4)<<48;
    }

    void unpack(long pack, LongSetter dst, int pos) {
        dst.set(pos, pack & 4095);
        dst.set(pos+1, (pack >>> 12) & 4095);
        dst.set(pos+2, (pack >>> 24) & 4095);
        dst.set(pos+3, (pack >>> 36) & 4095);
        dst.set(pos+4, (pack >>> 48) & 4095);
    }
}

final class Packing4 extends Packing {

    Packing4() { super(4, 15); }

    long pack(LongGetter src, int pos) {
        return (12L << 60) | src.get(pos) | src.get(pos+1)<<15 | src.get(pos+2)<<30 | src.get(pos+3)<<45;
    }

    void unpack(long pack, LongSetter dst, int pos) {
        dst.set(pos, pack & 32767);
        dst.set(pos+1, (pack >>> 15) & 32767);
        dst.set(pos+2, (pack >>> 30) & 32767);
        dst.set(pos+3, (pack >>> 45) & 32767);
    }
}

final class Packing3 extends Packing {

    Packing3() { super(3, 20); }

    long pack(LongGetter src, int pos) {
        return (13L << 60) | src.get(pos) | src.get(pos+1)<<20 | src.get(pos+2)<<40;
    }

    void unpack(long pack, LongSetter dst, int pos) {
        dst.set(pos, pack & 1048575);
        dst.set(pos+1, (pack >>> 20) & 1048575);
        dst.set(pos+2, (pack >>> 40) & 1048575);
    }
}

final class Packing2 extends Packing {

    Packing2() { super(2, 30); }

    long pack(LongGetter src, int pos) {
        return (14L << 60) | src.get(pos) | src.get(pos+1)<<30;
    }

    void unpack(long pack, LongSetter dst, int pos) {
        dst.set(pos, pack & 1073741823);
        dst.set(pos+1, (pack >>> 30) & 1073741823);
    }
}

final class Packing1 extends Packing {

    Packing1() { super(1, 60); }

    long pack(LongGetter src, int pos) {
        return (15L << 60) | src.get(pos);
    }

    void unpack(long pack, LongSetter dst, int pos) {
        dst.set(pos, pack & 1152921504606846975L);
    }
}
