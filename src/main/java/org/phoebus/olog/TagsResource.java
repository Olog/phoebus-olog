/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import static org.phoebus.olog.OlogResourceDescriptors.TAG_RESOURCE_URI;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resource for handling the requests to ../tags
 * @author Kunal Shroff
 *
 */
@RestController
@RequestMapping(TAG_RESOURCE_URI)
public class TagsResource {

    private Logger log = Logger.getLogger(TagsResource.class.getName());

    @Autowired
    private TagRepository tagRepository;

    /** Creates a new instance of TagsResource */
    public TagsResource() {
    }

    /**
     * GET method for retrieving the list of tags in the database.
     *
     * @return list of tags
     */
    @GetMapping
    public Iterable<Tag> findAll() {
        return tagRepository.findAll();
    }

    /**
     * Get method for retrieving the tag with name matching tagName
     *
     * @param tagName - the name of the tag to be retrieved
     * @return the matching tag, or null
     */
    @GetMapping("/{tagName}")
    public Tag findByTitle(@PathVariable(name = "tagName") String tagName) {
        Optional<Tag> foundTag = tagRepository.findById(tagName);
        if (foundTag.isPresent()) {
            return foundTag.get();
        } else {
            String message = MessageFormat.format(TextUtil.TAG_NOT_FOUND, tagName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    /**
     * PUT method for creating a tag.
     *
     * @param tagName - the name of the tag to be created
     * @param tag - the tag object with owner and state information
     * @return the created tag
     */
    @PutMapping("/{tagName}")
    public Tag createTag(@PathVariable(name = "tagName") String tagName, @RequestBody final Tag tag) {
        // TODO Check permissions
        // Validate

        // Validate request parameters
        validateTagRequest(tag);

        // check if present
        Optional<Tag> existingTag = tagRepository.findById(tagName);
        if (existingTag.isPresent()) {
            // delete existing tag
            tagRepository.deleteById(tagName);
        }

        // create new tag
        return tagRepository.save(tag);
    }

    /**
     * PUT method for the tags resource to support the creation of a list of tags
     *
     * @param tags - the list of tags to be created
     * @return the list of tags created
     */
    @PutMapping
    public Iterable<Tag> updateTag(@RequestBody final List<Tag> tags) {
        // TODO Check permissions
        // Validate

        // Validate request parameters
        validateTagRequest(tags);

        // delete existing tags
        for(Tag tag: tags) {
            if(tagRepository.existsById(tag.getName())) {
                // delete existing tag
                tagRepository.deleteById(tag.getName());
            }
        }

        // create new tags
        return tagRepository.saveAll(tags);
    }

    @DeleteMapping("/{tagName}")
    public void deleteTag(@PathVariable(name = "tagName") String tagName) {
        // TODO Check permissions

        // check if present
        Optional<Tag> existingTag = tagRepository.findById(tagName);
        if (existingTag.isPresent()) {
            // delete existing tag
            tagRepository.deleteById(tagName);
        } else {
            String message = MessageFormat.format(TextUtil.TAG_NOT_EXISTS, tagName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    /**
     * Checks if all the tags included satisfy the following conditions:
     *
     * <ol>
     * <li> the tag names are not null or empty
     * <li> no validation for tag states
     * </ol>
     *
     * @param tags the tags to be validated
     */
    public void validateTagRequest(Iterable<Tag> tags) {
        for (Tag tag : tags) {
            validateTagRequest(tag);
        }
    }

    /**
     * Checks if the tag satisfies the following conditions:
     *
     * <ol>
     * <li> the tag name is not null or empty
     * <li> no validation for tag state
     * </ol>
     *
     * @param tag the tag to be validated
     */
    public void validateTagRequest(Tag tag) {
        if (tag.getName() == null || tag.getName().isEmpty()) {
            String message = MessageFormat.format(TextUtil.TAG_NAME_CANNOT_BE_NULL_OR_EMPTY, tag.toString());
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, null);
        }
    }

}
