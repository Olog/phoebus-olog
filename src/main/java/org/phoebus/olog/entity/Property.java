/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;

/**
 * Property object that can be represented as JSON in payload data.
 * @author Kunal Shroff
 *
 */
public class Property implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    private String name;
    private String owner;
    private State state = State.Active;

    private Set<Attribute> attributes = new HashSet<Attribute>();

    /**
     * Creates a new instance of Property.
     *
     */
    public Property() {
    }

    /**
     * Create a new instance of Property.
     * @param name - the name of the property
     */
    public Property(String name) {
        this.name = name;
    }

    /**
     * Create a new instance of Property.
     * @param name - name property name
     * @param owner - the owner of this property
     * @param state - state property state
     * @param attributes - attributes a set of attributes for this property
     */
    public Property(String name, String owner, State state, Set<Attribute> attributes) {
        this.name = name;
        this.setOwner(owner);
        this.state = state;
        this.attributes = attributes;
    }

    /**
     * Create a new instance of Property.
     * @param name - property name
     * @param attributes - a set of attributes for this property
     */
    public Property(String name, Set<Attribute> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    /**
     * Getter for property name.
     *
     * @return the property name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for property name.
     *
     * @param name - property name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for property owner.
     * @return the property owner
     */
    public String getOwner()
    {
        return owner;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    /**
     * Getter for property state.
     * @return the property state
     */
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    
    /**
     * Getter for property attributes.
     * @return a set of the attributes
     */
    public Set<Attribute> getAttributes() {
        return attributes;
    }


    /**
     * Find the attributes with the matching name.
     * @param name - attribute name to be used to filter the set of property attributes
     * @return a set of attributes with matching names
     */
    public Set<Attribute> getAttribute(String name)
    {
        return attributes.stream().filter(attr -> {
            return name.equals(attr.getName());
        }).collect(Collectors.toSet());
    }

    /**
     * Set the attributes of this property.
     * @param attributes - the attributes to set
     */
    public void setAttributes(Set<Attribute> attributes) {
        this.attributes = attributes;
    }

    /**
     * Append the given set of attributes to the existing attributes of this
     * property
     *
     * @param attributes - the attributes to be added to the existing attribute sets
     */
    public void addAttributes(Set<Attribute> attributes) {
        this.attributes.addAll(attributes);
    }

    /**
     * @param attribute the attribute to be added to the existing attribute sets
     */
    public void addAttributes(Attribute attribute) {
        this.attributes.add(attribute);
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @return string representation for log
     */
    public String toLogger()
    {
        if (this.attributes == null)
        {
            return this.getName();
        } else
        {
            return this.getName() + "(" + this.getAttributes().toString() + ")";
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Property other = (Property) obj;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
