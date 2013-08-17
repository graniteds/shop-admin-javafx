package com.wineshop.entities;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class Address implements Serializable {
 
    private static final long serialVersionUID = 1L;
 
    @Basic
    @NotNull
    @Size(min=5, max=100, 
    	message="The address must contain between {min} and {max} characters")
    private String address;
 
    public String getAddress() {
        return address;
    }
 
    public void setAddress(String adresse) {
        this.address = adresse;
    }
}
