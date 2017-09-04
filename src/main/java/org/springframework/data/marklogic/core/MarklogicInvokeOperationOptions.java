package org.springframework.data.marklogic.core;

import java.util.HashMap;
import java.util.Map;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicInvokeOperationOptions extends MarklogicOperationOptions {

    default Map<Object, Object> params() {
        return new HashMap<>();
    }

    default boolean useCacheResult() {
        return true;
    }

}
