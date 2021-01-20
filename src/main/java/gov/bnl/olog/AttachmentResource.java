/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package gov.bnl.olog;

import gov.bnl.olog.entity.Attachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static gov.bnl.olog.OlogResourceDescriptors.ATTACHMENT_URI;

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

    /**
     *
     * @param attachmentId The unique GridFS id set by client or by GridFS during upload.
     * @return An {@link Attachment} if found, otherwise client will get HTTP 404 response.
     */
    @GetMapping("{attachmentId}")
    public Attachment getLog(@PathVariable String attachmentId) {
        Optional<Attachment> attachment = attachmentRepository.findById(attachmentId);
        if(attachment.isPresent()){
            return attachment.get();
        }
        else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment with id=" + attachmentId + " not found");
        }
    }
}
