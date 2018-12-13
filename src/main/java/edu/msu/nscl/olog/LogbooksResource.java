/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */
package edu.msu.nscl.olog;

import static edu.msu.nscl.olog.OlogResourceDescriptors.LOGBOOK_RESOURCE_URI;

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

import edu.msu.nscl.olog.entity.Tag;

/**
 * Top level Jersey HTTP methods for the .../logbooks URL
 *
 * @author Eric Berryman taken from Ralph Lange <Ralph.Lange@bessy.de>
 */
@RestController
@RequestMapping(LOGBOOK_RESOURCE_URI)
public class LogbooksResource {

    @Autowired
    private TagRepository logbookRepository;

    private static Logger audit = Logger.getLogger(LogbooksResource.class.getName() + ".audit");
    static Logger log = Logger.getLogger(LogbooksResource.class.getName());

    /** Creates a new instance of LogbooksResource */
    public LogbooksResource() {
    }

    @GetMapping
    public Iterable<Tag> findAll() {
        return logbookRepository.findAll();
    }

    @PostMapping
    public Iterable<Tag> updateTag(@RequestBody final List<Tag> logbooks) {
        // TODO Check permissions
        // TODO Validate
        // TODO Create a logbook
        return logbookRepository.saveAll(logbooks);
    }

    @GetMapping("/{logbookName}")
    public Tag findByTitle(@PathVariable String logbookName) {
        return logbookRepository.findById(logbookName).orElseGet(null);
    }

    @PutMapping("/{logbookName}")
    public Tag createTag(@PathVariable String logbookName, @RequestBody final Tag logbook) {
        // TODO Check permissions
        // TODO Validate
        // TODO Create a logbook
        return logbookRepository.index(logbook);
    }

    @PostMapping("/{logbookName}")
    public Tag updateTag(@PathVariable String logbookName, @RequestBody final Tag logbook) {
        // TODO Check permissions
        // TODO Validate
        // TODO Create a logbook
        return logbookRepository.save(logbook);
    }

