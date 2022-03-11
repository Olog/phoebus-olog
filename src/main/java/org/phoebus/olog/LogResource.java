/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import org.apache.commons.collections4.CollectionUtils;
import org.phoebus.olog.entity.Attachment;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.LogEntryGroupHelper;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.SearchResult;
import org.phoebus.olog.entity.Tag;
import org.phoebus.olog.entity.preprocess.LogPropertyProvider;
import org.phoebus.olog.entity.preprocess.MarkupCleaner;
import org.phoebus.olog.notification.LogEntryNotifier;
import org.phoebus.util.time.TimeParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.olog.OlogResourceDescriptors.LOG_RESOURCE_URI;
import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;

/**
 * Resource for handling the requests to ../logs
 *
 * @author kunal
 */
@RestController
@RequestMapping(LOG_RESOURCE_URI)
public class LogResource {
    private final Logger logger = Logger.getLogger(LogResource.class.getName());

    @Autowired
    LogRepository logRepository;
    @Autowired
    AttachmentRepository attachmentRepository;
    @SuppressWarnings("unused")
    @Autowired
    private LogbookRepository logbookRepository;
    @SuppressWarnings("unused")
    @Autowired
    private TagRepository tagRepository;
    @SuppressWarnings("unused")
    @Autowired
    private List<MarkupCleaner> markupCleaners;
    @SuppressWarnings("unused")
    @Autowired
    private List<LogEntryNotifier> logEntryNotifiers;
    @SuppressWarnings("unused")
    @Autowired
    private TaskExecutor taskExecutor;
    @SuppressWarnings("unused")
    @Autowired
    private String defaultMarkup;
    @SuppressWarnings("unused")
    @Autowired
    private List<LogPropertyProvider> propertyProviders;
    @SuppressWarnings("unused")
    @Autowired
    private ExecutorService executorService;
    @SuppressWarnings("unused")
    @Autowired
    private Long propertyProvidersTimeout;

    private static final String OLOG_CLIENT_INFO_HEADER = "X-Olog-Client-Info";

    private final Object logGroupSyncObject = new Object();

