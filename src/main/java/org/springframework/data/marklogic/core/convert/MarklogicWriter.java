package org.springframework.data.marklogic.core.convert;

import org.springframework.data.convert.EntityWriter;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicWriter<T> extends EntityWriter<T, MarklogicContentHolder> {

}