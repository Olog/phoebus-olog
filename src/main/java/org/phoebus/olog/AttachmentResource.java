/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import org.phoebus.olog.entity.Attachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.phoebus.olog.OlogResourceDescriptors.ATTACHMENT_URI;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Resource for handling the requests to ../attachment
 * @author Georg Weiss
 *
 */
@RestController
@RequestMapping(ATTACHMENT_URI)
public class AttachmentResource
{
    @Autowired
    AttachmentRepository attachmentRepository;

    private Logger log = Logger.getLogger(AttachmentResource.class.getName());

    /**
     *
     * @param attachmentId The unique GridFS id set by client or by GridFS during upload.
     * @return A {@link ResponseEntity} if found, otherwise client will get HTTP 404 response. If
     * an {@link IOException} is thrown when the input stream of the GridFS resource is requested,
     * a HTTP 500 response is returned.
     */
    @GetMapping("{attachmentId}")
    public ResponseEntity<Resource> getAttachment(@PathVariable String attachmentId) {
        log.log(Level.INFO, "Requesting attachment " + attachmentId);
        Optional<Attachment> attachment = attachmentRepository.findById(attachmentId);
        if(attachment.isPresent()){
            InputStreamResource resource;
            try
            {
                resource = new InputStreamResource(attachment.get().getAttachment().getInputStream());
                ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                        .filename(attachment.get().getFilename())
                        .build();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentDisposition(contentDisposition);
                MediaType mediaType = ContentTypeResolver.determineMediaType(attachment.get().getFilename());
                if(mediaType != null){
                    httpHeaders.setContentType(mediaType);
                }
                return new ResponseEntity<>(resource, httpHeaders, HttpStatus.OK);
            }
            catch (IOException e) {
                Logger.getLogger(LogResource.class.getName())
                    .log(Level.SEVERE, "Unable to retrieve attachment with id: " + attachmentId, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        else{
            Logger.getLogger(LogResource.class.getName())
                .log(Level.WARNING, "Attachment with id " + attachmentId + " not found");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
