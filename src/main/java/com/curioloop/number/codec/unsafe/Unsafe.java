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

import java.lang.invoke.*;
import java.lang.reflect.Field;

/**
 * Provide a unified Unsafe access interface among different versions of JDK.
 *
 * @author curioloops@gmail.com
 * @since 2024/4/23
 */
public class Unsafe {

    public static final String JDK_INTERNAL_UNSAFE_CLASS = "jdk.internal.misc.Unsafe";
    public static final String SUN_MISC_UNSAFE_CLASS = "sun.misc.Unsafe";

    public static final Object UNSAFE = getUnsafe();
    public static final String UNSAFE_TYPE = UNSAFE == null ? "" : UNSAFE.getClass().getName();

    private static Object getUnsafe() {
        try {
            // First, try to use jdk.internal.misc.Unsafe (for JDK 9+)
            Class<?> internalUnsafeClass = Class.forName(JDK_INTERNAL_UNSAFE_CLASS);
            Field theUnsafeField = internalUnsafeClass.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            return theUnsafeField.get(null);
        } catch (Throwable ignore) {
            // If jdk.internal.misc.Unsafe is not available (likely JDK 8), fallback to sun.misc.Unsafe
            try {
                Class<?> sunUnsafeClass = Class.forName(SUN_MISC_UNSAFE_CLASS);
                Field theUnsafeField = sunUnsafeClass.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                return theUnsafeField.get(null);
            } catch (Throwable ignored) {
                // Neither jdk.internal.misc.Unsafe nor sun.misc.Unsafe is available ...
            }
        }
        return null;
    }

    /**
     * Wraps Unsafe methods.
     *
     * @param lambdaClass  The lambda class.
     * @param signature    The method signature.
     * @param lambdaName   The lambda name.
     * @param unsafeName   The name of the unsafe operation.
     * @param <T>          The type of the lambda.
     * @return The wrapped unsafe method.
     * @throws Throwable if an error occurs during the operation.
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrapUnsafe(Class<T> lambdaClass, MethodType signature, String lambdaName, String unsafeName) throws Throwable {
        if (UNSAFE == null) return null;
        Class<?> unsafeClass = UNSAFE.getClass();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle handle = lookup.findVirtual(unsafeClass, unsafeName, signature);
        MethodType factory = MethodType.methodType(lambdaClass, unsafeClass);
        CallSite site = LambdaMetafactory.metafactory(lookup,
                lambdaName,
                factory,
                signature,
                handle,
                signature);
        return (T) site.getTarget().invoke(UNSAFE);
    }

    /**
     * Wraps Unsafe methods quietly.
     *
     * @param lambdaClass  The lambda class.
     * @param signature    The method signature.
     * @param lambdaName   The lambda name.
     * @param unsafeName   The name of the unsafe operation.
     * @param <T>          The type of the lambda.
     * @return The wrapped unsafe method, or null if an error occurs.
     */
    public static <T> T wrapUnsafeQuietly(Class<T> lambdaClass, MethodType signature, String lambdaName, String unsafeName) {
        try {
            return wrapUnsafe(lambdaClass, signature, lambdaName, unsafeName);
        } catch (Throwable ignore) {
            return null;
        }
    }

    /**
     * Represents Unsafe.getInt operation.
     */
    public static final GetInt GET_INT = wrapUnsafeQuietly(GetInt.class, MethodType.methodType(int.class, Object.class, long.class), "getInt", "getInt");

    /**
     * Represents Unsafe.getLong operation.
     */
    public static final GetLong GET_LONG = wrapUnsafeQuietly(GetLong.class, MethodType.methodType(long.class, Object.class, long.class), "getLong", "getLong");

    /**
     * Represents Unsafe.putInt operation.
     */
    public static final PutInt PUT_INT = wrapUnsafeQuietly(PutInt.class, MethodType.methodType(void.class, Object.class, long.class, int.class), "putInt", "putInt");

    /**
     * Represents Unsafe.putLong operation.
     */
    public static final PutLong PUT_LONG = wrapUnsafeQuietly(PutLong.class, MethodType.methodType(void.class, Object.class, long.class, long.class), "putLong", "putLong");

    /**
     * Represents Unsafe.arrayBaseOffset operation.
     */
    public static final ArrayOffset ARRAY_OFFSET = wrapUnsafeQuietly(ArrayOffset.class, MethodType.methodType(int.class, Class.class), "arrayBaseOffset", "arrayBaseOffset");

}
