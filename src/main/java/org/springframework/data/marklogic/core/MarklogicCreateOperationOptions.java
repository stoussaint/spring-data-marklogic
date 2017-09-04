package org.springframework.data.marklogic.core;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicCreateOperationOptions extends MarklogicOperationOptions {

    String uri();
    String[] extraCollections();

}
