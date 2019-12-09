/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010-2011 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package gov.bnl.olog.entity;

import static gov.bnl.olog.OlogResourceDescriptors.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;

/**
 * Property object that can be represented as XML/JSON in payload data.
 *
 * @author Eric Berryman taken from Ralph Lange
 *         <Ralph.Lange@helmholtz-berlin.de>
 */
public class Property implements Serializable {

    /**
     * 
     */
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
     * Creates a new instance of Property.
     *
     * @param name
     * @param value
     */
    public Property(String name) {
        this.name = name;
    }


    /**
     * @param name
     * @param state
     * @param attributes
     */
    public Property(String name, String owner, State state, Set<Attribute> attributes) {
        this.name = name;
        this.setOwner(owner);
        this.state = state;
        this.attributes = attributes;
    }
    /**
     * @param name
     * @param attributes
     */
    public Property(String name, Set<Attribute> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    /**
     * Getter for property name.
     *
     * @return property name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for property name.
     *
     * @param name
     *            property name
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getOwner()
    {
        return owner;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    
    /**
     * @return the attributes
     */
    public Set<Attribute> getAttributes() {
        return attributes;
    }


    /**
     * Find the attributes with the matching name
     * @param name attribute name to be used to filter the set of property attributes
     * @return a set of attributes with matching names
     */
    public Set<Attribute> getAttribute(String name)
    {
        return attributes.stream().filter(attr -> {
            return name.equals(attr.getName());
        }).collect(Collectors.toSet());
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(Set<Attribute> attributes) {
        this.attributes = attributes;
    }

    /**
     * @param attributes the attributes to be added to the existing attribute sets
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
     * @param data
     *            the Property to log
     * @return string representation for log
     */
    public String toLogger() {
        if (this.attributes == null) {
            return this.getName();
        } else {
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
