/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

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

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.phoebus.olog.OlogResourceDescriptors.LEVEL_RESOURCE_RUI;

/**
 * Resource for handling the requests to ../levels
 *
 * @author Georg Weiss
 */
@RestController
@RequestMapping(LEVEL_RESOURCE_RUI)
public class LevelsResource {

    private final Logger log = Logger.getLogger(LevelsResource.class.getName());

    @Autowired
    private LevelRepository levelRepository;

    /**
     * GET method for retrieving the list of levels in the database.
     *
     * @return list of {@link org.phoebus.olog.entity.Level}s
     */
    @GetMapping
    public Iterable<org.phoebus.olog.entity.Level> findAll() {
        return levelRepository.findAll();
    }

    /**
     * Get method for retrieving the level with name matching levelName
     *
     * @param levelName - the name of the level to be retrieved
     * @return the matching {@link org.phoebus.olog.entity.Level}. If not
     * found, HTTP 404 reponse is triggered.
     */
    @GetMapping("/{levelName}")
    public org.phoebus.olog.entity.Level findByTitle(@PathVariable String levelName) {
        Optional<org.phoebus.olog.entity.Level> foundTag = levelRepository.findById(levelName);
        if (foundTag.isPresent()) {
            return foundTag.get();
        } else {
            String message = MessageFormat.format(TextUtil.LEVEL_NOT_FOUND, levelName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    /**
     * PUT method for creating a {@link org.phoebus.olog.entity.Level}.
     *
     * @param levelName - the name of the tag to be created
     * @param level     - the {@link org.phoebus.olog.entity.Level} object with owner and state information
     * @return the created tag
     */
    @PutMapping("/{levelName}")
    public org.phoebus.olog.entity.Level createLevel(@PathVariable String levelName, @RequestBody final org.phoebus.olog.entity.Level level) {

        // Validate request parameters
        validateLevelRequest(level);

        // check if present
        Optional<org.phoebus.olog.entity.Level> existingTag =
                levelRepository.findById(levelName);
        if (existingTag.isPresent()) {
            // delete existing tag
            levelRepository.deleteById(levelName);
        }

        // create new level
        return levelRepository.save(level);
    }

    /**
     * PUT method for the level resource to support the creation of a list of levels
     *
     * @param levels - the list of levels to be created
     * @return the list of levels created
     */
    @PutMapping
    public Iterable<org.phoebus.olog.entity.Level> updateTag(@RequestBody final List<org.phoebus.olog.entity.Level> levels) {

        // Validate request parameters
        validateLevelsRequest(levels);

        // delete existing levels
        for (org.phoebus.olog.entity.Level level : levels) {
            if (levelRepository.existsById(level.name())) {
                // delete existing tag
                levelRepository.deleteById(level.name());
            }
        }

        // create new tags
        return levelRepository.saveAll(levels);
    }

    @DeleteMapping("/{levelName}")
    public void deleteTag(@PathVariable String levelName) {
        // TODO Check permissions

        // check if present
        Optional<org.phoebus.olog.entity.Level> existingLevel = levelRepository.findById(levelName);
        if (existingLevel.isPresent()) {
            // delete existing level
            levelRepository.deleteById(levelName);
        } else {
            String message = MessageFormat.format(TextUtil.LEVEL_EXISTS_FAILED, levelName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    /**
     * @param levels List of {@link org.phoebus.olog.entity.Level}s
     */
    public void validateLevelsRequest(List<org.phoebus.olog.entity.Level> levels) {
        levels.forEach(this::validateLevelRequest);
    }

    /**
     * Validates a {@link org.phoebus.olog.entity.Level}: name must be non-empty. If
     * {@link org.phoebus.olog.entity.Level#defaultLevel()} is true, then no other
     * exsting {@link org.phoebus.olog.entity.Level} must be flagged as default.
     *
     * @param level {@link org.phoebus.olog.entity.Level} to add.
     */
    public void validateLevelRequest(org.phoebus.olog.entity.Level level) {
        if (level.name() == null || level.name().isEmpty()) {
            String message = TextUtil.LEVEL_NAME_CANNOT_BE_NULL_OR_EMPTY;
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, null);
        }
        Iterable<org.phoebus.olog.entity.Level> existing =
                levelRepository.findAll();
        while (existing.iterator().hasNext()) {
            org.phoebus.olog.entity.Level l = existing.iterator().next();
            if (l.defaultLevel() && level.defaultLevel()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        MessageFormat.format(TextUtil.DEFAULT_LEVEL_ALREADY_EXISTS, l.name()));
            }
        }
    }
}
