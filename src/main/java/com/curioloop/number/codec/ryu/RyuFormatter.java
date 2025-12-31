/*
 * Copyright © 2025 CurioLoop (curioloops@gmail.com)
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
package com.curioloop.number.codec.ryu;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Utility facade for converting floating-point numbers to decimal representations
 * using the Ryu algorithm. 
 *
 * @author curioloops@gmail.com
 * @since 2025/12/31
 */
@SuppressWarnings("all")
public class RyuFormatter {

    public static RoundingMode DEFAULT_MODE = RoundingMode.ROUND_EVEN;

    public static final ThreadLocal<char[]> RYU_CHARS =
            ThreadLocal.withInitial(() -> new char[RyuDouble.MAX_DOUBLE_LEN + 1]);

    /**
     * Converts a float value to BigDecimal using the Ryu algorithm.
     * 
     * @param value the float value to convert
     * @return the BigDecimal representation
     * @throws NumberFormatException if the value cannot be converted (NaN or Infinity)
     */
    public static BigDecimal decimal(float value) {
        BigDecimal decimal = (BigDecimal) RyuFloat.floatTo(value, DEFAULT_MODE, null);
        if (decimal == null) throw new NumberFormatException(Float.toString(value));
        return decimal;
    }

    /**
     * Converts a double value to BigDecimal using the Ryu algorithm.
     * 
     * @param value the double value to convert
     * @return the BigDecimal representation
     * @throws NumberFormatException if the value cannot be converted (NaN or Infinity)
     */
    public static BigDecimal decimal(double value) {
        BigDecimal decimal = (BigDecimal) RyuDouble.doubleTo(value, DEFAULT_MODE, null);
        if (decimal == null) throw new NumberFormatException(Double.toString(value));
        return decimal;
    }

    /**
     * Converts a float value to a char array representation using the Ryu algorithm.
     * 
     * @param value the float value to convert
     * @return a char array containing the decimal representation
     */
    public static char[] chars(float value) {
        char[] chars = RYU_CHARS.get();
        chars = (char[]) RyuFloat.floatTo(value, DEFAULT_MODE, chars);
        return Arrays.copyOf(chars, chars[RyuFloat.MAX_FLOAT_LEN]);
    }

    /**
     * Converts a double value to a char array representation using the Ryu algorithm.
     * 
     * @param value the double value to convert
     * @return a char array containing the decimal representation
     */
    public static char[] chars(double value) {
        char[] chars = RYU_CHARS.get();
        chars = (char[]) RyuDouble.doubleTo(value, DEFAULT_MODE, chars);
        return Arrays.copyOf(chars, chars[RyuDouble.MAX_DOUBLE_LEN]);
    }

    /**
     * Converts a float value to its string representation using the Ryu algorithm.
     * 
     * @param value the float value to convert
     * @return the string representation of the value
     */
    public static String string(float value) {
        char[] chars = RYU_CHARS.get();
        chars = (char[]) RyuFloat.floatTo(value, DEFAULT_MODE, chars);
        return new String(chars, 0, chars[RyuFloat.MAX_FLOAT_LEN]);
    }

    /**
     * Converts a double value to its string representation using the Ryu algorithm.
     * 
     * @param value the double value to convert
     * @return the string representation of the value
     */
    public static String string(double value) {
        char[] chars = RYU_CHARS.get();
        chars = (char[]) RyuDouble.doubleTo(value, DEFAULT_MODE, chars);
        return new String(chars, 0, chars[RyuDouble.MAX_DOUBLE_LEN]);
    }

    /**
     * Appends the string representation of a float value to a StringBuilder using the Ryu algorithm.
     * 
     * @param value the float value to convert and append
     * @param buffer the StringBuilder to append to
     */
    public static void printTo(float value, StringBuilder buffer) {
        char[] chars = RYU_CHARS.get();
        chars = (char[]) RyuFloat.floatTo(value, DEFAULT_MODE, chars);
        buffer.append(chars, 0, chars[RyuFloat.MAX_FLOAT_LEN]);
    }

    /**
     * Appends the string representation of a double value to a StringBuilder using the Ryu algorithm.
     * 
     * @param value the double value to convert and append
     * @param buffer the StringBuilder to append to
     */
    public static void printTo(double value, StringBuilder buffer) {
        char[] chars = RYU_CHARS.get();
        chars = (char[]) RyuDouble.doubleTo(value, DEFAULT_MODE, chars);
        buffer.append(chars, 0, chars[RyuDouble.MAX_DOUBLE_LEN]);
    }

}
