package com._4dconcept.springframework.data.marklogic.repository;

import org.springframework.data.annotation.Id;

/**
 * Sample contact domain class.
 * 
 * @author Stéphane Toussaint
 */
public abstract class Contact {

	@Id
	protected String id;

	public String getId() {
		return id;
	}
}
