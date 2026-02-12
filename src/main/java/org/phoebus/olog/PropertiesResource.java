/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import static org.phoebus.olog.OlogResourceDescriptors.PROPERTY_RESOURCE_URI;

import java.security.Principal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Property;
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

    private final Logger log = Logger.getLogger(PropertiesResource.class.getName());

    @Autowired
    private PropertyRepository propertyRepository;

    /** Creates a new instance of LogbooksResource */
    public PropertiesResource() {
    }

    /**
     * GET method to retrieve the list of all active properties. If the inactive flag is set true
     * @param inactive Whether to include inactive {@link Property}s.
     * @return a list of all {@link Property}s
     */
    @GetMapping
    public Iterable<Property> findAll(@RequestParam(required=false, name = "inactive") boolean inactive) {
        if(inactive) {
            return propertyRepository.findAll(true);
        }
        return propertyRepository.findAll();
    }

    @GetMapping("/{propertyName}")
    public Property findByName(@PathVariable(name = "propertyName") String propertyName) {
        Optional<Property> foundProperty = propertyRepository.findById(propertyName);
        if (foundProperty.isPresent()) {
            return foundProperty.get();
        } else {
            String message = MessageFormat.format(TextUtil.PROPERTY_NOT_FOUND, propertyName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    @PutMapping("/{propertyName}")
    public Property createProperty(@PathVariable(name = "propertyName") String propertyName,
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

        // create new property
        return propertyRepository.save(property);
    }

    /**
     * PUT method for creating multiple properties.
     *
     * @param properties a list of properties to be created
     * @return The list of successfully created properties
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
    public void deleteProperty (@PathVariable(name = "propertyName") String propertyName) {
        // TODO Check permissions

        // check if present
        Optional<Property> existingProperty = propertyRepository.findById(propertyName);
        if (existingProperty.isPresent()) {
            // delete existing property
            propertyRepository.deleteById(propertyName);
        } else {
            String message = MessageFormat.format(TextUtil.PROPERTY_NOT_EXISTS, propertyName);
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    /**
     * Checks if all the properties included satisfy the following conditions:
     *
     * <ol>
     * <li> the property names are not null or empty
     * <li> no validation for property owners
     * <li> no validation for property states
     * </ol>
     *
     * @param properties the properties to be validated
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
     * <li> no validation for property owner
     * <li> no validation for property state
     * </ol>
     *
     * @param property the property to be validated
     */
    public void validatePropertyRequest(Property property) {
        if (property.getName() == null || property.getName().isEmpty()) {
            String message = MessageFormat.format(TextUtil.PROPERTY_NAME_CANNOT_BE_NULL_OR_EMPTY, property.toString());
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, null);
        }

        validateAttributeRequest(property.getAttributes());
    }

    /**
     * Checks if all the attributes included satisfy the following conditions:
     *
     * <ol>
     * <li> the attribute names are not null or empty
     * <li> (no checks for attribute values)
     * <li> no validation for attribute states
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
     * <li> no validation for attribute state
     * </ol>
     *
     * @param attribute the attribute to be validated
     */
    public void validateAttributeRequest(Attribute attribute) {
        if (attribute.getName() == null || attribute.getName().isEmpty()) {
            String message = MessageFormat.format(TextUtil.ATTRIBUTE_NAME_CANNOT_BE_NULL_OR_EMPTY, attribute.toString());
            log.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.BAD_REQUEST));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message, null);
        }
    }

}
