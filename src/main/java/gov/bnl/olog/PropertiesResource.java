/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */
package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.PROPERTY_RESOURCE_URI;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gov.bnl.olog.entity.Property;

/**
 * Top level Jersey HTTP methods for the .../properties URL
 *
 * @author Eric Berryman taken from Ralph Lange <Ralph.Lange@bessy.de>
 */
@RestController
@RequestMapping(PROPERTY_RESOURCE_URI)
public class PropertiesResource {

    @Autowired
    private PropertyRepository propertyRepository;

    private static Logger audit = Logger.getLogger(PropertiesResource.class.getName() + ".audit");
    static Logger log = Logger.getLogger(PropertiesResource.class.getName());

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

  /**
   * POST method for creating multiple properties.
   *
   * @param properties a list of properties to be created
   * @return The list of successfully created properties
   * @throws IOException when audit or log fail
   */
    @PostMapping
    public Iterable<Property> updateProperty(@RequestBody final List<Property> properties) {
        // TODO Check permissions
        // TODO Validate
        // TODO Create a property
        return propertyRepository.saveAll(properties);
    }

    @GetMapping("/{propertyName}")
    public Property findByTitle(@PathVariable String propertyName) {
        return propertyRepository.findById(propertyName).orElseGet(null);
    }

    @PutMapping("/{propertyName}")
    public Property createProperty(@PathVariable String propertyName, @RequestBody final Property property) {
        // TODO Check permissions
        // TODO Validate
        // TODO Create a property
        return propertyRepository.index(property);
    }

    @PostMapping("/{propertyName}")
    public Property updateProperty(@PathVariable String propertyName, @RequestBody final Property property) {
        // TODO Check permissions
        // TODO Validate
        // TODO Create a property
        return propertyRepository.save(property);
    }

    @DeleteMapping("/{propertyName}")
    public void deleteProperty (@PathVariable String propertyName) {
        propertyRepository.deleteById(propertyName);
    }

//    /**
//     * GET method for retrieving the list of properties in the database.
//     *
//     * @return list of logs with their properties and properties that match
//     */


//    /**
//     * GET method for retrieving the property with the
//     * path parameter <tt>propertyName</tt> and its logs.
//     *
//     * @param property URI path parameter: property name to search for
//     * @return list of logs with their properties and properties that match
//     */

//
//    /**
//     * PUT method to create and <b>exclusively</b> update the property identified by the
//     * path parameter <tt>name</tt> to all logs identified in the payload
//     * structure <tt>data</tt>.
//     * Setting the owner attribute in the XML root element is mandatory.
//     *
//     * @param property URI path parameter: property name
//     * @param data Property structure containing the list of logs to be propertyged
//     * @return HTTP Response
//     */

//
//    /**
//     * POST method to update the the property identified by the path parameter <tt>name</tt>,
//     * adding it to all logs identified by the logs inside the payload
//     * structure <tt>data</tt>.
//     * Setting the owner attribute in the XML root element is mandatory.
//     *
//     * @param property URI path parameter: property name
//     * @param data list of logs to addSingle the property <tt>name</tt> to
//     * @return HTTP Response
//     */

//
//    /**
//     * DELETE method for deleting the property identified by the path parameter <tt>name</tt>
//     * from all logs.
//     *
//     * @param property URI path parameter: property name to remove
//     * @return HTTP Response
//     */

//
//    /**
//     * PUT method for adding the property identified by <tt>property</tt> to the single log
//     * <tt>id</tt> (both path parameters).
//     *
//     * @param property URI path parameter: property name
//     * @param logId URI path parameter: log to update <tt>property</tt> to
//     * @param data property data (ignored)
//     * @return HTTP Response
//     */

//
//    /**
//     * DELETE method for deleting the property identified by <tt>property</tt> from the log
//     * <tt>id</tt> (both path parameters).
//     *
//     * @param property URI path parameter: property name to remove
//     * @param logId URI path parameter: log to remove <tt>property</tt> from
//     * @return HTTP Response
//     */

}
