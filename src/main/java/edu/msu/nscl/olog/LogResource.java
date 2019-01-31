package edu.msu.nscl.olog;

import static edu.msu.nscl.olog.OlogResourceDescriptors.LOG_RESOURCE_URI;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.msu.nscl.olog.entity.Attachment;
import edu.msu.nscl.olog.entity.Log;

@RestController
@RequestMapping(LOG_RESOURCE_URI)
public class LogResource
{
    @Autowired
    LogRepository logRepository;
    @Autowired
    AttachmentRepository attachmentRepository;

    /** Creates a new instance of LogResource */
    public LogResource()
    {
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
}
