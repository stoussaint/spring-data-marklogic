package com._4dconcept.springframework.data.marklogic;

/**
 * Helper class featuring helper methods for working with Marklogic urls.
 * <p/>
 * <p/>
 * Mainly intended for internal use within the framework.
 *
 * @author Stéphane Toussaint
 */
public abstract class MarklogicUrlUtils {

    private static String URL_PREFIX = "/content/";
    private static String URL_SUFFIX = "/#{id}.xml";

    /**
     * Private constructor to prevent instantiation.
     */
    private MarklogicUrlUtils() {}

    /**
     * Obtains the uri name to use for the provided class
     *
     * @param entityClass The class to determine the preferred uri name for
     * @return The preferred rul name
     */
    public static String getPreferredUrlPattern(Class<?> entityClass) {
        return URL_PREFIX + entityClass.getSimpleName().toLowerCase() + URL_SUFFIX;
    }
}
