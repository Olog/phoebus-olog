package org.phoebus.olog.entity;

import java.util.Set;

public record LogbookMemberRequest(String infnUuid, Set<LogbookPermission> permissions,
                                   String visibleFrom, String visibleTo) {
}
