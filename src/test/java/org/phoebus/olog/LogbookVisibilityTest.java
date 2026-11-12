package org.phoebus.olog;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogbookVisibilityTest {
    LogbookRepository books;
    LogbookMemberRepository members;
    LogbookAccessService service;
    Logbook book = new Logbook("operations", "owner");
    Principal reader = () -> "reader";
    LogbookMember member;

    @BeforeEach void setup() {
        books = mock(LogbookRepository.class);
        members = mock(LogbookMemberRepository.class);
        service = new LogbookAccessService(books, members);
        when(books.findById("operations")).thenReturn(Optional.of(book));
        when(books.findAll()).thenReturn(List.of(book));
        member = new LogbookMember("operations", "reader", EnumSet.of(LogbookPermission.READ), "owner", "now", "now");
        member.setVisibleFrom("2026-09-01");
        member.setVisibleTo("2026-09-10");
        when(members.find("operations", "reader")).thenReturn(Optional.of(member));
        when(members.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    Log entry(String instant) {
        return Log.LogBuilder.createLog().id(1L).withLogbooks(Set.of(book)).createDate(Instant.parse(instant)).build();
    }

    @Test void bothBoundaryDaysAreIncluded() {
        assertDoesNotThrow(() -> service.requireReadable(entry("2026-09-01T00:00:00Z"), reader));
        assertDoesNotThrow(() -> service.requireReadable(entry("2026-09-10T23:59:59.999Z"), reader));
        denied("2026-08-31T23:59:59.999Z", reader, HttpStatus.FORBIDDEN);
        denied("2026-09-11T00:00:00Z", reader, HttpStatus.FORBIDDEN);
    }

    @Test void absentAndOneSidedDates() {
        member.setVisibleFrom(null);
        assertDoesNotThrow(() -> service.requireReadable(entry("2000-01-01T00:00:00Z"), reader));
        denied("2027-01-01T00:00:00Z", reader, HttpStatus.FORBIDDEN);
        member.setVisibleFrom("2026-09-01");
        member.setVisibleTo(null);
        assertDoesNotThrow(() -> service.requireReadable(entry("2030-01-01T00:00:00Z"), reader));
        member.setVisibleFrom(null);
        assertDoesNotThrow(() -> service.requireReadable(entry("2000-01-01T00:00:00Z"), reader));
    }

    @Test void ownerAndMembershipAdminHaveFullAccess() {
        assertDoesNotThrow(() -> service.requireReadable(entry("2000-01-01T00:00:00Z"), () -> "owner"));
        member.setPermissions(EnumSet.of(LogbookPermission.ADMIN));
        assertDoesNotThrow(() -> service.requireReadable(entry("2000-01-01T00:00:00Z"), reader));
    }

    @Test void anonymousAndNonMemberAreDenied() {
        denied("2026-09-05T00:00:00Z", null, HttpStatus.UNAUTHORIZED);
        denied("2026-09-05T00:00:00Z", () -> "stranger", HttpStatus.FORBIDDEN);
        assertTrue(service.readFilter(() -> "stranger").isMatchNone());
    }

    @Test void writerIsAlsoRestricted() {
        member.setPermissions(EnumSet.of(LogbookPermission.WRITE));
        assertDoesNotThrow(() -> service.requireReadable(entry("2026-09-05T00:00:00Z"), reader));
        denied("2026-09-11T00:00:00Z", reader, HttpStatus.FORBIDDEN);
    }

    @Test void globalGroupDoesNotBypassLogbookGrant() {
        Principal groupMember = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "group-member", "", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        denied("2026-09-05T00:00:00Z", groupMember, HttpStatus.FORBIDDEN);
    }

    @Test void searchUsesPerBookRangeAndExclusiveNextDay() {
        var filter = service.readFilter(reader).bool().should().get(0).bool().filter();
        assertEquals("operations", filter.get(0).nested().query().term().value().stringValue());
        assertEquals(Instant.parse("2026-09-01T00:00:00Z").toEpochMilli(), filter.get(1).range().untyped().gte().to(Long.class));
        assertEquals(Instant.parse("2026-09-11T00:00:00Z").toEpochMilli(), filter.get(1).range().untyped().lt().to(Long.class));
    }

    @Test void periodsAreValidatedAndCanBeRemoved() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.updateMember("operations", "reader", member.getPermissions(), "2026-09-12", "2026-09-01", () -> "owner")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.updateMember("operations", "reader", member.getPermissions(), "not-a-date", null, () -> "owner")).getStatusCode());
        var saved = service.updateMember("operations", "reader", member.getPermissions(), null, null, () -> "owner");
        assertNull(saved.getVisibleFrom());
        assertNull(saved.getVisibleTo());
    }

    private void denied(String instant, Principal principal, HttpStatus expected) {
        assertEquals(expected, assertThrows(ResponseStatusException.class,
                () -> service.requireReadable(entry(instant), principal)).getStatusCode());
    }
}
