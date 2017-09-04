package com._4dconcept.springframework.data.marklogic.sample;

import javax.xml.bind.annotation.XmlElement;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-07-31
 */
public class Address {

    private String city;
    private String street;
    private String country;

    /**
     * @return the city
     */
    @XmlElement(name = "town", namespace = "http://global.com/address/")
    public String getCity() {
        return city;
    }

    /**
     * @param city the city to set
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country the country to set
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * @return the street
     */
    public String getStreet() {
        return street;
    }

    /**
     * @param street the street to set
     */
    public void setStreet(String street) {
        this.street = street;
    }
}
