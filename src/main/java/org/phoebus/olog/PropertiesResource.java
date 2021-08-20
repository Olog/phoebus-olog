/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import static org.phoebus.olog.OlogResourceDescriptors.PROPERTY_RESOURCE_URI;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Property;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resource for handling the requests to ../properties
 * @author kunal
 */
@RestController
@RequestMapping(PROPERTY_RESOURCE_URI)
public class PropertiesResource {

    private Logger log = Logger.getLogger(PropertiesResource.class.getName());

    @Autowired
    private PropertyRepository propertyRepository;

    /** Creates a new instance of LogbooksResource */
    public PropertiesResource() {
    }

    /**
     * GET method to retrieve the list of all active properties. If the inactive flag is set true
     * @return a list of all
     */
    @GetMapping
    public Iterable<Property> findAll(@RequestParam(required=false) boolean inactive) {
        if(inactive) {
            return propertyRepository.findAll(true);
        }
        return propertyRepository.findAll();
    }

    @GetMapping("/{propertyName}")
    public Property findByTitle(@PathVariable String propertyName) {
        Optional<Property> foundProperty = propertyRepository.findById(propertyName);
        if (foundProperty.isPresent()) {
            return foundProperty.get();
        } else {
            log.log(Level.SEVERE, "Failed to find property: " + propertyName, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find property: " + propertyName);
        }
    }

    @PutMapping("/{propertyName}")
    public Property createProperty(@PathVariable String propertyName,
                                   @RequestBody final Property property,
                                   @AuthenticationPrincipal Principal principal) {
        // TODO Check permissions
        // Validate
        // Create a property

        // Validate request parameters
        validatePropertyRequest(property);

        // check if present
        Optional<Property> existingProperty = propertyRepository.findById(propertyName);
        if (existingProperty.isPresent()) {
            // delete existing logbook
            propertyRepository.deleteById(propertyName);
        }

        property.setOwner(principal.getName());

        // create new property
        return propertyRepository.save(property);
    }

    /**
     * PUT method for creating multiple properties.
     *
     * @param properties a list of properties to be created
     * @return The list of successfully created properties
     * @throws IOException when audit or log fail
     */
    @PutMapping
    public Iterable<Property> updateProperty(@RequestBody final List<Property> properties) {
        // TODO Check permissions
        // Validate
        // Create a property

        // Validate request parameters
        validatePropertyRequest(properties);

        // delete existing properties
        for(Property property: properties) {
            if(propertyRepository.existsById(property.getName())) {
                // delete existing tag
                propertyRepository.deleteById(property.getName());
            }
        }

        // create new properties
        return propertyRepository.saveAll(properties);
    }

    @DeleteMapping("/{propertyName}")
    public void deleteProperty (@PathVariable String propertyName) {
        // TODO Check permissions

        // check if present
        Optional<Property> existingProperty = propertyRepository.findById(propertyName);
        if (existingProperty.isPresent()) {
            // delete existing property
            propertyRepository.deleteById(propertyName);
        } else {
            log.log(Level.SEVERE, "The property with the name " + propertyName + " does not exist", new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The property with the name " + propertyName + " does not exist");
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
     * @param logbooks the properties to be validated
     */
    public void validatePropertyRequest(Iterable<Property> properties) {
        for (Property property : properties) {
            validatePropertyRequest(property);
        }
    }

    /**
     * Checks if the property satisfies the following conditions:
     *
     * <ol>
     * <li> the property name is not null or empty
     * <li> the property owner is not null or empty
     * <li> the property state is not null or empty, and is either Active or Inactive
     * </ol>
     *
     * @param property the property to be validated
     */
    public void validatePropertyRequest(Property property) {
        // 1
        if (property.getName() == null || property.getName().isEmpty()) {
            log.log(Level.SEVERE, "The property name cannot be null or empty " + property.toString(), new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The property name cannot be null or empty " + property.toString(), null);
        }
        // 2
        if (property.getOwner() == null || property.getOwner().isEmpty()) {
            log.log(Level.SEVERE, "The property owner cannot be null or empty " + property.toString(), new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The property owner cannot be null or empty " + property.toString(), null);
        }
        // 3
        if (property.getState() == null || !(State.Active.equals(property.getState()) || State.Inactive.equals(property.getState()))) {
            log.log(Level.SEVERE, "The property state cannot be null or empty or not Active/Inactive " + property.toString(), new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The property state cannot be null or empty or not Active/Inactive " + property.toString(), null);
        }

        validateAttributeRequest(property.getAttributes());
    }

    /**
     * Checks if all the attributes included satisfy the following conditions:
     *
     * <ol>
     * <li> the attribute names are not null or empty
     * <li> (no checks for attribute values)
     * <li> the attribute states are not null or empty, and are either Active or Inactive
     * </ol>
     *
     * @param attributes the attributes to be validated
     */
    public void validateAttributeRequest(Iterable<Attribute> attributes) {
        for (Attribute attribute : attributes) {
            validateAttributeRequest(attribute);
        }
    }

    /**
     * Checks if the attribute satisfies the following conditions:
     *
     * <ol>
     * <li> the attribute name is not null or empty
     * <li> (no checks for attribute value)
     * <li> the attribute state is not null or empty, and is either Active or Inactive
     * </ol>
     *
     * @param attribute the attribute to be validated
     */
    public void validateAttributeRequest(Attribute attribute) {
        // 1
        if (attribute.getName() == null || attribute.getName().isEmpty()) {
            log.log(Level.SEVERE, "The attribute name cannot be null or empty " + attribute.toString(), new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The attribute name cannot be null or empty " + attribute.toString(), null);
        }
        // 2
        // 3
        if (attribute.getState() == null || !(State.Active.equals(attribute.getState()) || State.Inactive.equals(attribute.getState()))) {
            log.log(Level.SEVERE, "The attribute state cannot be null or empty or not Active/Inactive " + attribute.toString(), new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The attribute state cannot be null or empty or not Active/Inactive " + attribute.toString(), null);
        }
    }

}
