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
package com.curioloop.number.codec.delta2;

import com.curioloop.number.codec.stream.LongSetter;
import com.curioloop.number.codec.CodecException;

/**
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
public class Delta2Setter implements LongSetter {

    protected final LongSetter stream;

    protected long base;
    protected int offset;
    protected int expected = 0;

    public Delta2Setter(LongSetter stream, long base, int offset) {
        this.stream = stream;
        this.base = base;
        this.offset = offset;
    }

    @Override
    public void set(int index, long value) {
        CodecException.notAllow(index != expected);
        long next = base + value;
        stream.set(offset + index, next);
        base = next;
        expected++;
    }

}