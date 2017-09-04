package com._4dconcept.springframework.data.marklogic.core.mapping;

import javax.xml.namespace.QName;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicIdentifier {

    QName qname();

    String value();

}