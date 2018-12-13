/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */
package edu.msu.nscl.olog;

import static edu.msu.nscl.olog.OlogResourceDescriptors.PROPERTY_RESOURCE_URI;

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
import org.springframework.web.bind.annotation.RestController;

import edu.msu.nscl.olog.entity.Property;

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

    @GetMapping
    public Iterable<Property> findAll() {
        return propertyRepository.findAll();
    }

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
//
//    @GET
//    @Produces({"application/xml", "application/json"})
//    public Response list() {
//        OlogImpl cm = OlogImpl.getInstance();
//        String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "";
//        Propertys result = null;
//        try {
//            result = cm.listPropertys();
//            Response r = Response.ok(result).build();
//            log.fine(user + "|" + uriInfo.getPath() + "|GET|OK|" + r.getStatus()
//                    + "|returns " + result.getPropertys().size() + " properties");
//            return r;
//        } catch (OlogException e) {
//            log.warning(user + "|" + uriInfo.getPath() + "|GET|ERROR|"
//                    + e.getResponseStatusCode() +  "|cause=" + e);
//            return e.toResponse();
//        }
//    }
//
//    /**
//     * POST method for creating multiple properties.
//     *
//     * @param data Propertys data (from payload)
//     * @return HTTP Response
//     * @throws IOException when audit or log fail
//     */
//    @POST
//    @Consumes({"application/xml", "application/json"})
//    public Response add(Propertys data) throws IOException {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        Propertys result = null;
//        try {
//            cm.checkValidNameAndOwner(data);
//            result = cm.createOrReplacePropertys(data);
//            Response r = Response.ok(result).build();
//            audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|POST|OK|" + r.getStatus()
//                    + "|data=" + Propertys.toLogger(data));
//            return r;
//        } catch (OlogException e) {
//            log.warning(um.getUserName() + "|" + uriInfo.getPath() + "|POST|ERROR|" + e.getResponseStatusCode()
//                    + "|data=" + Propertys.toLogger(data) + "|cause=" + e);
//            return e.toResponse();
//        }
//    }
//
//    /**
//     * GET method for retrieving the property with the
//     * path parameter <tt>propertyName</tt> and its logs.
//     *
//     * @param property URI path parameter: property name to search for
//     * @return list of logs with their properties and properties that match
//     */
//    @GET
//    @Path("{propertyName}")
//    @Produces({"application/xml", "application/json"})
//    public Response read(@PathParam("propertyName") String property) {
//        OlogImpl cm = OlogImpl.getInstance();
//        String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "";
//        Property result = null;
//        try {
//            result = cm.findPropertyByName(property);
//            Response r;
//            if (result == null) {
//                r = Response.status(Response.Status.NOT_FOUND).build();
//            } else {
//                r = Response.ok(result).build();
//            }
//            log.fine(user + "|" + uriInfo.getPath() + "|GET|OK|" + r.getStatus());
//            return r;
//        } catch (OlogException e) {
//            log.warning(user + "|" + uriInfo.getPath() + "|GET|ERROR|"
//                    + e.getResponseStatusCode() +  "|cause=" + e);
//            return e.toResponse();
//        }
//    }
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
//    @PUT
//    @Path("{propertyName}")
//    @Consumes({"application/xml", "application/json"})
//    public Response create(@PathParam("propertyName") String property, Property data) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        Property result = null;
//        try {
//            cm.checkValidNameAndOwner(data);
//            cm.checkNameMatchesPayload(property, data);
//            result = cm.createOrReplaceProperty(property, data);
//            Response r = Response.ok(result).build();
//            audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|PUT|OK|" + r.getStatus()
//                    + "|data=" + Property.toLogger(data));
//            return r;
//        } catch (OlogException e) {
//            log.warning(um.getUserName() + "|" + uriInfo.getPath() + "|PUT|ERROR|" + e.getResponseStatusCode()
//                    + "|data=" + Property.toLogger(data) + "|cause=" + e);
//            return e.toResponse();
//        }
//    }
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
//    @POST
//    @Path("{propertyName}")
//    @Consumes({"application/xml", "application/json"})
//    public Response update(@PathParam("propertyName") String property, Property data) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        Property result = null;
//        try {
//            result = cm.updateProperty(property, data);
//            Response r = Response.ok(result).build();
//            audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|POST|OK|" + r.getStatus()
//                    + "|data=" + Property.toLogger(data));
//            return r;
//        } catch (OlogException e) {
//            log.warning(um.getUserName() + "|" + uriInfo.getPath() + "|POST|ERROR|" + e.getResponseStatusCode()
//                    + "|data=" + Property.toLogger(data) + "|cause=" + e);
//            return e.toResponse();
//        }
//    }
//
//    /**
//     * DELETE method for deleting the property identified by the path parameter <tt>name</tt>
//     * from all logs.
//     *
//     * @param property URI path parameter: property name to remove
//     * @return HTTP Response
//     */
//    @DELETE
//    @Path("{propertyName}")
//    public Response remove(@PathParam("propertyName") String property) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        try {
//            cm.removeExistingProperty(property);
//            Response r = Response.ok().build();
//            audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|DELETE|OK|" + r.getStatus());
//            return r;
//        } catch (OlogException e) {
//            log.warning(um.getUserName() + "|" + uriInfo.getPath() + "|DELETE|ERROR|" + e.getResponseStatusCode()
//                    + "|cause=" + e);
//            return e.toResponse();
//        }
//    }
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
//    @PUT
//    @Path("{propertyName}/{logId}")
//    @Consumes({"application/xml", "application/json"})
//    public Response addSingle(@PathParam("propertyName") String property, @PathParam("logId")Long logId) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        Property result = null;
//        try {
//            result = cm.addSingleProperty(property, logId);
//            Response r = Response.ok(result).build();
//            audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|PUT|OK|" + r.getStatus());
//            return r;
//        } catch (OlogException e) {
//            log.warning(um.getUserName() + "|" + uriInfo.getPath() + "|PUT|ERROR|" + e.getResponseStatusCode()
//                    + "|cause=" + e);
//            return e.toResponse();
//        }
//    }
//
//    /**
//     * DELETE method for deleting the property identified by <tt>property</tt> from the log
//     * <tt>id</tt> (both path parameters).
//     *
//     * @param property URI path parameter: property name to remove
//     * @param logId URI path parameter: log to remove <tt>property</tt> from
//     * @return HTTP Response
//     */
//    @DELETE
//    @Path("{propertyName}/{logId}")
//    public Response removeSingle(@PathParam("propertyName") String property, @PathParam("logId")Long logId) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        try {
//            cm.removeSingleProperty(property, logId);
//            Response r = Response.ok().build();
//            audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|DELETE|OK|" + r.getStatus());
//            return r;
//        } catch (OlogException e) {
//            log.warning(um.getUserName() + "|" + uriInfo.getPath() + "|DELETE|ERROR|" + e.getResponseStatusCode()
//                    + "|cause=" + e);
//            return e.toResponse();
//        }
//    }
}
