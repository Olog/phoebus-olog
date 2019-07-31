package gov.bnl.olog.entity;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class InstanceDeserializer extends StdDeserializer<Instant>
{
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
        String instance = p.getText();
        Number n = Long.parseLong(instance);
        return Instant.ofEpochMilli(n.longValue());
    }
}
