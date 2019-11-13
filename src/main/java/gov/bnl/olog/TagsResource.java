/*
 * Copyright (c) 2010 Brookhaven National Laboratory
 * Copyright (c) 2010 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * Subject to license terms and conditions.
 */
package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.TAG_RESOURCE_URI;

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

import gov.bnl.olog.entity.Tag;

/**
 * Top level Jersey HTTP methods for the .../tags URL
 *
 * @author Eric Berryman taken from Ralph Lange <Ralph.Lange@bessy.de>
 */
@RestController
@RequestMapping(TAG_RESOURCE_URI)
public class TagsResource {

    @Autowired
    private TagRepository tagRepository;

    private static Logger audit = Logger.getLogger(TagsResource.class.getName() + ".audit");
    static Logger log = Logger.getLogger(TagsResource.class.getName());

    /** Creates a new instance of TagsResource */
    public TagsResource()
    {
    }

    @GetMapping
    public Iterable findAll()
    {
        return tagRepository.findAll();
    }

    @PostMapping
    public Iterable<Tag> updateTag(@RequestBody final List<Tag> tags)
    {
        // TODO Check permissions
        // TODO Validate
        // TODO Create a tag
        return tagRepository.saveAll(tags);
    }

    @GetMapping("/{tagName}")
    public Tag findByTitle(@PathVariable String tagName)
    {
        return tagRepository.findById(tagName).orElseGet(null);
    }

    @PutMapping("/{tagName}")
    public Tag createTag(@PathVariable String tagName, @RequestBody final Tag tag)
    {
        // TODO Check permissions
        // TODO Validate
        // TODO Create a tag
        return tagRepository.index(tag);
    }

    @PostMapping("/{tagName}")
    public Tag updateTag(@PathVariable String tagName, @RequestBody final Tag tag)
    {
        // TODO Check permissions
        // TODO Validate
        // TODO Create a tag
        return tagRepository.save(tag);
    }

    @DeleteMapping("/{tagName}")
    public void deleteTag(@PathVariable String tagName)
    {
        tagRepository.deleteById(tagName);
    }

//    /**
//     * GET method for retrieving the list of tags in the database.
//     *
//     * @return list of logs with their logbooks and tags that match
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
//                    + "|returns " + result.getTags().size() + " tags");
//            return r;
//        } catch (OlogException e) {
//            log.warning(user + "|" + uriInfo.getPath() + "|GET|ERROR|"
//                    + e.getResponseStatusCode() +  "|cause=" + e);
//            return e.toResponse();
//        }
//    }
//
//    /**
//     * POST method for creating multiple tags.
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
//     * GET method for retrieving the tag with the
//     * path parameter <tt>tagName</tt> and its logs.
//     *
//     * @param tag URI path parameter: tag name to search for
//     * @return list of logs with their logbooks and tags that match
//     */
//    @GET
//    @Path("{tagName}")
//    @Produces({"application/xml", "application/json"})
//    public Response read(@PathParam("tagName") String tag) {
//        OlogImpl cm = OlogImpl.getInstance();
//        String user = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "";
//        Tag result = null;
//        try {
//            result = cm.findTagByName(tag);
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
//     * PUT method to create and <b>exclusively</b> update the tag identified by the
//     * path parameter <tt>name</tt> to all logs identified in the payload
//     * structure <tt>data</tt>.
//     * Setting the owner attribute in the XML root element is mandatory.
//     *
//     * @param tag URI path parameter: tag name
//     * @param data Tag structure containing the list of logs to be tagged
//     * @return HTTP Response
//     */
//    @PUT
//    @Path("{tagName}")
//    @Consumes({"application/xml", "application/json"})
//    public Response create(@PathParam("tagName") String tag, Tag data) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        Tag result = null;
//        try {
//            cm.checkValidNameAndOwner(data);
//            cm.checkNameMatchesPayload(tag, data);
//            result = cm.createOrReplaceTag(tag, data);
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
//     * POST method to update the the tag identified by the path parameter <tt>name</tt>,
//     * adding it to all logs identified by the logs inside the payload
//     * structure <tt>data</tt>.
//     * Setting the owner attribute in the XML root element is mandatory.
//     *
//     * @param tag URI path parameter: tag name
//     * @param data list of logs to addSingle the tag <tt>name</tt> to
//     * @return HTTP Response
//     */
//    @POST
//    @Path("{tagName}")
//    @Consumes({"application/xml", "application/json"})
//    public Response update(@PathParam("tagName") String tag, Tag data) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        Tag result = null;
//        try {
//            result = cm.updateTag(tag, data);
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
//     * DELETE method for deleting the tag identified by the path parameter <tt>name</tt>
//     * from all logs.
//     *
//     * @param tag URI path parameter: tag name to remove
//     * @return HTTP Response
//     */
//    @DELETE
//    @Path("{tagName}")
//    public Response remove(@PathParam("tagName") String tag) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        try {
//            cm.removeExistingTag(tag);
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
//     * PUT method for adding the tag identified by <tt>tag</tt> to the single log
//     * <tt>id</tt> (both path parameters).
//     *
//     * @param tag URI path parameter: tag name
//     * @param logId URI path parameter: log to update <tt>tag</tt> to
//     * @param data tag data (ignored)
//     * @return HTTP Response
//     */
//    @PUT
//    @Path("{tagName}/{logId}")
//    @Consumes({"application/xml", "application/json"})
//    public Response addSingle(@PathParam("tagName") String tag, @PathParam("logId")Long logId) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        Tag result = null;
//        try {
//            result = cm.addSingleTag(tag, logId);
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
//     * DELETE method for deleting the tag identified by <tt>tag</tt> from the log
//     * <tt>id</tt> (both path parameters).
//     *
//     * @param tag URI path parameter: tag name to remove
//     * @param logId URI path parameter: log to remove <tt>tag</tt> from
//     * @return HTTP Response
//     */
//    @DELETE
//    @Path("{tagName}/{logId}")
//    public Response removeSingle(@PathParam("tagName") String tag, @PathParam("logId")Long logId) {
//        OlogImpl cm = OlogImpl.getInstance();
//        UserManager um = UserManager.getInstance();
//        um.setUser(securityContext.getUserPrincipal(), securityContext.isUserInRole("Administrator"));
//        try {
//            cm.removeSingleTag(tag, logId);
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
