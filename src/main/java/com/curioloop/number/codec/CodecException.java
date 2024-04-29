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

/**
 * Exception class used for signaling errors during encoding and decoding operations.
 *
 * <p>Instances of this class can represent various types of errors:</p>
 * <ul>
 *     <li>{@link CodecException.AssertFailure AssertException}: Indicates an assertion failure.</li>
 *     <li>{@link CodecException.ValueOverflow ValueOverflow}: Indicates an error due to value overflow during encoding.</li>
 *     <li>{@link CodecException.MalformedData MalformedData}: Indicates an error due to malformed data during decoding.</li>
 *     <li>{@link CodecException.WrapUncaught WrapUncaught}: Indicates an uncaught exception that has been wrapped.</li>
 * </ul>
 *
 * @author curioloops@gmail.com
 * @since 2024/4/22
 */
public class CodecException extends RuntimeException {

    private CodecException() {}

    private CodecException(Throwable t) { super(t); }

    /**
     * Exception indicating an assertion failure.
     */
    public static class AssertFailure extends CodecException {
        private AssertFailure() {}
    }

    /**
     * Exception indicating an error due to value overflow during encoding.
     */
    public static class ValueOverflow extends CodecException {
        private ValueOverflow() {}
    }

    /**
     * Exception indicating an error due to malformed data during decoding.
     */
    public static class MalformedData extends CodecException {
        private MalformedData() {}
    }

    /**
     * Exception indicating an uncaught exception that has been wrapped.
     */
    public static class WrapUncaught extends CodecException {
        private WrapUncaught(Throwable t) { super(t); }
    }

    /**
     * Throws an {@link AssertFailure} if the specified condition is {@code true}.
     *
     * @param cond The condition to check.
     * @throws AssertFailure If the condition is {@code true}.
     */
    public static void notAllow(boolean cond) throws AssertFailure {
        if (cond) throw new AssertFailure();
    }

    /**
     * Throws a {@link ValueOverflow} if the specified condition is {@code true}.
     *
     * @param cond The condition to check.
     * @throws ValueOverflow If the condition is {@code true}.
     */
    public static void valueOverflow(boolean cond) throws ValueOverflow {
        if (cond) throw new ValueOverflow();
    }

    /**
     * Throws a {@link MalformedData} if the specified condition is {@code true}.
     *
     * @param cond The condition to check.
     * @throws MalformedData If the condition is {@code true}.
     */
    public static void malformedData(boolean cond) throws MalformedData {
        if (cond) throw new MalformedData();
    }

    /**
     * Throws a {@link WrapUncaught} exception wrapping the specified throwable.
     *
     * @param ex The throwable to wrap.
     * @throws WrapUncaught Always throws this exception.
     */
    public static void wrapUncaught(Throwable ex) throws WrapUncaught {
        throw new WrapUncaught(ex);
    }

}
