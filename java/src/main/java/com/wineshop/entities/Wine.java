package com.wineshop.entities;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;

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
    private String name;
 
    @Basic
    private Integer year;
 
    @Enumerated(EnumType.STRING)
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
