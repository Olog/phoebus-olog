/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.TAG_RESOURCE_URI;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.bnl.olog.entity.Tag;

/**
 * Resource for handling the requests to ../tags
 * @author Kunal Shroff
 *
 */
@RestController
@RequestMapping(TAG_RESOURCE_URI)
public class TagsResource {

    @Autowired
    private TagRepository tagRepository;

    private Logger log = Logger.getLogger(TagsResource.class.getName());

    /** Creates a new instance of TagsResource */
    public TagsResource()
    {
    }

    /**
     * GET method for retrieving the list of tags in the database.
     *
     * @return list of tags
     */
    @GetMapping
    public Iterable<Tag> findAll()
    {
        return tagRepository.findAll();
    }

    /**
     * Get method for retrieving the tag with name matching tagName
     * 
     * @param tagName - the name of the tag to be retrieved
     * @return the matching tag, or null
     */
    @GetMapping("/{tagName}")
    public Tag findByTitle(@PathVariable String tagName)
    {
        return tagRepository.findById(tagName).orElseGet(null);
    }

    /**
     * PUT method for creating a tag.
     *
     * @param tagName - the name of the tag to be created
     * @param tag - the tag object with owner and state information
     * @return the created tag
     */
    @PutMapping("/{tagName}")
    public Tag createTag(@PathVariable String tagName, @RequestBody final Tag tag)
    {
        // TODO Check permissions
        // TODO Validate
        return tagRepository.save(tag);
    }

    /**
     * PUT method for the tags resource to support the creation of a list of tags
     * 
     * @param tags - the list of tags to be created
     * @return the list of tags created
     */
    @PutMapping
    public Iterable<Tag> updateTag(@RequestBody final List<Tag> tags)
    {
        // TODO Check permissions
        // TODO Validate
        return tagRepository.saveAll(tags);
    }

    @DeleteMapping("/{tagName}")
    public void deleteTag(@PathVariable String tagName)
    {
        tagRepository.deleteById(tagName);
    }

}
