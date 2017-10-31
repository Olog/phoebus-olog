/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin fuer Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */
package edu.msu.nscl.olog;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Log object that can be represented as XML/JSON in payload data.
 *
 * @author Eric Berryman taken from Ralph Lange <Ralph.Lange@bessy.de>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "createdDate", "modifiedDate", "owner", "source", "version", "description", "logbooks", "tags",
        "xmlProperties", "xmlAttachments" })
@XmlRootElement(name = "log")
public class Log implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Long id;

    private Instant createdDate;

    private String version;

    private String owner;

    private String source;
    private String description;

    private Level level;
    private State state;

    private Collection<Property> properties = new ArrayList<Property>();
    private Set<Logbook> logbooks = new HashSet<Logbook>();
    private Set<Tag> tags = new HashSet<Tag>();

    public Log(Long id, String version, String owner, String source, String description, Level level, State state,
            Date modifiedDate, Collection<Property> properties, Set<Logbook> logbooks, Set<Tag> tags) {

        super();
        this.id = id;
        this.version = version;
        this.owner = owner;
        this.source = source;
        this.description = description;
        this.level = level;
        this.state = state;
        this.properties = properties;

        this.logbooks = logbooks;
        this.tags = tags;
    }

}
