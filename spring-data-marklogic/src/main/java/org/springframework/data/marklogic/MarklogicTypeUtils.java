package org.springframework.data.marklogic;

import java.util.HashMap;
import java.util.Map;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-05-15
 */
public class MarklogicTypeUtils {

    public static final Map<Class,Class> primitiveMap = new HashMap<Class, Class>() {{
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

    public static boolean isBoxingType(Class<?> type) {
        return MarklogicTypeUtils.primitiveMap.values().contains(type);
    }

}
