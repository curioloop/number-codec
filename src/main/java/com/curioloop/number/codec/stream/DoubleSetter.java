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
package com.curioloop.number.codec.stream;

/**
 * Write-only double sequences that support random access.
 *
 * @author curioloop
 * @since 2024/4/22
 */
@FunctionalInterface
public interface DoubleSetter {

    /**
     * Sets the double value at the specified position.
     *
     * @param index the position where the value will be set
     * @param value the double value to set
     */
    void set(int index, double value);
}
