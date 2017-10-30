/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */
package edu.msu.nscl.olog;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Logbook object that can be represented as XML/JSON in payload data.
 *
 * @author Eric Berryman taken from Ralph Lange <Ralph.Lange@bessy.de>
 */
@XmlRootElement(name = "logbook")
@XmlType(propOrder = { "name", "owner", "state" })
public class Logbook implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String name = null;
    private String owner = null;
    private State state = State.Active;
    // private List<Log> logs = new Logs();

    /**
     * Creates a new instance of Logbook.
     *
     */
    public Logbook() {
    }

    /**
     * Creates a new instance of Logbook.
     *
     * @param name
     * @param owner
     */
    public Logbook(String name, String owner) {
        this.owner = owner;
        this.name = name;
    }

    /**
     * Creates a new instance of Logbook.
     *
     * @param name
     * @param owner
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
    @XmlAttribute
    public String getOwner() {
        return owner;
    }

    /**
     * Setter for logbook owner.
     *
     * @param owner
     *            logbook owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Getter for tag name.
     *
     * @return name tag name
     */
    @XmlAttribute
    public String getName() {
        return name;
    }

    /**
     * Setter for tag name.
     *
     * @param name
     *            tag name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the status
     */
    @XmlAttribute
    public State getState() {
        return state;
    }

    /**
     * @return the status
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Creates a compact string representation for the log.
     *
     * @param data
     *            the Label to log
     * @return string representation for log
     */
    public static String toLogger(Logbook data) {
        return data.getName() + "(" + data.getOwner() + ")";
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
