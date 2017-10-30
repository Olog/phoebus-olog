package edu.msu.nscl.olog;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A representation of an attribute associated with a property. Simply put it is
 * a key value pair with a state.
 * 
 * @author Kunal Shroff
 *
 */
@XmlRootElement(name = "attribute")
@XmlType(propOrder = { "name", "value", "state" })
public class Attribute {
    private String name;
    private String value;
    private State state = State.Active;

    public Attribute() {
    }

    public Attribute(String name) {
        this.name = name;
    }

    public Attribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Attribute(String name, String value, State state) {
        this.name = name;
        this.value = value;
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Attribute other = (Attribute) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
