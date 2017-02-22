package org.springframework.data.marklogic.core.mapping;

import org.springframework.data.annotation.Persistent;

import java.lang.annotation.*;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Document {

    String uri() default "";

    String defaultCollectionPrefix() default "";

    String defaultCollection() default "";

    boolean idInPropertyFragment() default false;

}