    @DeleteMapping("/{logbookName}")
    public void deleteTag (@PathVariable String logbookName) {
        logbookRepository.deleteById(logbookName);
    }

//    /**
//     * GET method for retrieving the list of logbooks in the database.
//     *
//     * @return list of logs with their logbooks and logbooks that match
//     */
//
//    @GET
//    @Produces({"application/xml", "application/json"})
//    public Response list() {
//        OlogImpl cm = OlogImpl.getInstance();
//        String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "";
//        Tags result = null;
//        try {
//            result = cm.listTags();
//            Response r = Response.ok(result).build();
//            log.fine(user + "|" + uriInfo.getPath() + "|GET|OK|" + r.getStatus()
//                    + "|returns " + result.getTags().size() + " logbooks");
//            return r;
//        } catch (OlogException e) {
//            log.warning(user + "|" + uriInfo.getPath() + "|GET|ERROR|"
//                    + e.getResponseStatusCode() +  "|cause=" + e);
//            return e.toResponse();
//        }
//    }
//
//    /**
//     * POST method for creating multiple logbooks.
//     *
//     * @param data Tags data (from payload)
//     * @return HTTP Response
//     * @throws IOException when audit or log fail
//     */
//    @POST
//    @Consumes({"application/xml", "application/json"})
//    public Response add(Tags data) throws IOException {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        Tags result = null;
//        try {
//            cm.checkValidNameAndOwner(data);
//            result = cm.createOrReplaceTags(data);
//            Response r = Response.ok(result).build();
//            audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|POST|OK|" + r.getStatus()
//                    + "|data=" + Tags.toLogger(data));
//            return r;
//        } catch (OlogException e) {
//            log.warning(um.getUserName() + "|" + uriInfo.getPath() + "|POST|ERROR|" + e.getResponseStatusCode()
//                    + "|data=" + Tags.toLogger(data) + "|cause=" + e);
//            return e.toResponse();
//        }
//    }
//
//    /**
//     * GET method for retrieving the logbook with the
//     * path parameter <tt>logbookName</tt> and its logs.
//     *
//     * @param logbook URI path parameter: logbook name to search for
//     * @return list of logs with their logbooks and logbooks that match
//     */
//    @GET
//    @Path("{logbookName}")
//    @Produces({"application/xml", "application/json"})
//    public Response read(@PathParam("logbookName") String logbook) {
//        OlogImpl cm = OlogImpl.getInstance();
//        String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "";
//        Tag result = null;
//        try {
//            result = cm.findTagByName(logbook);
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
//     * PUT method to create and <b>exclusively</b> update the logbook identified by the
//     * path parameter <tt>name</tt> to all logs identified in the payload
//     * structure <tt>data</tt>.
//     * Setting the owner attribute in the XML root element is mandatory.
//     *
//     * @param logbook URI path parameter: logbook name
//     * @param data Tag structure containing the list of logs to be logbookged
//     * @return HTTP Response
//     */
//    @PUT
//    @Path("{logbookName}")
//    @Consumes({"application/xml", "application/json"})
//    public Response create(@PathParam("logbookName") String logbook, Tag data) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        Tag result = null;
//        try {
//            cm.checkValidNameAndOwner(data);
//            cm.checkNameMatchesPayload(logbook, data);
//            result = cm.createOrReplaceTag(logbook, data);
//            Response r = Response.ok(result).build();
//            audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|PUT|OK|" + r.getStatus()
//                    + "|data=" + Tag.toLogger(data));
//            return r;
//        } catch (OlogException e) {
//            log.warning(um.getUserName() + "|" + uriInfo.getPath() + "|PUT|ERROR|" + e.getResponseStatusCode()
//                    + "|data=" + Tag.toLogger(data) + "|cause=" + e);
//            return e.toResponse();
//        }
//    }
//
//    /**
//     * POST method to update the the logbook identified by the path parameter <tt>name</tt>,
//     * adding it to all logs identified by the logs inside the payload
//     * structure <tt>data</tt>.
//     * Setting the owner attribute in the XML root element is mandatory.
//     *
//     * @param logbook URI path parameter: logbook name
//     * @param data list of logs to addSingle the logbook <tt>name</tt> to
//     * @return HTTP Response
//     */
//    @POST
//    @Path("{logbookName}")
//    @Consumes({"application/xml", "application/json"})
//    public Response update(@PathParam("logbookName") String logbook, Tag data) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        Tag result = null;
//        try {
//            result = cm.updateTag(logbook, data);
//            Response r = Response.ok(result).build();
//            audit.info(um.getUserName() + "|" + uriInfo.getPath() + "|POST|OK|" + r.getStatus()
//                    + "|data=" + Tag.toLogger(data));
//            return r;
//        } catch (OlogException e) {
//            log.warning(um.getUserName() + "|" + uriInfo.getPath() + "|POST|ERROR|" + e.getResponseStatusCode()
//                    + "|data=" + Tag.toLogger(data) + "|cause=" + e);
//            return e.toResponse();
//        }
//    }
//
//    /**
//     * DELETE method for deleting the logbook identified by the path parameter <tt>name</tt>
//     * from all logs.
//     *
//     * @param logbook URI path parameter: logbook name to remove
//     * @return HTTP Response
//     */
//    @DELETE
//    @Path("{logbookName}")
//    public Response remove(@PathParam("logbookName") String logbook) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        try {
//            cm.removeExistingTag(logbook);
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
//     * PUT method for adding the logbook identified by <tt>logbook</tt> to the single log
//     * <tt>id</tt> (both path parameters).
//     *
//     * @param logbook URI path parameter: logbook name
//     * @param logId URI path parameter: log to update <tt>logbook</tt> to
//     * @param data logbook data (ignored)
//     * @return HTTP Response
//     */
//    @PUT
//    @Path("{logbookName}/{logId}")
//    @Consumes({"application/xml", "application/json"})
//    public Response addSingle(@PathParam("logbookName") String logbook, @PathParam("logId")Long logId) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        Tag result = null;
//        try {
//            result = cm.addSingleTag(logbook, logId);
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
//     * DELETE method for deleting the logbook identified by <tt>logbook</tt> from the log
//     * <tt>id</tt> (both path parameters).
//     *
//     * @param logbook URI path parameter: logbook name to remove
//     * @param logId URI path parameter: log to remove <tt>logbook</tt> from
//     * @return HTTP Response
//     */
//    @DELETE
//    @Path("{logbookName}/{logId}")
//    public Response removeSingle(@PathParam("logbookName") String logbook, @PathParam("logId")Long logId) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        try {
//            cm.removeSingleTag(logbook, logId);
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
