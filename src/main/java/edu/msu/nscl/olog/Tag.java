/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.msu.nscl.olog;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author berryman, shroffk
 */
@XmlType(propOrder = { "id", "name", "logs" })
@XmlRootElement(name = "tag")
public class Tag implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String id = null;
    private String name = null;
    private State state;

    public Tag() {
    }

    /**
     * Creates a new instance of Tag.
     *
     * @param name
     */
    public Tag(String name) {
        this.name = name;
    }

    public Tag(String name, State state) {
        this.name = name;
        this.state = state;
    }

    public Tag(String name, State state, String id) {
        this.id = id;
        this.name = name;
        this.state = state;
    }

    /**
     * Getter for tag id.
     *
     * @return id tag id
     */
    @XmlElement
    public Object getId() {
        return id;
    }

    /**
     * Setter for tag id.
     *
     * @param id tag id
     */
    public void setId(String id) {
        this.id = id;
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
    public static String toLogger(Tag data) {
        return data.getName();
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
