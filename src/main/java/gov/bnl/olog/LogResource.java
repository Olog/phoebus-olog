/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.LOG_RESOURCE_URI;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/attachments/{logId}/{attachmentName}")
    public ResponseEntity<Resource> findResources(@PathVariable String logId, @PathVariable String attachmentName)
    {
        Optional<Log> log = logRepository.findById(logId);
        if (log.isPresent())
        {
            Set<Attachment> attachments = log.get().getAttachments().stream().filter(attachment -> {
                return attachment.getFilename().equals(attachmentName);
            }).collect(Collectors.toSet());
            if (attachments.size() == 1)
            {
                Attachment foundAttachment = attachmentRepository.findById(attachments.iterator().next().getId()).get();
                InputStreamResource resource;
                try
                {
                    resource = new InputStreamResource(foundAttachment.getAttachment().getInputStream());
                    ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                            .filename(attachmentName)
                            .build();
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.setContentDisposition(contentDisposition);
                    MediaType mediaType = ContentTypeResolver.determineMediaType(attachmentName);
                    if(mediaType != null){
                        httpHeaders.setContentType(mediaType);
                    }
                    return new ResponseEntity<>(resource, httpHeaders, HttpStatus.OK);
                } catch (IOException e)
                {
                    Logger.getLogger(LogResource.class.getName()).log(Level.WARNING,
                            String.format("Unable to retrieve attachment %s for log id %s", attachmentName, logId),
                            e);
                }
            } else
            {
                Logger.getLogger(LogResource.class.getName()).log(Level.WARNING,
                        String.format("Found %d attachments named %s for log id %s", attachments.size(), attachmentName, logId));
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
    public Log createLog(@RequestBody Log log,
                         @AuthenticationPrincipal Principal principal) {
        log.setOwner(principal.getName());
        return logRepository.save(log);
    }

    @PostMapping("/attachments/{logId}")
    public Log createLog(@PathVariable String logId,
                         @RequestPart("file") MultipartFile file,
                         @RequestPart("filename") String filename,
                         @RequestPart("fileMetadataDescription") String fileMetadataDescription) {
        Optional<Log> foundLog = logRepository.findById(logId);
        if (logRepository.findById(logId).isPresent())
        {
            filename = filename == null || filename.isEmpty() ? file.getName() : filename;
            fileMetadataDescription = fileMetadataDescription == null || fileMetadataDescription.isEmpty()
                    ? file.getContentType()
                    : fileMetadataDescription;
            Attachment attachment = new Attachment(file, filename, fileMetadataDescription);
            // Store the attachment
            Attachment createdAttachement = attachmentRepository.save(attachment);
            // Update the log entry with the id of the stored attachment
            Log log = foundLog.get();
            Set<Attachment> existingAttachments = log.getAttachments();
            existingAttachments.add(createdAttachement);
            log.setAttachments(existingAttachments);
            return logRepository.update(log);
        } else
        {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve log with id " + logId);
        }
    }
}
