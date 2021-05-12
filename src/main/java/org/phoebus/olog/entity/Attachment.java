/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog.entity;

import org.springframework.core.io.InputStreamSource;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An object describing a log entry attachment.
 * 
 * @author Kunal Shroff
 *
 */
public class Attachment
{
    private String id;
    private String filename;
    private String fileMetadataDescription;
    @JsonIgnore
    private InputStreamSource attachment;

    /**
     * Creates a new instance of Attachment.
     */
    public Attachment()
    {

    }

    /**
     * Creates a new instance of Attachment.
     * @param attachment - {@link InputStreamSource} to the attachment file
     * @param filename - the attachment file name
     * @param fileMetadataDescription - the attachment file metadata
     */
    public Attachment(InputStreamSource attachment, String filename, String fileMetadataDescription)
    {
        this.attachment = attachment;
        this.filename = filename;
        this.fileMetadataDescription = fileMetadataDescription;
    }

    /**
     * Creates a new instance of Attachment.
     * @param id The unique attachment id.
     * @param attachment - {@link InputStreamSource} to the attachment file
     * @param filename - the attachment file name
     * @param fileMetadataDescription - the attachment file metadata
     */
    public Attachment(String id, InputStreamSource attachment, String filename, String fileMetadataDescription)
    {
        this.id = id;
        this.attachment = attachment;
        this.filename = filename;
        this.fileMetadataDescription = fileMetadataDescription;
    }

    /**
     * Getter for attachment id
     * 
     * @return attachment id
     */
    public String getId()
    {
        return id;
    }

    /**
     * Setter for attachment id
     * 
     * @param id - the id for the attachment
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * An input stream to the attachment
     *
     * @return attachment input stream
     */
    public InputStreamSource getAttachment()
    {
        return attachment;
    }

    /**
     * Setter for attachment{@link InputStreamSource}.
     *
     * @param attachment - attachment {@link InputStreamSource}
     */
    public void setAttachment(InputStreamSource attachment)
    {
        this.attachment = attachment;
    }

    /**
     * Getter for file name
     *
     * @return the attachment file name
     */
    public String getFilename()
    {
        return filename;
    }

    /**
     * Setter for attachment file name
     *
     * @param filename - attachment filename
     */
    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    /**
     * Getter for attachment file metadata
     *
     * @return attachement file metadata
     */
    public String getFileMetadataDescription()
    {
        return fileMetadataDescription;
    }

    /**
     * Setter for attachment file metadata
     *
     * @param fileMetadataDescription - metadata description
     */
    public void setFileMetadataDescription(String fileMetadataDescription)
    {
        this.fileMetadataDescription = fileMetadataDescription;
    }
}
