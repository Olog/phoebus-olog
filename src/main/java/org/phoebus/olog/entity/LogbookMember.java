package org.phoebus.olog.entity;

import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class LogbookMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private String logbook;
    private String infnUuid;
    private Set<LogbookPermission> permissions = new HashSet<>();
    private String createdBy;
    private String createdAt;
    private String updatedAt;
    private String visibleFrom;
    private String visibleTo;

    public String getVisibleFrom() { return visibleFrom; }
    public void setVisibleFrom(String visibleFrom) { this.visibleFrom = visibleFrom; }
    public String getVisibleTo() { return visibleTo; }
    public void setVisibleTo(String visibleTo) { this.visibleTo = visibleTo; }

    public LogbookMember() {
    }

    public LogbookMember(String logbook, String infnUuid, Set<LogbookPermission> permissions,
                         String createdBy, String createdAt, String updatedAt) {
        this.id = id(logbook, infnUuid);
        this.logbook = logbook;
        this.infnUuid = infnUuid;
        this.permissions = new HashSet<>(permissions);
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static String id(String logbook, String infnUuid) {
        return logbook + ":" + infnUuid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogbook() {
        return logbook;
    }

    public void setLogbook(String logbook) {
        this.logbook = logbook;
    }

    public String getInfnUuid() {
        return infnUuid;
    }

    public void setInfnUuid(String infnUuid) {
        this.infnUuid = infnUuid;
    }

    public Set<LogbookPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<LogbookPermission> permissions) {
        this.permissions = permissions;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
