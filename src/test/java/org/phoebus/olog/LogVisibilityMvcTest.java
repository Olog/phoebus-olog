package org.phoebus.olog;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.phoebus.olog.entity.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LogVisibilityMvcTest {
    MockMvc mvc;
    Logbook book = new Logbook("operations", "owner");
    LogRepository logs;
    LogbookMemberRepository members;

    @BeforeEach void setup() {
        LogbookRepository books = mock(LogbookRepository.class);
        members = mock(LogbookMemberRepository.class);
        logs = mock(LogRepository.class);
        when(books.findById("operations")).thenReturn(Optional.of(book));
        LogbookMember member = new LogbookMember("operations", "reader", Set.of(LogbookPermission.READ), "owner", "now", "now");
        member.setVisibleFrom("2026-09-01");
        member.setVisibleTo("2026-09-10");
        when(members.find("operations", "reader")).thenReturn(Optional.of(member));
        for (int id = 1; id <= 2; id++) {
            Log log = Log.LogBuilder.createLog().id((long) id).withLogbooks(Set.of(book))
                    .createDate(Instant.parse(id == 1 ? "2026-09-05T12:00:00Z" : "2026-09-11T00:00:00Z")).build();
            when(logs.findById(Integer.toString(id))).thenReturn(Optional.of(log));
        }
        LogResource resource = new LogResource();
        ReflectionTestUtils.setField(resource, "logRepository", logs);
        LogbookAccessService access = new LogbookAccessService(books, members);
        ReflectionTestUtils.setField(resource, "logbookAccessService", access);
        when(members.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        mvc = MockMvcBuilders.standaloneSetup(resource, new LogbookMembersResource(access)).build();
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    void login(String name) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(name, "", List.of()));
    }

    @Test void directEntryRespectsPeriod() throws Exception {
        login("reader");
        mvc.perform(get("/Olog/logs/1")).andExpect(status().isOk());
        mvc.perform(get("/Olog/logs/2")).andExpect(status().isForbidden());
    }

    @Test void archivedAndAttachmentPathsCannotBypassPeriod() throws Exception {
        login("reader");
        mvc.perform(get("/Olog/logs/archived/2")).andExpect(status().isForbidden());
        mvc.perform(get("/Olog/logs/attachments/2/file.txt")).andExpect(status().isForbidden());
        verify(logs, never()).findArchivedById(any());
    }

    @Test void anonymousAndUnrelatedUserCannotRead() throws Exception {
        mvc.perform(get("/Olog/logs/1")).andExpect(status().isUnauthorized());
        login("other");
        mvc.perform(get("/Olog/logs/1")).andExpect(status().isForbidden());
    }

    @Test void ownerCanReadOutsidePeriod() throws Exception {
        login("owner");
        mvc.perform(get("/Olog/logs/2")).andExpect(status().isOk());
    }

    @Test void ownerCanCreateAndClearPeriodViaApi() throws Exception {
        mvc.perform(post("/Olog/logbooks/operations/members").principal(() -> "owner")
                .contentType("application/json").content("{\"infnUuid\":\"new-reader\",\"permissions\":[\"READ\"],\"visibleFrom\":\"2026-09-01\",\"visibleTo\":\"2026-09-10\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.visibleFrom").value("2026-09-01"))
                .andExpect(jsonPath("$.visibleTo").value("2026-09-10"));
        mvc.perform(put("/Olog/logbooks/operations/members/reader").principal(() -> "owner")
                .contentType("application/json").content("{\"permissions\":[\"READ\"],\"visibleFrom\":null,\"visibleTo\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.visibleFrom").isEmpty()).andExpect(jsonPath("$.visibleTo").isEmpty());
    }

    @Test void invalidRangeAndNonAdminUpdatesAreRejected() throws Exception {
        String body = "{\"permissions\":[\"READ\"],\"visibleFrom\":\"2026-09-12\",\"visibleTo\":\"2026-09-01\"}";
        mvc.perform(put("/Olog/logbooks/operations/members/reader").principal(() -> "owner").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/Olog/logbooks/operations/members/reader").principal(() -> "reader").contentType("application/json").content(body))
                .andExpect(status().isForbidden());
    }
}
