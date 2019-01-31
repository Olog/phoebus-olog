package edu.msu.nscl.olog.entity;

import org.springframework.core.io.InputStreamSource;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Attachment
{
    private String id;
    private String filename;
    private String fileMetadataDescription;
    @JsonIgnore
    private InputStreamSource attachment;

    public Attachment() {
        
    }
    public Attachment(InputStreamSource attachment, String filename, String fileMetadataDescription)
    {
        this.attachment = attachment;
        this.filename = filename;
        this.fileMetadataDescription = fileMetadataDescription;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public InputStreamSource getAttachment()
    {
        return attachment;
    }

    public void setAttachment(InputStreamSource attachment)
    {
        this.attachment = attachment;
    }

    public String getFilename()
    {
        return filename;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public String getFileMetadataDescription()
    {
        return fileMetadataDescription;
    }

    public void setFileMetadataDescription(String fileMetadataDescription)
    {
        this.fileMetadataDescription = fileMetadataDescription;
    }
}
