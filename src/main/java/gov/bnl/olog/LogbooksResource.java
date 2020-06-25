/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.LOGBOOK_RESOURCE_URI;

import java.security.Principal;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.bnl.olog.entity.Logbook;

/**
 * Resource for handling the requests to ../logbooks
 * @author kunal
 *
 */
@RestController
@RequestMapping(LOGBOOK_RESOURCE_URI)
public class LogbooksResource {

    @Autowired
    private LogbookRepository logbookRepository;

    private static Logger audit = Logger.getLogger(LogbooksResource.class.getName() + ".audit");
    static Logger log = Logger.getLogger(LogbooksResource.class.getName());

    /** Creates a new instance of LogbooksResource */
    public LogbooksResource() {
    }

    @GetMapping
    public Iterable<Logbook> findAll() {
        return logbookRepository.findAll();
    }

    @GetMapping("/{logbookName}")
    public Logbook findByTitle(@PathVariable String logbookName) {
        return logbookRepository.findById(logbookName).orElseGet(null);
    }

    @PutMapping("/{logbookName}")
    public Logbook createLogbook(@PathVariable String logbookName,
                                 @RequestBody final Logbook logbook,
                                 @AuthenticationPrincipal Principal principal) {
        // TODO Check permissions
        // TODO Validate
        logbook.setOwner(principal.getName());
        return logbookRepository.save(logbook);
    }

    @PutMapping
    public Iterable<Logbook> updateLogbooks(@RequestBody final List<Logbook> logbooks) {
        // TODO Check permissions
        // TODO Validate
        return logbookRepository.saveAll(logbooks);
    }

    @DeleteMapping("/{logbookName}")
    public void deleteLogbook (@PathVariable String logbookName) {
        logbookRepository.deleteById(logbookName);
    }

}
