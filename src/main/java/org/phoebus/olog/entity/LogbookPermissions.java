package org.phoebus.olog.entity;

import java.util.Set;

public record LogbookPermissions(String logbook, String infnUuid,
                                 Set<LogbookPermission> permissions, boolean admin) {
}
