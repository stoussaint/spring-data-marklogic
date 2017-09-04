@XmlSchema(
        elementFormDefault = XmlNsForm.QUALIFIED,
        namespace = "http://spring.data.marklogic/test/contact",
        xmlns={@XmlNs(
                prefix="sdm",
                namespaceURI="http://spring.data.marklogic/test/contact"
        )}
)
package org.springframework.data.marklogic.repository;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;