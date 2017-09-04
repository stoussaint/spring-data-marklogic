package org.springframework.data.marklogic.repository;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-08-02
 */
public class Address {

    private String country;
    private String town;

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
     * @return the town
     */
    public String getTown() {
        return town;
    }

    /**
     * @param town the town to set
     */
    public void setTown(String town) {
        this.town = town;
    }
}
