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

import com.curioloop.number.codec.gorilla.GorillaCodec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Represents the result of a codec operation, containing encoded data and codec flags.
 * <p>Codec flags are used to indicate the encoding scheme used in the data.
 * Multiple flags can be combined using bitwise OR operation.</p>
 *
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PUBLIC, staticName = "of")
public class CodecResult {

    public static final int
            CODEC_RAW =     1,      // Raw encoding scheme
            CODEC_GORILLA = 1 << 1, // Gorilla encoding scheme
            CODEC_VAR_INT = 1 << 2, // VarInt encoding scheme
            CODEC_ZIGZAG =  1 << 3, // ZigZag encoding scheme
            CODEC_SIMPLE8 = 1 << 4, // Simple8 encoding scheme
            CODEC_DELTA2 =  1 << 5; // Delta2 encoding scheme

    private final byte[] data;
    private final int codecs;

    @Override
    public String toString() {
        return "CodecResult(" + "codecs=" + codecs + ", data=" + GorillaCodec.toBinaryString(data) + ')';
    }

}