    @GetMapping("{logId}")
    public Log getLog(@PathVariable String logId) {
        Optional<Log> foundLog = logRepository.findById(logId);
        if (foundLog.isPresent()) {
            return foundLog.get();
        } else {
            logger.log(Level.SEVERE, "Failed to find log: " + logId, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find log: " + logId);
        }
    }

    @GetMapping("/attachments/{logId}/{attachmentName}")
    public ResponseEntity<Resource> findResources(@PathVariable String logId, @PathVariable String attachmentName) {
        Optional<Log> log = logRepository.findById(logId);
        if (log.isPresent()) {
            Set<Attachment> attachments = log.get().getAttachments().stream().filter(attachment -> {
                return attachment.getFilename().equals(attachmentName);
            }).collect(Collectors.toSet());
            if (attachments.size() == 1) {
                Attachment attachment = attachments.iterator().next();
                this.logger.log(Level.INFO, "Requesting attachment " + attachment.getId() + ": " + attachment.getFilename());
                Attachment foundAttachment = attachmentRepository.findById(attachment.getId()).get();
                InputStreamResource resource;
                try {
                    resource = new InputStreamResource(foundAttachment.getAttachment().getInputStream());
                    ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                            .filename(attachmentName)
                            .build();
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.setContentDisposition(contentDisposition);
                    MediaType mediaType = ContentTypeResolver.determineMediaType(attachmentName);
                    if (mediaType != null) {
                        httpHeaders.setContentType(mediaType);
                    }
                    return new ResponseEntity<>(resource, httpHeaders, HttpStatus.OK);
                } catch (IOException e) {
                    Logger.getLogger(LogResource.class.getName())
                            .log(Level.WARNING, String.format("Unable to retrieve attachment %s for log id %s", attachmentName, logId), e);
                }
            } else {
                Logger.getLogger(LogResource.class.getName())
                        .log(Level.WARNING, String.format("Found %d attachments named %s for log id %s", attachments.size(), attachmentName, logId));
            }
        }
        return null;
    }

    /**
     * Finds matching log entries based on the specified search parameters.
     * @param clientInfo A string sent by client identifying it with respect to version and platform.
     * @param allRequestParams A map of search query parameters. Note that this method supports date/time expressions
     *                         like "12 hours" or "2 days" as well as formatted strings like "2021-01-20 12:00:00.123".
     * @return A {@link List} of {@link Log} objects matching the query parameters, or an
     * empty list if no matching logs are found.
     */
    @GetMapping()
    @Deprecated
    public List<Log> findLogs(@RequestHeader(value = OLOG_CLIENT_INFO_HEADER, required = false, defaultValue = "n/a") String clientInfo, @RequestParam MultiValueMap<String, String> allRequestParams) {
        logSearchRequest(clientInfo, allRequestParams);
        for (String key : allRequestParams.keySet()) {
            if ("start".equalsIgnoreCase(key.toLowerCase()) || "end".equalsIgnoreCase(key.toLowerCase())) {
                String value = allRequestParams.get(key).get(0);
                Object time = TimeParser.parseInstantOrTemporalAmount(value);
                if (time instanceof Instant) {
                    allRequestParams.get(key).clear();
                    allRequestParams.get(key).add(MILLI_FORMAT.format((Instant) time));
                } else if (time instanceof TemporalAmount) {
                    allRequestParams.get(key).clear();
                    allRequestParams.get(key).add(MILLI_FORMAT.format(Instant.now().minus((TemporalAmount) time)));
                }
            }
        }
        return logRepository.search(allRequestParams).getLogs();
    }

    @GetMapping("/search")
    public SearchResult search(@RequestHeader(value = OLOG_CLIENT_INFO_HEADER, required = false, defaultValue = "n/a") String clientInfo, @RequestParam MultiValueMap<String, String> allRequestParams) {
        logSearchRequest(clientInfo, allRequestParams);
        for (String key : allRequestParams.keySet()) {
            if ("start".equalsIgnoreCase(key.toLowerCase()) || "end".equalsIgnoreCase(key.toLowerCase())) {
                String value = allRequestParams.get(key).get(0);
                Object time = TimeParser.parseInstantOrTemporalAmount(value);
                if (time instanceof Instant) {
                    allRequestParams.get(key).clear();
                    allRequestParams.get(key).add(MILLI_FORMAT.format((Instant) time));
                } else if (time instanceof TemporalAmount) {
                    allRequestParams.get(key).clear();
                    allRequestParams.get(key).add(MILLI_FORMAT.format(Instant.now().minus((TemporalAmount) time)));
                }
            }
        }
        SearchResult searchResult = logRepository.search(allRequestParams);
        return searchResult;
    }

    /**
     * Creates a new log entry. If the <code>inReplyTo</code> parameters identifies an existing log entry,
     * this method will treat the new log entry as a reply.
     * <p>
     * This may return a HTTP 400 if for instance <code>inReplyTo</code> does not identify an existing log entry,
     * or if the logbooks listed in the {@link Log} object contains invalid (i.e. non-existing) logbooks.
     * @param clientInfo A string sent by client identifying it with respect to version and platform.
     * @param log       A {@link Log} object to be persisted.
     * @param markup    Optional string identifying the wanted markup scheme.
     * @param inReplyTo Optional log entry id specifying to which log entry the new log entry is a response.
     * @param principal The authenticated {@link Principal} of the request.
     * @return The persisted {@link Log} object.
     */
    @PutMapping()
    public Log createLog(@RequestHeader(value = OLOG_CLIENT_INFO_HEADER, required = false, defaultValue = "n/a") String clientInfo,
                         @RequestParam(value = "markup", required = false) String markup,
                         @Valid @RequestBody Log log,
                         @RequestParam(value = "inReplyTo", required = false, defaultValue = "-1") String inReplyTo,
                         @AuthenticationPrincipal Principal principal) {
        if (!inReplyTo.equals("-1")) {
            handleReply(inReplyTo, log);
        }
        log.setOwner(principal.getName());
        log = cleanMarkup(markup, log);
        addPropertiesFromProviders(log);
        Log newLogEntry = logRepository.save(log);
        sendToNotifiers(newLogEntry);

        logger.log(Level.INFO, "Entry id " + newLogEntry.getId() + " created from " + clientInfo);

        return newLogEntry;
    }

    @PostMapping("/attachments/{logId}")
    public Log uploadAttachment(@PathVariable String logId,
                                @RequestPart("file") MultipartFile file,
                                @RequestPart("filename") String filename,
                                @RequestPart(value = "id", required = false) String id,
                                @RequestPart(value = "fileMetadataDescription", required = false) String fileMetadataDescription) {
        Optional<Log> foundLog = logRepository.findById(logId);
        if (logRepository.findById(logId).isPresent()) {
            filename = filename == null || filename.isEmpty() ? file.getName() : filename;
            fileMetadataDescription = fileMetadataDescription == null || fileMetadataDescription.isEmpty()
                    ? file.getContentType()
                    : fileMetadataDescription;
            Attachment attachment = new Attachment(id, file, filename, fileMetadataDescription);
            // Store the attachment
            Attachment createdAttachement = attachmentRepository.save(attachment);
            // Update the log entry with the id of the stored attachment
            Log log = foundLog.get();
            Set<Attachment> existingAttachments = log.getAttachments();
            existingAttachments.add(createdAttachement);
            log.setAttachments(existingAttachments);
            return logRepository.update(log);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve log with id: " + logId);
        }
    }

    /**
     * Updates existing log record. Data sent by client is saved, i.e. if client specifies a shorter list
     * of logbooks or tags, the updated log record will reflect that. However, the following data is NOT updated:
     * <ul>
     *     <li>Attachments</li>
     *     <li>Owner (author)</li>
     *     <li>Created date</li>
     *     <li>Events</li>
     * </ul>
     * Notifiers - if such have been registered - are not called.
     *
     * @param logId  The log id of the entry subject to update. It must exist, i.e. it is not created of not found.
     * @param markup Markup strategy, if any.
     * @param log    The log record data as sent by client.
     * @return The updated log record, or HTTP status 404 if the log record does not exist. If the path
     * variable does not match the id in the log record, HTTP status 400 (bad request) is returned.
     */
    @SuppressWarnings("unused")
    @PostMapping("/{logId}")
    public Log updateLog(@PathVariable String logId,
                         @RequestParam(value = "markup", required = false) String markup,
                         @Valid @RequestBody Log log) {

        Optional<Log> foundLog = logRepository.findById(logId);
        if (foundLog.isPresent()) {
            Log persistedLog = foundLog.get();
            // In case a client sends a log record where the id does not match the path variable, return HTTP 400 (bad request)
            if (!logId.equals(Long.toString(log.getId()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Log entry id does not match path variable");
            }
            persistedLog.setDescription(log.getDescription());
            persistedLog.setLevel(log.getLevel());
            persistedLog.setProperties(log.getProperties());
            persistedLog.setModifyDate(Instant.now());
            persistedLog.setDescription(log.getDescription());
            persistedLog.setTags(log.getTags());
            persistedLog.setLogbooks(log.getLogbooks());
            persistedLog.setTitle(log.getTitle());

            Log newLogEntry = logRepository.update(persistedLog);
            return newLogEntry;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve log with id: " + logId);
        }
    }


    /**
     * Endpoint supporting upload of multiple files, i.e. saving the client from sending one POST request per file.
     * Calls {@link #uploadAttachment(String, MultipartFile, String, String, String)} internally, using the original file's
     * name and content type.
     *
     * @param logId A (numerical) id of a {@link Log}
     * @param files The files subject to upload.
     * @return The persisted {@link Log} object.
     */
    @SuppressWarnings("unused")
    @PostMapping(value = "/attachments-multi/{logId}", consumes = "multipart/form-data")
    public Log uploadMultipleAttachments(@PathVariable String logId,
                                         @RequestPart("file") MultipartFile[] files) {
        if (logRepository.findById(logId).isPresent()) {
            for (MultipartFile file : files) {
                uploadAttachment(logId, file, file.getOriginalFilename(), file.getName(), file.getContentType());
            }
            return logRepository.findById(logId).get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to retrieve log with id: " + logId);
        }
    }

    @SuppressWarnings("unused")
    @PostMapping(value = "/group")
    public void groupLogEntries(@RequestBody List<Long> logEntryIds) {
        logger.log(Level.INFO, "Grouping log entries: " + logEntryIds.stream().map(id -> Long.toString(id)).collect(Collectors.joining(",")));
        Property existingLogEntryGroupProperty = null;
        List<Log> logs = new ArrayList<>();
        // Check prerequisites: if two (or more) log entries are already contained in a group, they must all be contained in
        // the same group. If not, throw exception.
        synchronized (logGroupSyncObject) {
            for (Long id : logEntryIds) {
                Optional<Log> log = null;
                try {
                    log = logRepository.findById(Long.toString(id));
                } catch (ResponseStatusException exception) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Log id " + id + " not found");
                }
                Property logEntryGroupProperty = LogEntryGroupHelper.getLogEntryGroupProperty(log.get());
                if (logEntryGroupProperty != null && existingLogEntryGroupProperty != null &&
                        !logEntryGroupProperty.getAttribute(LogEntryGroupHelper.ATTRIBUTE_ID).equals(existingLogEntryGroupProperty.getAttribute(LogEntryGroupHelper.ATTRIBUTE_ID))) {
                    logger.log(Level.INFO, "Grouping not allowed due to conflicting log entry groups.");
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot group: at least two entries already contained in different groups");
                }
                if(logEntryGroupProperty != null){
                    existingLogEntryGroupProperty = logEntryGroupProperty;
                }
                logs.add(log.get());
            }

            final Property logEntryGroupProperty;
            // If no existing log entry group was found, create a new.
            if (existingLogEntryGroupProperty == null) {
                logEntryGroupProperty = LogEntryGroupHelper.createNewLogEntryProperty();
            } else {
                logEntryGroupProperty = existingLogEntryGroupProperty;
            }

            // Now update the log entries by adding the log group property. Except for those that already have it.
            logs.forEach(log -> {
                if (LogEntryGroupHelper.getLogEntryGroupProperty(log) == null) {
                    log.getProperties().add(logEntryGroupProperty);
                    logRepository.update(log);
                }
            });
        }
    }

    /**
     * {@link LogEntryNotifier} providers are called for the specified log entry. Since a provider
     * implementation may need some time to do it's job, calling them is done asynchronously. Any
     * error handling or logging has to be done in the {@link LogEntryNotifier}, but exceptions are
     * handled here in order to not abort if any of the providers fails.
     *
     * @param log
     */
    private void sendToNotifiers(Log log) {
        if (logEntryNotifiers.isEmpty()) {
            return;
        }
        taskExecutor.execute(() -> logEntryNotifiers.stream().forEach(n -> {
            try {
                n.notify(log);
            } catch (Exception e) {
                Logger.getLogger(LogResource.class.getName())
                        .log(Level.WARNING, "LogEntryNotifier " + n.getClass().getName() + " throws exception", e);
            }
        }));
    }

    private Log cleanMarkup(String markup, Log log) {
        if (markup == null || markup.isEmpty()) {
            markup = defaultMarkup;
        }
        for (MarkupCleaner cleaner : markupCleaners) {
            if (markup.equals(cleaner.getName())) {
                log = cleaner.process(log);
            }
        }
        return log;
    }

    /**
     * This will retrieve {@link Property}s from {@link LogPropertyProvider}s, if any are registered
     * over SPI.
     *
     * @param log The log entry to which the provided {@link Property}s are added. However, it is <i>not</i>
     *            added if a {@link Property} with the same name (case sensitive) is present in the log entry.
     */
    private void addPropertiesFromProviders(Log log) {
        List<String> propertyNames = log.getProperties().stream().map(Property::getName).collect(Collectors.toList());
        List<CompletableFuture<Property>> completableFutures =
                propertyProviders.stream()
                        .map(propertyProvider -> CompletableFuture.supplyAsync(() -> propertyProvider.getProperty(log), executorService))
                        .collect(Collectors.toList());

        CompletableFuture<Void> allFutures =
                CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]));

