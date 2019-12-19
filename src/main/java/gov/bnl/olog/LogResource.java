/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.LOG_RESOURCE_URI;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gov.bnl.olog.entity.Attachment;
import gov.bnl.olog.entity.Log;

/**
 * Resource for handling the requests to ../logs
 * @author kunal
 *
 */
@RestController
@RequestMapping(LOG_RESOURCE_URI)
public class LogResource
{
    @Autowired
    LogRepository logRepository;
    @Autowired
    AttachmentRepository attachmentRepository;

    @GetMapping("{logId}")
    public Log getLog(@PathVariable String logId) {
        return logRepository.findById(logId).get();
    }

    @GetMapping("/attachments/{logId}/{attachmentId}")
    public Resource findResources(@PathVariable String logId, @PathVariable String attachmentId)
    {
        Optional<Log> log = logRepository.findById(logId);
        if (log.isPresent())
        {
            Set<Attachment> attachments = log.get().getAttachments().stream().filter(attachment -> {
                return attachment.getId().equals(attachmentId);
            }).collect(Collectors.toSet());
            if (attachments.size() == 1)
            {
                Attachment foundAttachment = attachmentRepository.findById(attachments.iterator().next().getId()).get();
                InputStreamResource resource;
                try
                {
                    resource = new InputStreamResource(foundAttachment.getAttachment().getInputStream());
                    return resource;
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            } else
            {
                // Throw exception, either attachment not found or more than one found
            }
        }
        return null;
    }

    @GetMapping()
    public List<Log> findLogs(@RequestParam MultiValueMap<String, String> allRequestParams) {
        return logRepository.search(allRequestParams);
    }
    
    /**
     * 
     * @param log
     * @return
     */
    @PutMapping()
    public Log createLog(@RequestBody Log log) {
        return logRepository.save(log);
    }
}
