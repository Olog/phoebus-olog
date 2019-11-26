package gov.bnl.olog.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Event
{
    @Id
    private String name;
    @JsonSerialize(using = InstanceSerializer.class)
    @JsonDeserialize(using = InstanceDeserializer.class)
    private Instant event = null;

    public Event()
    {
    }

    public Event(String name)
    {
        this.name = name;
        this.event = Instant.now();
    }

    public Event(String name, Instant event)
    {
        this.name = name;
        this.event = event;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Instant getEvent()
    {
        return event;
    }

    public void setEvent(Instant event)
    {
        this.event = event;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((event == null) ? 0 : event.hashCode());
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
        if (event == null)
        {
            if (other.event != null)
                return false;
        } else if (!event.equals(other.event))
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
