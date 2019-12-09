/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.bnl.olog.entity;

import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_TYPE;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

/**
 *
 * @author berryman, shroffk
 */
public class Tag implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @Id
    private String name;
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
     * @param name tag name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the status
     */
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
     * @param data the Label to log
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
