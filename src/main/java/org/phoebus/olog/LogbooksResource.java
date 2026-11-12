/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.LogbookPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;

import java.security.Principal;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.phoebus.olog.OlogResourceDescriptors.LOGBOOK_RESOURCE_URI;

/**
 * Resource for handling the requests to ../logbooks
 *
 * @author kunal
 *
 */
@RestController
@RequestMapping(LOGBOOK_RESOURCE_URI)
@io.swagger.v3.oas.annotations.tags.Tag(name = "Logbooks")
public class LogbooksResource {

    private final Logger log = Logger.getLogger(LogbooksResource.class.getName());

    @Autowired
    private LogbookRepository logbookRepository;

    @Autowired
    private LogbookAccessService logbookAccessService;

    /**
     * Creates a new instance of LogbooksResource
     */
    public LogbooksResource() {
    }

    @GetMapping
    @Operation(summary = "Get all logbooks", operationId = "getAllLogbooks")
    public Iterable<Logbook> findAll() {
        return logbookRepository.findAll();
    }

    @SuppressWarnings("unused")
    @GetMapping("/{logbookName}")
    @Operation(summary = "Get a logbook by name", operationId = "findLogbookByName")
    public Logbook findByName(@PathVariable(name = "logbookName") String logbookName) {
        Optional<Logbook> foundLogbook = logbookRepository.findById(logbookName);
        if (foundLogbook.isPresent()) {
            return foundLogbook.get();
        } else {
            String message = MessageFormat.format(TextUtil.LOGBOOK_NOT_FOUND, logbookName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    @GetMapping("/{logbookName}/permissions/me")
    @Operation(summary = "Get the current user's permissions on a logbook", operationId = "findMyLogbookPermissions")
    public LogbookPermissions findMyPermissions(@PathVariable(name = "logbookName") String logbookName,
                                                @AuthenticationPrincipal Principal principal) {
        return logbookAccessService.permissionsFor(logbookName, principal);
    }

    @GetMapping("/permissions/me")
    @Operation(summary = "Get the current user's permissions on all accessible logbooks", operationId = "findAllMyLogbookPermissions")
    public List<LogbookPermissions> findMyPermissions(@AuthenticationPrincipal Principal principal) {
        return logbookAccessService.permissionsForCurrentUser(principal);
    }

    @SuppressWarnings("unused")
    @PutMapping("/{logbookName}")
    @Operation(summary = "Create a logbook")
    public Logbook createLogbook(@PathVariable(name = "logbookName") String logbookName,
                                 @RequestBody final Logbook logbook,
                                 @AuthenticationPrincipal Principal principal) {
        // Validate request parameters
        validateLogbookRequest(logbook);
        if (!logbookName.equals(logbook.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Logbook name does not match the path");
        }

        // check if present
        Optional<Logbook> existingLogbook = logbookRepository.findById(logbookName);
        if (existingLogbook.isPresent()) {
            logbookAccessService.requireAdmin(logbookName, principal);
            logbook.setOwner(existingLogbook.get().getOwner());
            // delete existing logbook
            logbookRepository.deleteById(logbookName);
        } else {
            logbook.setOwner(principal.getName());
        }

        // create new logbook
        Logbook savedLogbook = logbookRepository.save(logbook);
        if (existingLogbook.isEmpty()) {
            logbookAccessService.createOwnerMembership(logbookName, principal);
        }
        return savedLogbook;
    }

    @SuppressWarnings("unused")
    @PutMapping
    @Operation(summary = "Create a list of logbooks", operationId = "createLogbooks")
    public Iterable<Logbook> updateLogbooks(@RequestBody final List<Logbook> logbooks,
                                            @AuthenticationPrincipal Principal principal) {
        // Validate request parameters
        validateLogbookRequest(logbooks);

        // delete existing logbooks
        Set<String> newLogbooks = new HashSet<>();
        for (Logbook logbook : logbooks) {
            Optional<Logbook> existingLogbook = logbookRepository.findById(logbook.getName());
            if (existingLogbook.isPresent()) {
                logbookAccessService.requireAdmin(logbook.getName(), principal);
                logbook.setOwner(existingLogbook.get().getOwner());
                // delete existing tag
                logbookRepository.deleteById(logbook.getName());
            } else {
                logbook.setOwner(principal.getName());
                newLogbooks.add(logbook.getName());
            }
        }

        // create new logbooks
        Iterable<Logbook> savedLogbooks = logbookRepository.saveAll(logbooks);
        for (Logbook logbook : logbooks) {
            if (newLogbooks.contains(logbook.getName())) {
                logbookAccessService.createOwnerMembership(logbook.getName(), principal);
            }
        }
        return savedLogbooks;
    }

    @SuppressWarnings("unused")
    @DeleteMapping("/{logbookName}")
    @Operation(summary = "Delete a logbook by name")
    public void deleteLogbook(@PathVariable(name = "logbookName") String logbookName,
                              @AuthenticationPrincipal Principal principal) {

        // check if present
        Optional<Logbook> existingLogbook = logbookRepository.findById(logbookName);
        if (existingLogbook.isPresent()) {
            logbookAccessService.requireAdmin(logbookName, principal);
            // delete existing logbook
            logbookRepository.deleteById(logbookName);
            logbookAccessService.deleteMemberships(logbookName);
        } else {
            String message = MessageFormat.format(TextUtil.LOGBOOK_NOT_EXISTS, logbookName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    /**
     * Checks if all the logbooks included satisfy the following conditions:
     *
     * <ol>
     * <li> the logbook names are not null or empty
     * <li> no validation for logbook owners
     * <li> no validation for logbook states
     * </ol>
     *
     * @param logbooks the logbooks to be validated
     */
    public void validateLogbookRequest(Iterable<Logbook> logbooks) {
        for (Logbook logbook : logbooks) {
            validateLogbookRequest(logbook);
        }
    }

    /**
     * Checks if the logbook satisfies the following conditions:
     *
     * <ol>
     * <li> the logbook name is not null or empty
     * <li> no validation for logbook owner
     * <li> no validation for logbook state
     * </ol>
     *
     * @param logbook the logbook to be validated
     */
    public void validateLogbookRequest(Logbook logbook) {
        if (logbook.getName() == null || logbook.getName().isEmpty()) {
            String message = MessageFormat.format(TextUtil.LOGBOOK_NAME_CANNOT_BE_NULL_OR_EMPTY, logbook.toString());
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, null);
        }
    }

}
