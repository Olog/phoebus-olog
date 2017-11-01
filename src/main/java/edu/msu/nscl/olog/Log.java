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
import java.util.List;
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

    // TODO add support for attachments

    private Log() {

    }

    private Log(Long id, String version, String owner, String source, String description, Level level, State state,
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

    public static class LogBuilder {

        private String version;
        private String owner;
        private String source;
        private StringBuilder description;

        private Level level = Level.Info;
        private State state = State.Active;

        private Set<Property> properties = new HashSet<Property>();
        private Set<Logbook> logbooks = new HashSet<Logbook>();
        private Set<Tag> tags = new HashSet<Tag>();

        public LogBuilder() {
        }

        public LogBuilder(String source) {
            this.source = source;
        }

        public static LogBuilder createLog() {
            return new LogBuilder();
        }

        public static LogBuilder createLog(String source) {
            return new LogBuilder(source);
        }

        public LogBuilder description(String description) {
            if (description != null)
                this.description = new StringBuilder(description);
            else if (description == null)
                this.description = null;
            return this;
        }

        public LogBuilder appendDescription(String description) {
            if (this.description == null)
                this.description = new StringBuilder(description);
            else if (this.description != null)
                this.description.append("\n").append(description);
            return this;
        }

        public LogBuilder level(Level level) {
            this.level = level;
            return this;
        }

        public LogBuilder addLogbook(Logbook logbook) {
            this.logbooks.add(logbook);
            return this;
        }

        public LogBuilder addLogbooks(Set<Logbook> logbooks) {
            this.logbooks.addAll(logbooks);
            return this;
        }

        public LogBuilder setLogbooks(Set<Logbook> logbooks) {
            this.logbooks = logbooks;
            return this;
        }

        public LogBuilder addTag(Tag tag) {
            this.tags.add(tag);
            return this;
        }

        public LogBuilder addTags(Set<Tag> tags) {
            this.tags.addAll(tags);
            return this;
        }

        public LogBuilder setTags(Set<Tag> tags) {
            this.tags = tags;
            return this;
        }

        public LogBuilder addProperty(Property property) {
            this.properties.add(property);
            return this;
        }

        public LogBuilder addProperties(Set<Property> properties) {
            this.properties.addAll(properties);
            return this;
        }

        public LogBuilder setProperties(Set<Property> properties) {
            this.properties = properties;
            return this;
        }

        public Log build() {
            // TODO add validation
            return new Log();
        }
    }
}
