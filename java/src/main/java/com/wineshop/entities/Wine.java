package com.wineshop.entities;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
public class Wine extends AbstractEntity {
 
    private static final long serialVersionUID = 1L;
 
    public static enum Type {
        RED,
        WHITE,
        ROSE
    }
 
    @ManyToOne
    private Vineyard vineyard;
 
    @Basic
    @Size(min=5, max=100,
    	message="The name must contain between {min} and {max} characters")
    private String name;
 
    @Basic
    @Min(value=1900,
    	message="The year must be greater than {value}")
    private Integer year;
 
    @Enumerated(EnumType.STRING)
    @NotNull
    private Type type;
 
    public Vineyard getVineyard() {
        return vineyard;
    }
 
    public void setVineyard(Vineyard vineyard) {
        this.vineyard = vineyard;
    }
 
    public Integer getYear() {
        return year;
    }
 
    public void setYear(Integer annee) {
        this.year = annee;
    }
 
    public String getName() {
        return name;
    }
 
    public void setName(String nom) {
        this.name = nom;
    }
 
    public Type getType() {
        return type;
    }
 
    public void setType(Type type) {
        this.type = type;
    }
}
