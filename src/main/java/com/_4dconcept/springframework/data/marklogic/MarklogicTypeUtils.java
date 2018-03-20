/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com._4dconcept.springframework.data.marklogic;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class that provide information about property type
 *
 * @author stoussaint
 * @since 2017-05-15
 */
public class MarklogicTypeUtils {

    public static final Map<Class<?>,Class<?>> primitiveMap = new HashMap<Class<?>, Class<?>>() {{
        put(boolean.class, Boolean.class);
        put(byte.class, Byte.class);
        put(char.class, Character.class);
        put(short.class, Short.class);
        put(int.class, Integer.class);
        put(long.class, Long.class);
        put(float.class, Float.class);
        put(double.class, Double.class);
    }};

    public static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() || type.equals(String.class) || isBoxingType(type);
    }

    private static boolean isBoxingType(Class<?> type) {
        return MarklogicTypeUtils.primitiveMap.values().contains(type);
    }

}