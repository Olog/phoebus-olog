/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog.entity;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

/**
 * Logbook object that can be represented as JSON in payload data.
 * @author Kunal Shroff
 *
 */
public class Logbook implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    private String name = null;
    private String owner = null;
    private State state = State.Active;

    /**
     * Creates a new instance of Logbook.
     *
     */
    public Logbook() {
    }

    /**
     * Creates a new instance of Logbook.
     *
     * @param name - name of the logbook
     * @param owner - owner of the logbook
     */
    public Logbook(String name, String owner) {
        this.owner = owner;
        this.name = name;
    }

    /**
     * Creates a new instance of Logbook.
     *
     * @param name - name of the logbook
     * @param owner - owner of the logbook
     * @param state - the state of the logbook, i.e. Active or Inactive
     */
    public Logbook(String name, String owner, State state) {
        this.owner = owner;
        this.name = name;
        this.state = state;
    }

    /**
     * Getter for logbook owner.
     *
     * @return owner logbook owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Setter for logbook owner.
     *
     * @param owner - logbook owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Getter for logbook name.
     *
     * @return name logbook name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for logbook name.
     *
     * @param name logbook name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for logbook state.
     *
     * @return the state
     */
    public State getState() {
        return state;
    }


    /**
     * Set the state of the logbook, Active or InActive
     * 
     * @param state - the state of the logbook
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
        return this.getName() + "(" + this.getOwner() + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
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
        Logbook other = (Logbook) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (owner == null) {
            if (other.owner != null) {
                return false;
            }
        } else if (!owner.equals(other.owner)) {
            return false;
        }
        return true;
    }
}
