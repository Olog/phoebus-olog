/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import static org.phoebus.olog.OlogResourceDescriptors.LOGBOOK_RESOURCE_URI;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.State;
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

/**
 * Resource for handling the requests to ../logbooks
 * @author kunal
 *
 */
@RestController
@RequestMapping(LOGBOOK_RESOURCE_URI)
public class LogbooksResource {

    private Logger audit = Logger.getLogger(LogbooksResource.class.getName() + ".audit");
    private Logger log = Logger.getLogger(LogbooksResource.class.getName());

    @Autowired
    private LogbookRepository logbookRepository;

    /** Creates a new instance of LogbooksResource */
    public LogbooksResource() {
    }

    @GetMapping
    public Iterable<Logbook> findAll() {
        return logbookRepository.findAll();
    }

    @GetMapping("/{logbookName}")
    public Logbook findByTitle(@PathVariable String logbookName) {
        Optional<Logbook> foundLogbook = logbookRepository.findById(logbookName);
        if (foundLogbook.isPresent()) {
            return foundLogbook.get();
        } else {
            log.log(Level.SEVERE, "Failed to find logbook: " + logbookName, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find logbook: " + logbookName);
        }
    }

    @PutMapping("/{logbookName}")
    public Logbook createLogbook(@PathVariable String logbookName,
                                 @RequestBody final Logbook logbook,
                                 @AuthenticationPrincipal Principal principal) {
        // TODO Check permissions
        // Validate

        // Validate request parameters
        validateLogbookRequest(logbook);

        // check if present
        Optional<Logbook> existingLogbook = logbookRepository.findById(logbookName);
        if (existingLogbook.isPresent()) {
            // delete existing logbook
            logbookRepository.deleteById(logbookName);
        }

        logbook.setOwner(principal.getName());

        // create new logbook
        return logbookRepository.save(logbook);
    }

    @PutMapping
    public Iterable<Logbook> updateLogbooks(@RequestBody final List<Logbook> logbooks) {
        // TODO Check permissions
        // Validate

        // Validate request parameters
        validateLogbookRequest(logbooks);

        // delete existing logbooks
        for(Logbook logbook: logbooks) {
            if(logbookRepository.existsById(logbook.getName())) {
                // delete existing tag
                logbookRepository.deleteById(logbook.getName());
            }
        }

        // create new logbooks
        return logbookRepository.saveAll(logbooks);
    }

    @DeleteMapping("/{logbookName}")
    public void deleteLogbook (@PathVariable String logbookName) {
        // TODO Check permissions

        // check if present
        Optional<Logbook> existingLogbook = logbookRepository.findById(logbookName);
        if (existingLogbook.isPresent()) {
            // delete existing logbook
            logbookRepository.deleteById(logbookName);
        } else {
            log.log(Level.SEVERE, "The logbook with the name " + logbookName + " does not exist", new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The logbook with the name " + logbookName + " does not exist");
        }
    }

    /**
     * Checks if all the logbooks included satisfy the following conditions:
     *
     * <ol>
     * <li> the logbook names are not null or empty
     * <li> the logbook owners are not null or empty
     * <li> the logbook states are not null or empty, and are either Active or Inactive
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
     * <li> the logbook owner is not null or empty
     * <li> the logbook state is not null or empty, and is either Active or Inactive
     * </ol>
     *
     * @param logbook the logbook to be validated
     */
    public void validateLogbookRequest(Logbook logbook) {
        // 1
        if (logbook.getName() == null || logbook.getName().isEmpty()) {
            log.log(Level.SEVERE, "The logbook name cannot be null or empty " + logbook.toString(), new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The logbook name cannot be null or empty " + logbook.toString(), null);
        }
        // 2
        if (logbook.getOwner() == null || logbook.getOwner().isEmpty()) {
            log.log(Level.SEVERE, "The logbook owner cannot be null or empty " + logbook.toString(), new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The logbook owner cannot be null or empty " + logbook.toString(), null);
        }
        // 3
        if (logbook.getState() == null || !(State.Active.equals(logbook.getState()) || State.Inactive.equals(logbook.getState()))) {
            log.log(Level.SEVERE, "The logbook state cannot be null or empty or not Active/Inactive " + logbook.toString(), new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The logbook state cannot be null or empty or not Active/Inactive " + logbook.toString(), null);
        }
    }

}
