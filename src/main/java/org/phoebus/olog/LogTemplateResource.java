/**
 * Copyright (C) 2025 European Spallation Source ERIC.
 */
package org.phoebus.olog;

import org.apache.commons.collections4.CollectionUtils;
import org.phoebus.olog.entity.LogTemplate;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.Tag;
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

import java.security.Principal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.olog.OlogResourceDescriptors.LOG_TEMPLATE_RESOURCE_URI;

/**
 * Resource for handling the requests to ../templates
 *
 * @author georgweiss
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
    @SuppressWarnings("unused")
    @Autowired
    private PropertyRepository propertyRepository;


    @GetMapping("{logTemplateId}")
    @SuppressWarnings("unused")
    public LogTemplate getLogTemplateById(@PathVariable(name = "logTemplateId") String logTemplateId) {
        return logTemplateRepository.findById(logTemplateId).get();
    }

    /**
     * Creates a new {@link LogTemplate}.
     *
     * @param logTemplate A {@link LogTemplate} object to be persisted.
     * @param principal   The authenticated {@link Principal} of the request.
     * @return The persisted {@link LogTemplate} object.
     */
    @SuppressWarnings("unused")
    @PutMapping()
    public LogTemplate createLogTemplate(@RequestBody LogTemplate logTemplate,
                                         @AuthenticationPrincipal Principal principal) {

        // Check if there is template with same case-insensitive name
        Iterator<LogTemplate> iterator = logTemplateRepository.findAll().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getName().toLowerCase().equals(logTemplate.getName().trim().toLowerCase())) {
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

        // Check that template contains valid properties and attributes. Take advantage of Property#equals().
        Set<Property> properties = logTemplate.getProperties();
        if(properties != null && !properties.isEmpty()){
            Set<Property> persistedProperties = new HashSet<>();
            propertyRepository.findAll().forEach(p -> persistedProperties.add(p));
            if (!CollectionUtils.containsAll(persistedProperties, properties)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.LOG_INVALID_PROPERTIES);
            }
        }

        LogTemplate newLogTemplate = logTemplateRepository.save(logTemplate);
        logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.LOG_TEMPLATE_CREATED, logTemplate.getName(), newLogTemplate.getId()));
        return newLogTemplate;
    }

    /**
     * Delete a {@link LogTemplate} based on its unique id.
     * @param logTemplateId Unique id
     */
    @DeleteMapping("/{logTemplateId}")
    public void deleteLogTemplate(@PathVariable(name = "logTemplateId") String logTemplateId){
        logTemplateRepository.deleteById(logTemplateId);
    }

    /**
     * @return A potentially empty {@link List} of all existing {@link LogTemplate}s.
     */
    @SuppressWarnings("unused")
    @GetMapping
    public List<LogTemplate> getAllTemplates() {
        List<LogTemplate> allTemplates = new ArrayList<>();
        logTemplateRepository.findAll().forEach(allTemplates::add);
        return allTemplates;
    }
}
