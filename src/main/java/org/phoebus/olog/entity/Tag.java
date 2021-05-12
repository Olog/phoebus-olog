/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog.entity;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

/**
 * Tag object that can be represented as JSON in payload data.
 * @author berryman, Kunal Shroff
 */
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    private String name;
    private State state = State.Active;

    /**
     * Creates a new instance of Tag.
     */
    public Tag()
    {
    }

    /**
     * Creates a new instance of Tag with the given name.
     *
     * @param name - the name of the tag
     */
    public Tag(String name)
    {
        this.name = name;
    }

    /**
     * Creates a new instance of Tag with the given name and state.
     * @param name - the name of the tag
     * @param state - the {@link State} of the tag
     */
    public Tag(String name, State state)
    {
        this.name = name;
        this.state = state;
    }

    /**
     * Getter for tag name.
     *
     * @return name tag name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for tag name.
     *
     * @param name - tag name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the {@link State} of the tag
     */
    public State getState() {
        return state;
    }

    /**
     * Setter for the state of the Tag, Active or InActive
     * 
     * @param state - the {@link State} to set
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @return string representation for log
     */
    public String toLogger() {
        return this.getName();
    }

    @Override
    public String toString() {
        return "Tag [name=" + name + ", state=" + state + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Tag other = (Tag) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
}