        try {
            allFutures.get(propertyProvidersTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Logger.getLogger(LogResource.class.getName())
                    .log(Level.SEVERE, "A property provider failed to return in time or threw exception", e);
        }
        List<Property> providedProperties =
                completableFutures.stream()
                        .filter(future -> future.isDone() && !future.isCompletedExceptionally())
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList());

        providedProperties.forEach(property -> {
            if (property != null && !propertyNames.contains(property.getName())) {
                log.getProperties().add(property);
            }
        });
    }

    /**
     * Logs a search request. This may serve the purpose of analysis, i.e. what kind of search queries
     * are actually used (default?, custom?, completely unexpected?).
     *
     * @param clientInfo          String identifying client
     * @param allSearchParameters The list of all search parameters as provided by client.
     */
    private void logSearchRequest(String clientInfo, MultiValueMap<String, String> allSearchParameters) {
        String toLog = allSearchParameters.entrySet().stream()
                .map((e) -> e.getKey().trim() + "=" + e.getValue().stream().collect(Collectors.joining(",")))
                .collect(Collectors.joining("&"));
        logger.log(Level.INFO, "Query " + toLog + " from client " + clientInfo);
    }

    /**
     * Deals with the log entry group property such that if the original log entry (to which user
     * replies) does not already contain the property it is added and the original log entry is updated.
     * Then the reply entry is augmented with the log entry property.
     *
     * @param originalLogEntryId The (Elastic) id of the log entry user wants to reply to.
     * @param log                The contents of the reply entry.
     * @throws {@link ResponseStatusException} if <code>originalLogEntryId</code> does not identify an
     *                existing log entry. This will result in the client receiving a HTTP 400 status.
     */
    private void handleReply(String originalLogEntryId, Log log) {
        try {
            synchronized (logGroupSyncObject) {
                Log originalLogEntry = logRepository.findById(originalLogEntryId).get();
                // Check if the original entry already contains the log entry group property
                Property logEntryGroupProperty = LogEntryGroupHelper.getLogEntryGroupProperty(originalLogEntry);
                if (logEntryGroupProperty == null) {
                    logEntryGroupProperty = LogEntryGroupHelper.createNewLogEntryProperty();
                    originalLogEntry.getProperties().add(logEntryGroupProperty);
                    // Update the original log entry
                    logRepository.update(originalLogEntry);
                }
                // Add the log entry group property to the reply entry (i.e. the new entry)
                log.getProperties().add(logEntryGroupProperty);
            }
        } catch (ResponseStatusException exception) {
            // Log entry not found, return HTTP 400
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reply to log entry " + originalLogEntryId + " as it does not exist");
        }
    }
}
