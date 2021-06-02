package org.phoebus.olog.entity;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class InstanceDeserializer extends StdDeserializer<Instant>
{
    /**
     * 
     */
    private static final long serialVersionUID = -3082880744820026541L;

    public InstanceDeserializer()
    {
        this(null);
    }

    public InstanceDeserializer(Class<?> vc)
    {
        super(vc);
    }

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException
    {
        String[] split = p.getText().split("\\.");
        String instance = split[0];
        if(split.length == 2){ // If string contains period then assume the instant is represented as seconds.nanos
            instance = split[0] + split[1].substring(0, 3);
        }
        Number n = Long.parseLong(instance);
        return Instant.ofEpochMilli(n.longValue());
    }
}
