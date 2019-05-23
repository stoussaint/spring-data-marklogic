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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlTransient;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class that provide information about property type
 *
 * @author stoussaint
 * @since 2017-05-15
 */
public class MarklogicTypeUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarklogicTypeUtils.class);

    private static final Map<Class<?>,Class<?>> PRIMITIVE_MAP = new HashMap<>();

    static {
        PRIMITIVE_MAP.put(boolean.class, Boolean.class);
        PRIMITIVE_MAP.put(byte.class, Byte.class);
        PRIMITIVE_MAP.put(char.class, Character.class);
        PRIMITIVE_MAP.put(short.class, Short.class);
        PRIMITIVE_MAP.put(int.class, Integer.class);
        PRIMITIVE_MAP.put(long.class, Long.class);
        PRIMITIVE_MAP.put(float.class, Float.class);
        PRIMITIVE_MAP.put(double.class, Double.class);
    }

    private MarklogicTypeUtils() {}

    public static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() || type.equals(String.class) || isBoxingType(type);
    }

    public static <T> T convertStringToPrimitive(Class<T> returnType, String value) {
        try {
            Method m = PRIMITIVE_MAP.get(returnType).getMethod("valueOf", String.class);
            @SuppressWarnings("unchecked") // Trust the valueOf method of the boxing type
            T result = (T) m.invoke(null, value);
            return result;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.debug("Unable to generate primitive value for type {}", returnType.getName());
            return null;
        }
    }

    /**
     * Retrieve the first non XmlTransient annotated type from deeperType to upperType.
     *
     * @param deeperType the deeperType of the hierarchy to test
     * @param upperType the upperType of the hierarchy to test
     * @return the xml eligible type
     */
    public static Class<?> resolveXmlType(Class<?> deeperType, Class<?> upperType) {
        if (!deeperType.isAssignableFrom(upperType)) {
            throw new IllegalArgumentException(String.format("%s --> %s is not a valid hierarchy", upperType, deeperType));
        }

        Deque<Class<?>> types = new ArrayDeque<>();

        Class<?> currentType = upperType;
        while (! deeperType.equals(currentType)) {
            types.push(currentType);
            currentType = currentType.getSuperclass();
        }

        types.push(deeperType);

        while (! types.isEmpty()) {
            Class<?> type = types.pop();
            if (type.getAnnotation(XmlTransient.class) == null) {
                return type;
            }
        }

        throw new IllegalArgumentException(String.format("Unable to determine a non XmlTransient type in provided hierarchy %s --> %s", upperType, deeperType));
    }

    public static boolean isSupportedType(Class<?> aClass) {
        return MarklogicSupportedType.fromClass(aClass).isPresent();
    }

    private static boolean isBoxingType(Class<?> type) {
        return MarklogicTypeUtils.PRIMITIVE_MAP.values().contains(type);
    }
}