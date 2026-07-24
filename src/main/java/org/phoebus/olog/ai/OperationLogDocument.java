package org.phoebus.olog.ai;

import java.util.List;
//cleaned 
public class OperationLogDocument {

    private String id;
    private String owner;
    private String title;
    private String description;
    private String source;
    private String level;
    private String state;
    private Long createdDate;
    private Long modifyDate;
   
    private List<String> logbooksName; 
    private List<String> tagsName;
    private List<String> eventsName;
    

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Long getCreatedDate() { return createdDate; }
    public void setCreatedDate(Long createdDate) { this.createdDate = createdDate; }

    public Long getModifyDate() { return modifyDate; }
    public void setModifyDate(Long modifyDate) { this.modifyDate = modifyDate; }

    public List<String> getLogbooksName() { return logbooksName; }
    public void setLogbooksName(List<String> logbooksName) { this.logbooksName = logbooksName; }

    public List<String> getTagsName() { return tagsName; }
    public void setTagsName(List<String> tagsName) { this.tagsName = tagsName; }
    
    public List<String> getEventsName() { return eventsName; }
    public void setEventsName(List<String> eventsName) { this.eventsName = eventsName; }
}
