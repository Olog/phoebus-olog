/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A event represents a specific instance in time that can be assigned to log entries.
 * @author Kunal Shroff
 *
 */
public class Event
{
    @Id
    private String name;
    @JsonSerialize(using = InstanceSerializer.class)
    @JsonDeserialize(using = InstanceDeserializer.class)
    private Instant instant = null;

    /**
     * Create a new instant of {@link Event}
     */
    public Event()
    {
    }

    /**
     * Create a new instant of {@link Event} with the given name
     * 
     * @param name - name of the event
     */
    public Event(String name)
    {
        this.name = name;
        this.instant = Instant.now();
    }

    /**
     * Create a new instant of {@link Event} with the given name and instant
     * 
     * @param name - event name
     * @param instant - the instant in time described by this event
     */
    public Event(String name, Instant instant)
    {
        this.name = name;
        this.instant = instant;
    }

    /**
     * Getter for event name.
     *
     * @return name event name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Setter for event name.
     *
     * @param name - set event name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Getter for event instant.
     *
     * @return instant - the {@link Instant} in time described by this event
     */
    public Instant getInstant()
    {
        return instant;
    }

    /**
     * Setter for event instant.
     *
     * @param instant - set event Instant
     */
    public void setInstant(Instant instant)
    {
        this.instant = instant;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((instant == null) ? 0 : instant.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Event other = (Event) obj;
        if (instant == null)
        {
            if (other.instant != null)
                return false;
        } else if (!instant.equals(other.instant))
            return false;
        if (name == null)
        {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
