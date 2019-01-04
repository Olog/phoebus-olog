package edu.msu.nscl.olog.entity;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Attachment
{
    private String id;
    @JsonIgnore
    private MultipartFile attachment;
    private String filename;
    private String fileMetadataDescription;

    public Attachment() {
        
    }
    public Attachment(MultipartFile attachment, String filename, String fileMetadataDescription)
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

    public MultipartFile getAttachment()
    {
        return attachment;
    }

    public void setAttachment(MultipartFile attachment)
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
