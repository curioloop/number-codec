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

import com.curioloop.number.codec.stream.LongGetter;
import com.curioloop.number.codec.CodecException;
import lombok.RequiredArgsConstructor;

/**
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
@RequiredArgsConstructor
final class Delta2Getter implements LongGetter {

    final boolean sorted;
    final LongGetter timestamps; // sorted timestamp

    public long get(int i) {
        long current = timestamps.get(i);
        long previous = i == 0 ? 0 : timestamps.get(i-1);
        long delta2 = current - previous;
        CodecException.valueOverflow(sorted && delta2 < 0);
        return delta2;
    }

}
