package org.phoebus.olog.entity;

/**
 * A representation of an attribute associated with a property.
 * Simply put it is a key value pair with a state.
 * 
 * @author Kunal Shroff
 *
 */
public class Attribute {
    private String name;
    private String value;
    private State state = State.Active;

    /**
     * Creates a new instance of Attribute.
     */
    public Attribute()
    {
    }

    /**
     * Creates a new instance of Attribute with the given name
     * 
     * @param name - the name of the attribute
     */
    public Attribute(String name)
    {
        this.name = name;
    }

    /**
     * Creates a new instance of Attribute with the given name
     * 
     * @param name  - the attribute name
     * @param value - the attribute value
     */
    public Attribute(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    /**
     * Creates a new instance of Attribute with the given name
     * 
     * @param name - the attribute name
     * @param value - the attribute value
     * @param state - the attribute state
     */
    public Attribute(String name, String value, State state) {
        this.name = name;
        this.value = value;
        this.state = state;
    }

    /**
     * Getter for attribute name.
     *
     * @return attribute name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for attribute name.
     *
     * @param name - attribute name
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * Getter for attribute value.
     *
     * @return attribute value
     */
    public String getValue() {
        return value;
    }

    /**
     * Setter for attribute value.
     *
     * @param value - attribute value
     */
    public void setValue(String value) {
        this.value = value;
    }
    /**
     * Getter for attribute state.
     *
     * @return attribute state
     */
    public State getState() {
        return state;
    }

    /**
     * Setter for attribute state.
     *
     * @param state - attribute state
     */
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Attribute other = (Attribute) obj;
        if (name == null)
        {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (state != other.state)
            return false;
        if (value == null)
        {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
