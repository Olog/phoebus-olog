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
import java.util.logging.Logger;

import org.phoebus.olog.entity.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Resource for handling the requests to ../properties
 * @author kunal
 */
@RestController
@RequestMapping(PROPERTY_RESOURCE_URI)
public class PropertiesResource {

    @Autowired
    private PropertyRepository propertyRepository;

    private Logger log = Logger.getLogger(PropertiesResource.class.getName());

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
            propertyRepository.findAll(true);
        }
        return propertyRepository.findAll();
    }

    @GetMapping("/{propertyName}")
    public Property findByTitle(@PathVariable String propertyName) {
        return propertyRepository.findById(propertyName).orElseGet(null);
    }

    @PutMapping("/{propertyName}")
    public Property createProperty(@PathVariable String propertyName,
                                   @RequestBody final Property property,
                                   @AuthenticationPrincipal Principal principal) {
        // TODO Check permissions
        // TODO Validate
        // TODO Create a property
        property.setOwner(principal.getName());
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
          // TODO Validate
          // TODO Create a property
          return propertyRepository.saveAll(properties);
      }

    @DeleteMapping("/{propertyName}")
    public void deleteProperty (@PathVariable String propertyName) {
        propertyRepository.deleteById(propertyName);
    }

}
