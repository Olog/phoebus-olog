/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.phoebus.olog.entity.Attachment;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.LogEntryGroupHelper;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.SearchResult;
import org.phoebus.olog.entity.Tag;
import org.phoebus.olog.entity.preprocess.LogPropertyProvider;
import org.phoebus.olog.entity.preprocess.MarkupCleaner;
import org.phoebus.olog.entity.websocket.MessageType;
import org.phoebus.olog.entity.websocket.WebSocketMessage;
import org.phoebus.olog.notification.LogEntryNotifier;
import org.phoebus.olog.websocket.WebSocketService;
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
import org.springframework.util.LinkedMultiValueMap;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.olog.OlogResourceDescriptors.LOG_RESOURCE_URI;
import static org.phoebus.util.time.TimestampFormats.MILLI_PATTERN;

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

    @SuppressWarnings("unused")
    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private Detector detector;

    /**
     * Custom HTTP header that client may send in order to identify itself. This is logged for some of the
     * endpoints in this controller.
     */
    private static final String OLOG_CLIENT_INFO_HEADER = "X-Olog-Client-Info";

    private final Object logGroupSyncObject = new Object();

    @GetMapping("{logId}")
    @SuppressWarnings("unused")
    public Log getLogById(@PathVariable(name = "logId") String logId) {
        Optional<Log> foundLog = logRepository.findById(logId);
        if (foundLog.isPresent()) {
            return foundLog.get();
        } else {
            String message = MessageFormat.format(TextUtil.LOG_NOT_FOUND, logId);
            logger.log(Level.SEVERE, message, new ResponseStatusException(HttpStatus.NOT_FOUND));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    @GetMapping("archived/{logId}")
    @SuppressWarnings("unused")
    public SearchResult getArchivedLog(@PathVariable(name = "logId") String logId) {
        return logRepository.findArchivedById(logId);
    }

    @GetMapping("/attachments/{logId}/{attachmentName}")
    public ResponseEntity<Resource> getAttachment(@PathVariable(name = "logId") String logId, @PathVariable(name = "attachmentName") String attachmentName) {
        Optional<Log> log = logRepository.findById(logId);
        if (log.isPresent()) {
            Set<Attachment> attachments = log.get().getAttachments().stream().filter(attachment -> attachment.getFilename().equals(attachmentName)).collect(Collectors.toSet());
            if (attachments.size() == 1) {
                Attachment attachment = attachments.iterator().next();
                this.logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.ATTACHMENT_REQUEST_DETAILS, attachment.getId(), attachment.getFilename()));
                Optional<Attachment> attachmentOptional = attachmentRepository.findById(attachment.getId());
                if (attachmentOptional.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, MessageFormat.format(TextUtil.ATTACHMENT_UNABLE_TO_RETRIEVE_FOR_ID, attachmentName, logId));
                }
                InputStreamResource resource;
                try {
                    resource = new InputStreamResource(attachmentOptional.get().getAttachment().getInputStream());
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
                            .log(Level.WARNING, MessageFormat.format(TextUtil.ATTACHMENT_UNABLE_TO_RETRIEVE_FOR_ID, attachmentName, logId), e);
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, MessageFormat.format(TextUtil.ATTACHMENT_UNABLE_TO_RETRIEVE_FOR_ID, attachmentName, logId));

                }
            } else {
                Logger.getLogger(LogResource.class.getName())
                        .log(Level.WARNING, () -> MessageFormat.format(TextUtil.ATTACHMENTS_NAMED_FOUND_FOR_ID, attachments.size(), attachmentName, logId));
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, MessageFormat.format(TextUtil.ATTACHMENT_UNABLE_TO_RETRIEVE_FOR_ID, attachmentName, logId));
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, MessageFormat.format(TextUtil.ATTACHMENT_UNABLE_TO_RETRIEVE_FOR_ID, attachmentName, logId));
    }

    /**
     * Finds matching log entries based on the specified search parameters.
     *
     * @param clientInfo       A string sent by client identifying it with respect to version and platform.
     * @param allRequestParams A map of search query parameters. Note that this method supports date/time expressions
     *                         like "12 hours" or "2 days" as well as formatted strings like "2021-01-20 12:00:00.123".
     * @return A {@link List} of {@link Log} objects matching the query parameters, or an
     * empty list if no matching logs are found.
     */
    @GetMapping()
    public ResponseEntity<?> findLogs(@RequestHeader(value = OLOG_CLIENT_INFO_HEADER, required = false, defaultValue = "n/a") String clientInfo, @RequestParam MultiValueMap<String, String> allRequestParams) {
        ResponseEntity responseEntity = search(clientInfo, allRequestParams);
        if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
            return new ResponseEntity<>(((SearchResult) responseEntity.getBody()).getLogs(), HttpStatus.OK);
        }
        return responseEntity;
    }

    /**
     * Finds matching log entries based on the specified search parameters.
     *
     * @param clientInfo       A string sent by client identifying it with respect to version and platform.
     * @param allRequestParams A map of search query parameters. Note that this method supports date/time expressions
     *                         like "12 hours" or "2 days" as well as formatted strings like "2021-01-20 12:00:00.123".
     *                         Search parameters considered invalid may result in an HTTP 400 (bad request) response.
     * @return A {@link SearchResult} holding matching objects, if any.
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestHeader(value = OLOG_CLIENT_INFO_HEADER, required = false, defaultValue = "n/a") String clientInfo, @RequestParam MultiValueMap<String, String> allRequestParams) {
        logSearchRequest(clientInfo, allRequestParams);
        try {
            return new ResponseEntity<>(logRepository.search(allRequestParams), HttpStatus.OK);
        } catch (IllegalArgumentException exception) {
            return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Creates a new log entry. If the <code>inReplyTo</code> parameters identifies an existing log entry,
     * this method will treat the new log entry as a reply.
     * <p>
     * This may return a HTTP 400 if for instance <code>inReplyTo</code> does not identify an existing log entry,
     * or if the logbooks listed in the {@link Log} object contains invalid (i.e. non-existing) logbooks.
     * </p>
     * <p>
     * Primary use case is upload of log entry without attachments as this type of request is easier to
     * construct, i.e. client need not create a request with multipart items.
     * </p>
     *
     * @param clientInfo A string sent by client identifying it with respect to version and platform.
     * @param log        A {@link Log} object to be persisted.
     * @param markup     Optional string identifying the wanted markup scheme.
     * @param inReplyTo  Optional log entry id specifying to which log entry the new log entry is a reply.
     * @param principal  The authenticated {@link Principal} of the request.
     * @return The persisted {@link Log} object.
     */
    @PutMapping()
    public Log createLog(@RequestHeader(value = OLOG_CLIENT_INFO_HEADER, required = false, defaultValue = "n/a") String clientInfo,
                         @RequestParam(name = "markup", required = false) String markup,
                         @RequestParam(name = "inReplyTo", required = false, defaultValue = "-1") String inReplyTo,
                         @RequestBody Log log,
                         @AuthenticationPrincipal Principal principal) {
        if (log.getLogbooks().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.LOG_MUST_HAVE_LOGBOOK);
        }
        if (log.getTitle().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.LOG_MUST_HAVE_TITLE);
        }
        if (!inReplyTo.equals("-1")) {
            handleReply(inReplyTo, log);
        }
        log.setOwner(principal.getName());
        Set<String> logbookNames = log.getLogbooks().stream().map(Logbook::getName).collect(Collectors.toSet());
        Set<String> persistedLogbookNames = new HashSet<>();
        logbookRepository.findAll().forEach(l -> persistedLogbookNames.add(l.getName()));
        if (!CollectionUtils.containsAll(persistedLogbookNames, logbookNames)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.LOG_INVALID_LOGBOOKS);
        }
        Set<Tag> tags = log.getTags();
        if (tags != null && !tags.isEmpty()) {
            Set<String> tagNames = tags.stream().map(Tag::getName).collect(Collectors.toSet());
            Set<String> persistedTags = new HashSet<>();
            tagRepository.findAll().forEach(t -> persistedTags.add(t.getName()));
            if (!CollectionUtils.containsAll(persistedTags, tagNames)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.LOG_INVALID_TAGS);
            }
        }
        log = cleanMarkup(markup, log);
        addPropertiesFromProviders(log);
        Log newLogEntry = logRepository.save(log);
        sendToNotifiers(newLogEntry);

        webSocketService.sendMessageToClients(new WebSocketMessage(MessageType.NEW_LOG_ENTRY, null));

        logger.log(Level.INFO, () -> "Entry id " + newLogEntry.getId() + " created from " + clientInfo);

        return newLogEntry;
    }

    /**
     * Creates a new log entry. If the <code>inReplyTo</code> parameters identifies an existing log entry,
     * this method will treat the new log entry as a reply.
     * <p>
     * This may return a HTTP 400 if for instance <code>inReplyTo</code> does not identify an existing log entry,
     * or if the logbooks listed in the {@link Log} object contains invalid (i.e. non-existing) logbooks.
     * </p>
     * <p>Client calling this endpoint <b>must</b> set Content-Type=multipart/form-data.</p>
     *
     * @param clientInfo A string sent by client identifying it with respect to version and platform.
     * @param logEntry   A {@link Log} object to be persisted.
     * @param markup     Optional string identifying the wanted markup scheme.
     * @param inReplyTo  Optional log entry id specifying to which log entry the new log entry is a reply.
     * @param files      Optional array of {@link MultipartFile}s representing attachments. These <b>must</b> appear in the same
     *                   order as the {@link Attachment} items in the list of {@link Attachment}s of the log entry.
     * @param principal  The authenticated {@link Principal} of the request.
     * @return The persisted {@link Log} object.
     */
    @PutMapping("/multipart")
    public Log createLog(@RequestHeader(value = OLOG_CLIENT_INFO_HEADER, required = false, defaultValue = "n/a") String clientInfo,
                         @RequestParam(name = "markup", required = false) String markup,
                         @RequestParam(name = "inReplyTo", required = false, defaultValue = "-1") String inReplyTo,
                         @RequestPart("logEntry") Log logEntry,
                         @RequestPart(value = "files", required = false) MultipartFile[] files,
                         @AuthenticationPrincipal Principal principal) {

        if (files != null && logEntry.getAttachments() != null && files.length != logEntry.getAttachments().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.ATTACHMENT_DATA_INVALID);
        }

        List<MultipartFile> multipartFiles;

        try {
            multipartFiles = checkSupportedAttachmentTypes(files);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.ATTACHMENT_HEIC_NOT_SUPPORTED);
        }

        Log newLogEntry = createLog(clientInfo, markup, inReplyTo, logEntry, principal);

        if (files != null) {
            for (MultipartFile multipartFile : multipartFiles) {
                String originalFileName = multipartFile.getOriginalFilename();
                Optional<Attachment> attachment =
                        logEntry.getAttachments().stream()
                                .filter(a -> a.getFilename() != null && a.getFilename().equals(originalFileName)).findFirst();
                if (attachment.isEmpty()) { // Should not happen if client behaves correctly
                    logger.log(Level.WARNING, () -> MessageFormat.format(TextUtil.ATTACHMENT_FILE_NOT_MATCHED_META_DATA, originalFileName));
                    continue;
                }

                saveAttachment(Long.toString(newLogEntry.getId()),
                        multipartFile,
                        originalFileName,
                        attachment.get().getId(),
                        attachment.get().getFileMetadataDescription());
            }
        }

        logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.LOG_ENTRY_ID_CREATED_FROM, newLogEntry.getId(), clientInfo));

        return newLogEntry;
    }


    /**
     * Add an attachment to log entry identified by logId
     *
     * @param logId                   log entry ID
     * @param file                    the file to be attached
     * @param filename                name of file
     * @param id                      UUID for file in mongo
     * @param fileMetadataDescription file metadata
     * @return The updated {@link Log}.
     */
    @PostMapping("/attachments/{logId}")
    public Log uploadAttachment(@PathVariable(name = "logId") String logId,
                                @RequestPart("file") MultipartFile file,
                                @RequestPart("filename") String filename,
                                @RequestPart(name = "id", required = false) String id,
                                @RequestPart(name = "fileMetadataDescription", required = false) String fileMetadataDescription) {

        List<MultipartFile> multipartFiles;

        try {
            multipartFiles = checkSupportedAttachmentTypes(new MultipartFile[]{file});
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.ATTACHMENT_HEIC_NOT_SUPPORTED);
        }
        return saveAttachment(logId,
                multipartFiles.getFirst(),
                filename,
                id,
                fileMetadataDescription);
    }

    /**
     * Saves the content of the {@link MultipartFile} to the database.
     *
     * @param logId                   The log id associated with the attachment
     * @param file                    A {@link MultipartFile} representing the attachment contents
     * @param filename                The file name as defined by the client, e.g. file name on disk.
     * @param id                      A unique id for the attachment.
     * @param fileMetadataDescription Description of the content
     * @return A {@link Log} where the attachment field has been updated with the saved attachment.
     */
    private Log saveAttachment(String logId,
                               MultipartFile file,
                               String filename,
                               String id,
                               String fileMetadataDescription) {
        Optional<Log> foundLog = logRepository.findById(logId);
        if (logRepository.findById(logId).isPresent()) {
            filename = filename == null || filename.isEmpty() ? file.getName() : filename;
            fileMetadataDescription = fileMetadataDescription == null || fileMetadataDescription.isEmpty()
                    ? file.getContentType()
                    : fileMetadataDescription;
            Attachment attachment = new Attachment(id, file, filename, fileMetadataDescription);
            // Store the attachment
            Attachment createdAttachment = attachmentRepository.save(attachment);
            SortedSet<Attachment> existingAttachments = foundLog.get().getAttachments();
            existingAttachments.add(createdAttachment);
            foundLog.get().setAttachments(existingAttachments);
            return logRepository.update(foundLog.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MessageFormat.format(TextUtil.LOG_NOT_RETRIEVED, logId));
        }
    }

    /**
     * Updates existing log record. Data sent by client is saved, i.e. if client specifies a shorter list
     * of logbooks or tags, the updated log record will reflect that. However, the following data is NOT updated:
     * <ul>
     *     <li>Attachments</li>
     *     <li>Created date</li>
     *     <li>Events</li>
     * </ul>
     * Notifiers - if such have been registered - are not called.
     *
     * @param logId     The log id of the entry subject to update. It must exist, i.e. it is not created of not found.
     * @param markup    Markup strategy, if any.
     * @param log       The log record data as sent by client.
     * @param principal The authenticated {@link Principal} of the request.
     * @return The updated log record, or HTTP status 404 if the log record does not exist. If the path
     * variable does not match the id in the log record, HTTP status 400 (bad request) is returned.
     */
    @SuppressWarnings("unused")
    @PostMapping("/{logId}")
    public Log updateLog(@PathVariable(name = "logId") String logId,
                         @RequestParam(name = "markup", required = false) String markup,
                         @RequestBody Log log,
                         @AuthenticationPrincipal Principal principal) {

        // In case a client sends a log record where the id does not match the path variable, return HTTP 400 (bad request)
        if (!logId.equals(Long.toString(log.getId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.LOG_ENTRY_NOT_MATCH_PATH);
        }

        Optional<Log> foundLog = logRepository.findById(logId);
        if (foundLog.isPresent()) {
            Log persistedLog = foundLog.get();
            logRepository.archive(persistedLog);

            // log entry group property should not be editable but remain if it exists
            Property logEntryGroupProperty = LogEntryGroupHelper.getLogEntryGroupProperty(log);
            if (logEntryGroupProperty != null) {
                log.getProperties().remove(logEntryGroupProperty);
            }
            logEntryGroupProperty = LogEntryGroupHelper.getLogEntryGroupProperty(persistedLog);
            if (logEntryGroupProperty != null) {
                log.getProperties().add(logEntryGroupProperty);
            }

            persistedLog.setOwner(principal.getName());
            persistedLog.setLevel(log.getLevel());
            persistedLog.setProperties(log.getProperties());
            persistedLog.setModifyDate(Instant.now());
            persistedLog.setDescription(log.getDescription());   // to make it work with old clients where description field is sent instead of source
            persistedLog.setSource(log.getSource());
            persistedLog.setTags(log.getTags());
            persistedLog.setLogbooks(log.getLogbooks());
            persistedLog.setTitle(log.getTitle());
            persistedLog = cleanMarkup(markup, persistedLog);

            webSocketService.sendMessageToClients(new WebSocketMessage(MessageType.LOG_ENTRY_UPDATED, persistedLog.getId().toString()));

            return logRepository.update(persistedLog);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MessageFormat.format(TextUtil.LOG_NOT_RETRIEVED, logId));
        }
    }

    @SuppressWarnings("unused")
    @PostMapping(value = "/group")
    public void groupLogEntries(@RequestBody List<Long> logEntryIds) {
        logger.log(Level.INFO, () -> "Grouping log entries: " + logEntryIds.stream().map(id -> Long.toString(id)).collect(Collectors.joining(",")));
        Property existingLogEntryGroupProperty = null;
        List<Log> logs = new ArrayList<>();
        // Check prerequisites: if two (or more) log entries are already contained in a group, they must all be contained in
        // the same group. If not, throw exception.
        synchronized (logGroupSyncObject) {
            for (Long id : logEntryIds) {
                Optional<Log> log;
                try {
                    log = logRepository.findById(Long.toString(id));
                } catch (ResponseStatusException exception) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageFormat.format(TextUtil.LOG_ID_NOT_FOUND, id));
                }
                Property logEntryGroupProperty = LogEntryGroupHelper.getLogEntryGroupProperty(log.get());
                if (logEntryGroupProperty != null && existingLogEntryGroupProperty != null &&
                        !logEntryGroupProperty.getAttribute(LogEntryGroupHelper.ATTRIBUTE_ID).equals(existingLogEntryGroupProperty.getAttribute(LogEntryGroupHelper.ATTRIBUTE_ID))) {
                    logger.log(Level.INFO, TextUtil.GROUPING_NOT_ALLOWED);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.GROUPING_ENTRIES_IN_DIFFERENT_GROUPS);
                }
                if (logEntryGroupProperty != null) {
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
     * implementation may need some time to do its job, calling them is done asynchronously. Any
     * error handling or logging has to be done in the {@link LogEntryNotifier}, but exceptions are
     * handled here in order to not abort if any of the providers fails.
     *
     * @param log The log entry
     */
    private void sendToNotifiers(Log log) {

        if (logEntryNotifiers.isEmpty()) {
            return;
        }
        taskExecutor.execute(() -> logEntryNotifiers.forEach(n -> {
            try {
                n.notify(log);
            } catch (Exception e) {
                Logger.getLogger(LogResource.class.getName())
                        .log(Level.WARNING, MessageFormat.format(TextUtil.LOG_ENTRY_NOTIFIER, n.getClass().getName()), e);
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
                    .log(Level.SEVERE, TextUtil.PROPERTY_PROVIDER_FAILED_TO_RETURN, e);
        }
        List<Property> providedProperties =
                completableFutures.stream()
                        .filter(future -> future.isDone() && !future.isCompletedExceptionally())
                        .map(CompletableFuture::join)
                        .toList();

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
        logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.QUERY_FROM_CLIENT, toLog, clientInfo));
    }

    /**
     * Deals with the log entry group property such that if the original log entry (to which user
     * replies) does not already contain the property it is added and the original log entry is updated.
     * Then the reply entry is augmented with the log entry property.
     *
     * @param originalLogEntryId The (Elastic) id of the log entry user wants to reply to.
     * @param log                The contents of the reply entry.
     * @throws ResponseStatusException if <code>originalLogEntryId</code> does not identify an
     *                                 existing log entry. This will result in the client receiving a HTTP 400 status.
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageFormat.format(TextUtil.LOG_ENTRY_CANNOT_REPLY_NOT_EXISTS, originalLogEntryId));
        }
    }

    /**
     * GET method for retrieving an RSS feed of channels.
     *
     * @param allRequestParams Client's request parameters, may be <code>null</code>
     * @param request          {@link HttpServletRequest} from which to construct base URL.
     * @return the name of the RSS feed view, which will be resolved to render the feed
     */
    @GetMapping(path = "/rss", produces = "application/rss+xml")
    public com.rometools.rome.feed.rss.Channel getRssFeed(@RequestParam MultiValueMap<String, String> allRequestParams, HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/" + request.getContextPath();
        if (allRequestParams == null) {
            allRequestParams = new LinkedMultiValueMap<>();
        }

        Instant now = Instant.now();
        if (allRequestParams.get("start") == null || allRequestParams.get("start").isEmpty()) {
            allRequestParams.put("start", List.of(DateTimeFormatter.ofPattern(MILLI_PATTERN).withZone(ZoneId.systemDefault()).format(now.minus(Duration.ofDays(7)))));
        }
        if (allRequestParams.get("end") == null || allRequestParams.get("end").isEmpty()) {
            allRequestParams.put("end", List.of(DateTimeFormatter.ofPattern(MILLI_PATTERN).withZone(ZoneId.systemDefault()).format(now)));
        }
        if (allRequestParams.get("from") == null || allRequestParams.get("from").isEmpty()) {
            allRequestParams.put("from", List.of("0"));
        }
        if (allRequestParams.get("size") == null || allRequestParams.get("size").isEmpty()) {
            allRequestParams.put("size", List.of("100"));
        }

        Object result = search(request.getHeader("User-Agent"), allRequestParams).getBody();

        if (result instanceof SearchResult searchResult) {
            return RssFeedUtil.fromLogEntries(searchResult.getLogs(), baseUrl);
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find entries");
        }
    }

    /**
     * For each {@link MultipartFile} in the provided list, this method will check the type of attachment in order
     * to be able to reject unsupported types (e.g. HEIC image files). As it operates on the {@link InputStream} provided
     * through a {@link MultipartFile}, and in order to be able to consume that stream again when saving to
     * the attachments database, the original {@link MultipartFile} is cloned to a new one where the {@link InputStream}
     * is wrapped in a {@link BufferedInputStream}.
     * <p>
     * The analysis of content type is delegated to Apache Tika. If an unsupported content type is encountered,
     * this methid throws an {@link IllegalArgumentException}.
     * </p>
     *
     * @param multipartFiles Array of {@link MultipartFile}s from client as intercepted by the endpoint.
     * @return A {@link List} of {@link MultipartFile}s where the {@link InputStream} can be consumed again.
     * @throws IllegalArgumentException if an unsupported content type is encontered.
     *
     */
    protected List<MultipartFile> checkSupportedAttachmentTypes(MultipartFile[] multipartFiles) {
        if (multipartFiles == null || multipartFiles.length == 0) {
            return null;
        }

        List<MultipartFile> attachmentFiles = new ArrayList<>();
        for (MultipartFile multipartFile : multipartFiles) {
            try {
                Metadata metadata = new Metadata();
                metadata.add(TikaCoreProperties.RESOURCE_NAME_KEY, multipartFile.getName());
                InputStream inputStream = new BufferedInputStream(multipartFile.getInputStream());
                org.apache.tika.mime.MediaType mediaType = detector.detect(inputStream, metadata);
                String type = mediaType.getBaseType().toString().toLowerCase();
                if (type.contains("heic") || type.contains("heif")) {
                    throw new IllegalArgumentException("Encountered HEIC file in attachments upload");
                }
                attachmentFiles.add(new OlogMultipartFile(multipartFile, inputStream));
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to read multipart file stream or determine file content", e);
                throw new RuntimeException(e);
            }
        }
        return attachmentFiles;
    }

    /**
     * A {@link MultipartFile} implementation with the purpose of providing a custom {@link InputStream}.
     */
    private static final class OlogMultipartFile implements MultipartFile {

        private final MultipartFile originalMultipartFile;
        private final InputStream inputStream;

        public OlogMultipartFile(MultipartFile originalMultipartFile, InputStream inputStream) {
            this.originalMultipartFile = originalMultipartFile;
            this.inputStream = inputStream;
        }

        @Override
        public String getName() {
            return originalMultipartFile.getName();
        }

        @Override
        public String getOriginalFilename() {
            return originalMultipartFile.getOriginalFilename();
        }

        @Override
        public String getContentType() {
            return originalMultipartFile.getContentType();
        }

        @Override
        public boolean isEmpty() {
            return originalMultipartFile.isEmpty();
        }

        @Override
        public long getSize() {
            return originalMultipartFile.getSize();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return originalMultipartFile.getBytes();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            originalMultipartFile.transferTo(dest);
        }
    }

}
