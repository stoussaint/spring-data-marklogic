package com._4dconcept.springframework.data.marklogic.core.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to provide collection information.
 *
 * <p>This information are used by the store engine to set specific collections while saving an entity.
 * it is also used by the query engine as query constraints.</p>
 *
 * @author Stéphane Toussaint
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
public @interface Collection {

    String prefix() default "";

    String value() default "";

}
