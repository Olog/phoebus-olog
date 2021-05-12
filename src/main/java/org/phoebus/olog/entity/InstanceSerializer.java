package org.phoebus.olog.entity;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class InstanceSerializer extends StdSerializer<Instant>
{

    /**
     * 
     */
    private static final long serialVersionUID = 2186528737060744340L;

    public InstanceSerializer()
    {
        this(null);
    }

    public InstanceSerializer(Class<Instant> t)
    {
        super(t);
    }

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException
    {
        gen.writeNumber(value.toEpochMilli());
    }
}