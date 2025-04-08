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

    @SuppressWarnings("unused")
    @Autowired
    private LevelRepository levelRepository;

    /**
     * GET method for retrieving the list of {@link org.phoebus.olog.entity.Level}s in the database.
     *
     * @return list of {@link org.phoebus.olog.entity.Level}s
     */
    @GetMapping
    public Iterable<org.phoebus.olog.entity.Level> findAll() {
        return levelRepository.findAll();
    }

    /**
     * Get method for retrieving the {@link org.phoebus.olog.entity.Level} with name matching levelName
     *
     * @param levelName - the name of the {@link org.phoebus.olog.entity.Level} to be retrieved
     * @return the matching {@link org.phoebus.olog.entity.Level}. If not
     * found, HTTP 404 response is triggered.
     */
    @SuppressWarnings("unused")
    @GetMapping("/{levelName}")
    public org.phoebus.olog.entity.Level findByName(@PathVariable String levelName) {
        Optional<org.phoebus.olog.entity.Level> foundLevel = levelRepository.findById(levelName);
        if (foundLevel.isPresent()) {
            return foundLevel.get();
        } else {
            String message = MessageFormat.format(TextUtil.LEVEL_NOT_FOUND, levelName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    /**
     * PUT method for creating a {@link org.phoebus.olog.entity.Level}.
     *
     * <p>
     *     If the specified {@link org.phoebus.olog.entity.Level} is marked as default,
     *     checks are made to make sure no existing {@link org.phoebus.olog.entity.Level}
     *     in the database is marked as default. This is needed to ensure that only one
     *     {@link org.phoebus.olog.entity.Level} is defined to be the default.
     * </p>
     *
     * @param levelName - the name of the {@link org.phoebus.olog.entity.Level} to be created
     * @param level     - the {@link org.phoebus.olog.entity.Level} object, optionally specifying it
     *                  is the default {@link org.phoebus.olog.entity.Level}.
     */
    @SuppressWarnings("unused")
    @PutMapping("/{levelName}")
    public org.phoebus.olog.entity.Level createLevel(@PathVariable String levelName, @RequestBody final org.phoebus.olog.entity.Level level) {

        // Validate request parameters
        validateLevelRequest(level);

        // check if present
        Optional<org.phoebus.olog.entity.Level> existingLevel =
                levelRepository.findById(levelName);
        if (existingLevel.isPresent()) {
            // delete existing level
            levelRepository.deleteById(levelName);
        }

        // create new level
        return levelRepository.save(level);
    }

    /**
     * PUT method for the {@link org.phoebus.olog.entity.Level} resource to support the creation
     * of a list of {@link org.phoebus.olog.entity.Level}s
     *
     * @param levels - the list of {@link org.phoebus.olog.entity.Level}s to be created
     * @return the list of {@link org.phoebus.olog.entity.Level}s created
     */
    @SuppressWarnings("unused")
    @PutMapping
    public Iterable<org.phoebus.olog.entity.Level> createLevels(@RequestBody final List<org.phoebus.olog.entity.Level> levels) {

        // Validate request parameters
        validateLevelsRequest(levels);

        // delete existing levels
        for (org.phoebus.olog.entity.Level level : levels) {
            if (levelRepository.existsById(level.name())) {
                // delete existing tag
                levelRepository.deleteById(level.name());
            }
        }

        // create new levels
        return levelRepository.saveAll(levels);
    }

    @SuppressWarnings("unused")
    @DeleteMapping("/{levelName}")
    public void deleteLevel(@PathVariable String levelName) {

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
