/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import org.apache.commons.collections4.CollectionUtils;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.LogTemplate;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.olog.OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI;

/**
 * Resource for handling the requests to ../logs
 *
 * @author kunal
 */
@RestController
@RequestMapping(LOG_TEMPLATE_RESOURCE_URI)
public class LogTemplateResource {
    private final Logger logger = Logger.getLogger(LogTemplateResource.class.getName());

    @Autowired
    LogTemplateRepository logTemplateRepository;
    @SuppressWarnings("unused")
    @Autowired
    private LogbookRepository logbookRepository;
    @SuppressWarnings("unused")
    @Autowired
    private TagRepository tagRepository;

    @GetMapping("{logTemplateId}")
    @SuppressWarnings("unused")
    public LogTemplate getLogTemplate(@PathVariable String logTemplateId) {
        return logTemplateRepository.findById(logTemplateId).get();
    }

    /**
     * Creates a new log entry. If the <code>inReplyTo</code> parameters identifies an existing log entry,
     * this method will treat the new log entry as a reply.
     * <p>
     * This may return a HTTP 400 if for instance <code>inReplyTo</code> does not identify an existing log entry,
     * or if the logbooks listed in the {@link Log} object contains invalid (i.e. non-existing) logbooks.
     * </p>
     * <p>Client calling this endpoint <b>must</b> set Content-Type=multipart/form-data.</p>
     *
     * @param logTemplate A {@link LogTemplate} object to be persisted.
     * @param principal   The authenticated {@link Principal} of the request.
     * @return The persisted {@link Log} object.
     */
    @SuppressWarnings("unused")
    @PutMapping()
    public LogTemplate createLogTemplate(@RequestBody LogTemplate logTemplate,
                                         @AuthenticationPrincipal Principal principal) {

        // Check if there is template with same case-insensitive name
        Iterable<LogTemplate> iterable = logTemplateRepository.findAll();
        while (iterable.iterator().hasNext()) {
            if (iterable.iterator().next().getName().equals(logTemplate.getName().trim().toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template with name \"" + logTemplate.getName() + "\" already exists");
            }
        }
        logTemplate.setOwner(principal.getName());

        // If template specifies tags and properties, check that they actually exist
        Set<String> logbookNames = logTemplate.getLogbooks().stream().map(Logbook::getName).collect(Collectors.toSet());
        Set<String> persistedLogbookNames = new HashSet<>();
        logbookRepository.findAll().forEach(l -> persistedLogbookNames.add(l.getName()));
        if (!CollectionUtils.containsAll(persistedLogbookNames, logbookNames)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.LOG_INVALID_LOGBOOKS);
        }
        Set<Tag> tags = logTemplate.getTags();
        if (tags != null && !tags.isEmpty()) {
            Set<String> tagNames = tags.stream().map(Tag::getName).collect(Collectors.toSet());
            Set<String> persistedTags = new HashSet<>();
            tagRepository.findAll().forEach(t -> persistedTags.add(t.getName()));
            if (!CollectionUtils.containsAll(persistedTags, tagNames)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.LOG_INVALID_TAGS);
            }
        }
        LogTemplate newLogTemplate = logTemplateRepository.save(logTemplate);
        logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.LOG_TEMPLATE_CREATED, newLogTemplate.getId(), logTemplate.getName()));
        return newLogTemplate;
    }


    /**
     * Updates existing log template. Data sent by client is saved, i.e. if client specifies a shorter list
     * of logbooks or tags, the updated log record will reflect that. However, the following data is NOT updated:
     * <ul>
     *     <li>Created date</li>
     * </ul>
     *
     * @param logTemplateId The id of the template subject to update. It must exist, i.e. it is not created of not found.
     * @param markup        Markup strategy, if any.
     * @param logTemplate   The log template data as sent by client.
     * @param principal     The authenticated {@link Principal} of the request.
     * @return The updated log record, or HTTP status 404 if the log template does not exist. If the path
     * variable does not match the id in the log record, HTTP status 400 (bad request) is returned.
     */
    /*
    @SuppressWarnings("unused")
    @PostMapping("/{logTemplateId}")
    public Log updateLog(@PathVariable String logTemplateId,
                         @RequestParam(value = "markup", required = false) String markup,
                         @RequestBody LogTemplate logTemplate,
                         @AuthenticationPrincipal Principal principal) {

        // In case a client sends a log template record where the id does not match the path variable, return HTTP 400 (bad request)
        if (!logTemplateId.equals(Long.toString(logTemplate.getId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.LOG_TEMPLATE_NOT_MATCH_PATH);
        }

        Optional<LogTemplate> foundLogTemplate = logTemplateRepository.findById(logTemplateId);

        LogTemplate persistedLogTemplate = foundLogTemplate.get();
        persistedLogTemplate.setName(logTemplate.getName());
        persistedLogTemplate.setOwner(principal.getName());
        persistedLogTemplate.setLevel(logTemplate.getLevel());
        persistedLogTemplate.setProperties(logTemplate.getProperties());
        persistedLogTemplate.setModifyDate(Instant.now());
        persistedLogTemplate.setDescription(logTemplate.getDescription());   // to make it work with old clients where description field is sent instead of source
        persistedLogTemplate.setTags(logTemplate.getTags());
        persistedLogTemplate.setLogbooks(logTemplate.getLogbooks());
        persistedLogTemplate.setTitle(logTemplate.getTitle());

        return logTemplateRepository.update(persistedLogTemplate);

    }

     */

    /**
     * @return A potentially empty {@link List} of all existing {@link LogTemplate}s.
     */
    @SuppressWarnings("unused")
    @GetMapping
    public List<LogTemplate> getAllTemplates() {
        List<LogTemplate> allTemplates = new ArrayList();
        logTemplateRepository.findAll().forEach(allTemplates::add);
        return allTemplates;
    }
}
