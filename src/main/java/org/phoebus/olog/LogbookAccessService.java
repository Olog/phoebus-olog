package org.phoebus.olog;

import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Log;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.UntypedRangeQuery;
import co.elastic.clients.json.JsonData;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.phoebus.olog.entity.LogbookMember;
import org.phoebus.olog.entity.LogbookPermission;
import org.phoebus.olog.entity.LogbookPermissions;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class LogbookAccessService {

    private final LogbookRepository logbookRepository;
    private final LogbookMemberRepository memberRepository;

    /**
     * Accounts that may read every logbook without a membership, e.g. the backup and export jobs.
     * Comma separated (property logbook.service-readers, env LOGBOOK_SERVICE_READERS); empty by default.
     */
    private Set<String> serviceReaders = Set.of();

    public LogbookAccessService(LogbookRepository logbookRepository, LogbookMemberRepository memberRepository) {
        this.logbookRepository = logbookRepository;
        this.memberRepository = memberRepository;
    }

    @Value("${logbook.service-readers:}")
    void setServiceReaders(String serviceReaders) {
        this.serviceReaders = Arrays.stream(serviceReaders.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Permissions every authenticated user has on every logbook, on top of their memberships,
     * e.g. READ,WRITE for an open facility logbook. ADMIN is never granted this way.
     * Comma separated (property logbook.authenticated-permissions, env LOGBOOK_AUTHENTICATED_PERMISSIONS); empty by default.
     */
    private Set<LogbookPermission> authenticatedPermissions = EnumSet.noneOf(LogbookPermission.class);

    @Value("${logbook.authenticated-permissions:}")
    void setAuthenticatedPermissions(String authenticatedPermissions) {
        EnumSet<LogbookPermission> permissions = EnumSet.noneOf(LogbookPermission.class);
        Arrays.stream(authenticatedPermissions.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> LogbookPermission.valueOf(name.toUpperCase()))
                .forEach(permissions::add);
        permissions.remove(LogbookPermission.ADMIN);
        this.authenticatedPermissions = permissions;
    }

    private boolean everyoneReads() {
        return authenticatedPermissions.contains(LogbookPermission.READ);
    }

    private boolean isServiceReader(Principal principal) {
        return principal != null && serviceReaders.contains(principal.getName());
    }

    public LogbookMember createOwnerMembership(String logbook, Principal principal) {
        String now = Instant.now().toString();
        LogbookMember owner = new LogbookMember(logbook, principal.getName(),
                EnumSet.allOf(LogbookPermission.class), principal.getName(), now, now);
        return memberRepository.save(owner);
    }

    public List<LogbookMember> findMembers(String logbook, Principal principal) {
        requireAdmin(logbook, principal);
        return memberRepository.findByLogbook(logbook);
    }

    public LogbookMember addMember(String logbook, String infnUuid,
                                   Set<LogbookPermission> permissions, Principal principal) {
        return addMember(logbook, infnUuid, permissions, null, null, principal);
    }

    public LogbookMember addMember(String logbook, String infnUuid,
                                   Set<LogbookPermission> permissions, String visibleFrom, String visibleTo,
                                   Principal principal) {
        requireAdmin(logbook, principal);
        validateCollaborator(infnUuid, permissions);
        validatePeriod(visibleFrom, visibleTo);
        if (memberRepository.find(logbook, infnUuid).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The user is already a logbook member");
        }
        String now = Instant.now().toString();
        LogbookMember member = new LogbookMember(logbook, infnUuid, permissions,
                principal.getName(), now, now);
        member.setVisibleFrom(visibleFrom);
        member.setVisibleTo(visibleTo);
        return memberRepository.save(member);
    }

    public LogbookMember updateMember(String logbook, String infnUuid,
                                      Set<LogbookPermission> permissions, Principal principal) {
        return updateMember(logbook, infnUuid, permissions, null, null, principal);
    }

    public LogbookMember updateMember(String logbook, String infnUuid,
                                      Set<LogbookPermission> permissions, String visibleFrom, String visibleTo,
                                      Principal principal) {
        requireAdmin(logbook, principal);
        validateCollaborator(infnUuid, permissions);
        validatePeriod(visibleFrom, visibleTo);
        LogbookMember member = memberRepository.find(logbook, infnUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The user is not a logbook member"));
        if (member.getPermissions().contains(LogbookPermission.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The logbook owner cannot be modified");
        }
        member.setPermissions(EnumSet.copyOf(permissions));
        member.setVisibleFrom(visibleFrom);
        member.setVisibleTo(visibleTo);
        member.setUpdatedAt(Instant.now().toString());
        return memberRepository.save(member);
    }

    public void removeMember(String logbook, String infnUuid, Principal principal) {
        requireAdmin(logbook, principal);
        LogbookMember member = memberRepository.find(logbook, infnUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The user is not a logbook member"));
        if (member.getPermissions().contains(LogbookPermission.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The logbook owner cannot be removed");
        }
        memberRepository.delete(logbook, infnUuid);
    }

    public void requireAdmin(String logbook, Principal principal) {
        Logbook persistedLogbook = logbookRepository.findById(logbook)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The logbook does not exist"));
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        boolean owner = principal.getName().equals(persistedLogbook.getOwner());
        boolean membershipAdmin = memberRepository.find(logbook, principal.getName())
                .map(LogbookMember::getPermissions)
                .map(p -> p.contains(LogbookPermission.ADMIN))
                .orElse(false);
        if (!owner && !membershipAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the logbook administrator can manage collaborators");
        }
    }

    public LogbookPermissions permissionsFor(String logbook, Principal principal) {
        Logbook persistedLogbook = logbookRepository.findById(logbook)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The logbook does not exist"));
        requireAuthenticated(principal);

        Set<LogbookPermission> permissions;
        if (principal.getName().equals(persistedLogbook.getOwner())) {
            permissions = EnumSet.allOf(LogbookPermission.class);
        } else {
            permissions = memberRepository.find(logbook, principal.getName())
                    .map(LogbookMember::getPermissions)
                    .map(value -> value.isEmpty()
                            ? EnumSet.noneOf(LogbookPermission.class)
                            : EnumSet.copyOf(value))
                    .orElseGet(() -> EnumSet.noneOf(LogbookPermission.class));
            permissions.addAll(authenticatedPermissions);
            if (permissions.isEmpty() && isServiceReader(principal)) {
                permissions = EnumSet.of(LogbookPermission.READ);
            }
        }
        return new LogbookPermissions(logbook, principal.getName(), permissions,
                permissions.contains(LogbookPermission.ADMIN));
    }

    public List<LogbookPermissions> permissionsForCurrentUser(Principal principal) {
        requireAuthenticated(principal);
        List<LogbookPermissions> result = new ArrayList<>();
        logbookRepository.findAll().forEach(logbook -> {
            LogbookPermissions permissions = permissionsFor(logbook.getName(), principal);
            if (!permissions.permissions().isEmpty()) {
                result.add(permissions);
            }
        });
        return result;
    }

    public void requirePermission(Collection<String> logbooks, LogbookPermission permission,
                                  Principal principal) {
        requireAuthenticated(principal);
        for (String logbook : logbooks) {
            LogbookPermissions effective = permissionsFor(logbook, principal);
            if (!effective.permissions().contains(permission)
                    && !effective.permissions().contains(LogbookPermission.ADMIN)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Missing " + permission + " permission for logbook " + logbook);
            }
        }
    }

    public void deleteMemberships(String logbook) {
        memberRepository.deleteByLogbook(logbook);
    }

    private void validateCollaborator(String infnUuid, Set<LogbookPermission> permissions) {
        if (infnUuid == null || infnUuid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "infnUuid is required");
        }
        if (permissions == null || permissions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one permission is required");
        }
        if (permissions.contains(LogbookPermission.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ADMIN cannot be assigned through the collaborators API");
        }
    }

    private void requireAuthenticated(Principal principal) {
        if (principal == null || principal instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private void validatePeriod(String from, String to) {
        try {
            LocalDate start = from == null ? null : LocalDate.parse(from);
            LocalDate end = to == null ? null : LocalDate.parse(to);
            if (start != null && end != null && start.isAfter(end)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "visibleFrom must not be after visibleTo");
            }
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Visibility dates must use YYYY-MM-DD");
        }
    }

    private Principal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        requireAuthenticated(authentication);
        if (!authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return authentication;
    }

    public void requireReadable(Log log) {
        requireReadable(log, currentPrincipal());
    }

    public void requireReadable(Log log, Principal principal) {
        requireAuthenticated(principal);
        if (isServiceReader(principal) || everyoneReads()) {
            return;
        }
        boolean readable = log.getLogbooks().stream().anyMatch(book -> {
            LogbookPermissions access = permissionsFor(book.getName(), principal);
            if (access.admin()) return true;
            if (access.permissions().isEmpty()) return false;
            LogbookMember member = memberRepository.find(book.getName(), principal.getName()).orElse(null);
            if (member == null) return false;
            if (member.getVisibleFrom() == null && member.getVisibleTo() == null) return true;
            if (log.getCreatedDate() == null) return false;
            LocalDate date = log.getCreatedDate().atZone(ZoneOffset.UTC).toLocalDate();
            return (member.getVisibleFrom() == null || !date.isBefore(LocalDate.parse(member.getVisibleFrom())))
                    && (member.getVisibleTo() == null || !date.isAfter(LocalDate.parse(member.getVisibleTo())));
        });
        if (!readable) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to view this log entry or entry outside the permitted period");
        }
    }

    public Query readFilter() {
        return readFilter(currentPrincipal());
    }

    /** OR of per-logbook grants, applied before pagination and counting. */
    public Query readFilter(Principal principal) {
        requireAuthenticated(principal);
        if (isServiceReader(principal) || everyoneReads()) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        List<Query> grants = new ArrayList<>();
        for (LogbookPermissions access : permissionsForCurrentUser(principal)) {
            Query book = Query.of(q -> q.nested(n -> n.path("logbooks")
                    .query(c -> c.term(t -> t.field("logbooks.name").value(access.logbook())))));
            LogbookMember member = access.admin() ? null : memberRepository.find(access.logbook(), principal.getName()).orElse(null);
            if (!access.admin() && member == null) continue;
            if (member == null || (member.getVisibleFrom() == null && member.getVisibleTo() == null)) {
                grants.add(book);
            } else {
                UntypedRangeQuery.Builder dates = new UntypedRangeQuery.Builder().field("createdDate");
                if (member.getVisibleFrom() != null) {
                    dates.gte(JsonData.of(LocalDate.parse(member.getVisibleFrom()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()));
                }
                if (member.getVisibleTo() != null) {
                    dates.lt(JsonData.of(LocalDate.parse(member.getVisibleTo()).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()));
                }
                grants.add(Query.of(q -> q.bool(b -> b.filter(book, RangeQuery.of(r -> r.untyped(dates.build()))._toQuery()))));
            }
        }
        return grants.isEmpty() ? Query.of(q -> q.matchNone(m -> m))
                : Query.of(q -> q.bool(b -> b.should(grants).minimumShouldMatch("1")));
    }
}
