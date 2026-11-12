package org.phoebus.olog;

import org.phoebus.olog.entity.LogbookMember;
import org.phoebus.olog.entity.LogbookMemberRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

import static org.phoebus.olog.OlogResourceDescriptors.LOGBOOK_RESOURCE_URI;

@RestController
@RequestMapping(LOGBOOK_RESOURCE_URI + "/{logbookName}/members")
public class LogbookMembersResource {

    private final LogbookAccessService accessService;

    public LogbookMembersResource(LogbookAccessService accessService) {
        this.accessService = accessService;
    }

    @GetMapping
    public List<LogbookMember> findMembers(@PathVariable("logbookName") String logbookName,
                                          Principal principal) {
        return accessService.findMembers(logbookName, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LogbookMember addMember(@PathVariable("logbookName") String logbookName,
                                   @RequestBody LogbookMemberRequest request,
                                   Principal principal) {
        return accessService.addMember(logbookName, request.infnUuid(), request.permissions(),
                request.visibleFrom(), request.visibleTo(), principal);
    }

    @PutMapping("/{infnUuid}")
    public LogbookMember updateMember(@PathVariable("logbookName") String logbookName,
                                      @PathVariable("infnUuid") String infnUuid,
                                      @RequestBody LogbookMemberRequest request,
                                      Principal principal) {
        if (request.infnUuid() != null && !infnUuid.equals(request.infnUuid())) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "infnUuid does not match the path");
        }
        return accessService.updateMember(logbookName, infnUuid, request.permissions(),
                request.visibleFrom(), request.visibleTo(), principal);
    }

    @DeleteMapping("/{infnUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable("logbookName") String logbookName,
                             @PathVariable("infnUuid") String infnUuid,
                             Principal principal) {
        accessService.removeMember(logbookName, infnUuid, principal);
    }
}
