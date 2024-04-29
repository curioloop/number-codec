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
package com.curioloop.number.codec.unsafe;

/**
 * An operation to put an integer value into memory.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/23
 */
public interface PutInt {

    /**
     * Stores an integer value at the specified memory address.
     *
     * @param bytes  The base address in memory.
     * @param offset The offset from the base address.
     * @param value  The integer value to store.
     */
    void putInt(Object bytes, long offset, int value);

}
