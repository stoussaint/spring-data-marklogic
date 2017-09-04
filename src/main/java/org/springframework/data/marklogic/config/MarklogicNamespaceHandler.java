package org.springframework.data.marklogic.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@link org.springframework.beans.factory.xml.NamespaceHandler} for Marklogic configuration.
 * 
 * @author Stéphane Toussaint
 */
public class MarklogicNamespaceHandler extends NamespaceHandlerSupport {

	/*
	No XML definition support yet
	 */

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
	 */
	public void init() {
//		registerBeanDefinitionParser("mapping-converter", new MappingMarklogicConverterParser());
//		registerBeanDefinitionParser("jmx", new MarklogicJmxParser());
//		registerBeanDefinitionParser("template", new MarklogicTemplateParser());
	}
}
