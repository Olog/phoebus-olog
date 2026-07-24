/*
 * Copyright (c) 2010-2020 Brookhaven National Laboratory
 * Copyright (c) 2010-2020 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * Copyright (C) 2026 European Spallation Source ERIC.
 * All rights reserved. Use is subject to license terms and conditions.
 */
package org.phoebus.olog;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.CollectionUtils;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.IOException;
import java.security.Principal;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.olog.OlogResourceDescriptors.LOG_RESOURCE_URI;
import static org.phoebus.util.time.TimestampFormats.MILLI_PATTERN;
import org.phoebus.olog.ai.LogEntryCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Resource for handling the requests to ../logs
 *
 * @author kunal
 */
@RestController(value="Logs")
@RequestMapping(LOG_RESOURCE_URI)
@io.swagger.v3.oas.annotations.tags.Tag(name = "Logs")
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
    private AttachmentsUploadUtil attachmentsUploadUtil;

    @SuppressWarnings("unused")
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Custom HTTP header that client may send in order to identify itself. This is logged for some of the
     * endpoints in this controller.
     */
    private static final String OLOG_CLIENT_INFO_HEADER = "X-Olog-Client-Info";

    private final Object logGroupSyncObject = new Object();

    @GetMapping("{logId}")
    @SuppressWarnings("unused")
    @Operation(summary = "Get a log by Id")
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
    @Operation(summary = "Get an archived log by Id")
    public SearchResult getArchivedLog(@PathVariable(name = "logId") String logId) {
        return logRepository.findArchivedById(logId);
    }

    @GetMapping("/attachments/{logId}/{attachmentName}")
    @Operation(summary = "Get an attachment of a determined log", operationId = "getLogAttachment")
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
    @Operation(summary = "Finds matching log entries",
	description = "See /Ologs/logs/search (alias) for parameter details."
			+ "",
	operationId = "findLogs")
    @Parameters({
        @Parameter(name = "text", description = "A list of keywords which are present in the log entry description", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "fuzzy", description = "Allow fuzzy searches", schema = @Schema(type = "string"), required = false, example = "true|false"),
        @Parameter(name = "phrase", description = "Finds log entries with the exact same word/s", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "owner", description = "Finds log entries with the given owner", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "start", description = "Search for log entries created after given time instant", schema = @Schema(type = "string"), required = false, example = "2021-01-20 12:00:00.123"),
        @Parameter(name = "end", description = "Search for log entries created before the given time instant", schema = @Schema(type = "string"), required = false, example = "2021-01-21 12:00:00.123"),
        @Parameter(name = "includeevents", description = "A flag to include log event times when", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "tags", description = "Search for log entries with at least one of the given tags", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "logbooks", description = "Search for log entries with at least one of the given logbooks", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "attachments", description = "To search for entries with at least one attachment", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "size", description = "The number of log entries to be returned within each page", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "attachments", description = "The page number, i.e page 1 is the 1 to 1+size log. entries matching the search", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "sort", description = "Order the search results based on create time", schema = @Schema(type = "string"), required = false, example = "up|down"),
    })

    public ResponseEntity<?> findLogs(@RequestHeader(value = OLOG_CLIENT_INFO_HEADER, required = false, defaultValue = "n/a") String clientInfo, @RequestParam MultiValueMap<String, String> allRequestParams) {
        ResponseEntity<?> responseEntity = search(clientInfo, allRequestParams);
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
    @Operation(summary = "Finds matching log entries",
	description = "Finds matching log entries"
			+ "\n"
			+ "For time based search requests the client may specify a **tz** parameter indicating the client's time zone.\n"
			+ "The format must be recognized as a valid zone identifier, see for instance <https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html>.\n"
			+ "If the client does not specify the time zone, the time zone of the service is used to compute start end end timestamps.\n"
			+ "An invalid time zone specifier will result in a HTTP 400 (bad request) response.\n"
			+ "\n"
			+ "Example:\n"
			+ "\n"
			+ "**GET** <https://localhost:8181/Olog/logs/search?desc=dump&logbooks=Operations>\n"
			+ "\n"
			+ "The above search request will return all log entires with the term \"dump\" in their\n"
			+ "descriptions and which are part of the Operations logbook.\n"
			+ "\n"
			+ "Retrieving an attachment of a log entry\n"
			+ "\n"
			+ "**GET** <https://localhost:8181/Olog/logs/attachments>/\\{logId}/\\{filename}\n"
			+ "\n"
			+ "Find entries with at least one attachment of type 'image'\n"
			+ "\n"
			+ "**GET** <https://localhost:8181/Olog/logs/search?attachments=image>\n"
			+ "",
	operationId = "searchLogs")
    @Parameters({
        @Parameter(name = "text", description = "A list of keywords which are present in the log entry description", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "fuzzy", description = "Allow fuzzy searches", schema = @Schema(type = "string"), required = false, example = "true|false"),
        @Parameter(name = "phrase", description = "Finds log entries with the exact same word/s", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "owner", description = "Finds log entries with the given owner", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "start", description = "Search for log entries created after given time instant", schema = @Schema(type = "string"), required = false, example = "2021-01-20 12:00:00.123"),
        @Parameter(name = "end", description = "Search for log entries created before the given time instant", schema = @Schema(type = "string"), required = false, example = "2021-01-21 12:00:00.123"),
        @Parameter(name = "includeevents", description = "A flag to include log event times when", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "tags", description = "Search for log entries with at least one of the given tags", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "logbooks", description = "Search for log entries with at least one of the given logbooks", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "attachments", description = "To search for entries with at least one attachment", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "size", description = "The number of log entries to be returned within each page", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "attachments", description = "The page number, i.e page 1 is the 1 to 1+size log. entries matching the search", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "sort", description = "Order the search results based on create time", schema = @Schema(type = "string"), required = false, example = "up|down"),
    })
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
    @Operation(summary = "Create a new log entry",
	description = "Creates a new log entry. If the <code>inReplyTo</code> parameters identifies an existing log entry,\n"
			+ " this method will treat the new log entry as a reply.\n"
			+ " <p>\n"
			+ " This may return a HTTP 400 if for instance <code>inReplyTo</code> does not identify an existing log entry,\n"
			+ " or if the logbooks listed in the {@link Log} object contains invalid (i.e. non-existing) logbooks.\n"
			+ " </p>\n"
			+ " <p>\n"
			+ " Primary use case is upload of log entry without attachments as this type of request is easier to\n"
			+ " construct, i.e. client need not create a request with multipart items.\n"
			+ " </p>",
	operationId = "createLog")
    public Log createLog(@RequestHeader(value = OLOG_CLIENT_INFO_HEADER, required = false, defaultValue = "n/a") String clientInfo,
                         @RequestParam(name = "markup", required = false) String markup,
                         @RequestParam(name = "notifyWsClients", required = false, defaultValue = "true") boolean notifyWsClients,
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
        eventPublisher.publishEvent(new LogEntryCreatedEvent(this, newLogEntry));
        sendToNotifiers(newLogEntry);

        if (notifyWsClients) {
            webSocketService.sendMessageToClients(new WebSocketMessage(MessageType.NEW_LOG_ENTRY, null));
        }

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
     * @param logEntry   A {@link Log} object to be persisted. If log entry does not specify any attachments,
     *                   the {@link Log#getAttachments()} field is non-null and empty.
     * @param markup     Optional string identifying the wanted markup scheme.
     * @param inReplyTo  Optional log entry id specifying to which log entry the new log entry is a reply.
     * @param files      Optional array of {@link MultipartFile}s representing attachments. This is <code>null</code>
     *                   if log entry does not contain any attachments.
     * @param principal  The authenticated {@link Principal} of the request.
     * @return The persisted {@link Log} object.
     */
    @Operation(summary = "Create a new log entry (multipart)",
    				description = "```json\n"
    						+ "{\n"
    						+ "   \"owner\":\"log\",\n"
    						+ "     \"description\":\"Beam Dump due to Major power dip Current Alarms Booster transmitter switched back to lower state.\",\n"
    						+ "     \"level\":\"Info\",\n"
    						+ "     \"title\":\"Some title\",\n"
    						+ "     \"logbooks\":[\n"
    						+ "        {\n"
    						+ "           \"name\":\"Operations\"\n"
    						+ "        }\n"
    						+ "     ],\n"
    						+ "     \"attachments\":[\n"
    						+ "        {\"id\": \"82dd67fa-09df-11ee-be56-0242ac120002\", \"filename\":\"MyScreenShot.png\"},\n"
    						+ "        {\"id\": \"c02948ad-4bbd-432f-aa4d-a687a54f8d40\", \"filename\":\"MySpreadsheet.xlsx\"}\n"
    						+ "     ]\n"
    						+ "}\n"
    						+ "```\n"
    						+ "\n"
    						+ "**NOTE** Attachment ids must be unique, e.g. UUID. When creating a log entry - optionally with attachments - client **must**:\n"
    						+ "\n"
    						+ "Use a multipart request and set the Content-Type to \"multipart/form-data\", even if no attachments are present.\n"
    						+ "\n"
    						+ "#. If attachments are present: add one request part per attachment file, in the order they appear in the log entry. Each\n"
    						+ "file must be added using \"files\" as the name for the part.\n"
    						+ "#. Add the log entry as a request part with content type \"application/json\". The name of the part must be \"logEntry\".\n"
    						+ "\nThis may return a HTTP 400 if for instance <code>inReplyTo</code> does not identify an existing log entry,\n"
    						+ "or if the logbooks listed in the {@link Log} object contains invalid (i.e. non-existing) logbooks."
    						+ "Client must also be prepared to handle a HTTP 413 (payload too large) response in case the attached files exceed\n"
    						+ "file and request size limits configured in the service.",
    				operationId = "createLogMultipart")
    @PutMapping("/multipart")
    public Log createLog(@RequestHeader(value = OLOG_CLIENT_INFO_HEADER, required = false, defaultValue = "n/a") String clientInfo,
                         @RequestParam(name = "markup", required = false) String markup,
                         @RequestParam(name = "inReplyTo", required = false, defaultValue = "-1") String inReplyTo,
                         @RequestPart("logEntry") Log logEntry,
                         @RequestPart(value = "files", required = false) MultipartFile[] files,
                         @AuthenticationPrincipal Principal principal) {

        if (!attachmentsUploadUtil.isAttachmentUploadConsistent(logEntry, files)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.ATTACHMENT_DATA_INVALID);
        }

        List<MultipartFile> multipartFiles = null;

        if (files != null) {
            try {
                multipartFiles = attachmentsUploadUtil.checkSupportedAttachmentTypes(files);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.ATTACHMENT_HEIC_NOT_SUPPORTED);
            }
        }

        // Attachments consistency checked, safe to create log entry
        Log newLogEntry = createLog(clientInfo, markup, false, inReplyTo, logEntry, principal);

        if (files != null) {
            try {
                List<Attachment> savedAttachments = saveAttachments(logEntry, multipartFiles);
                newLogEntry.setAttachments(new TreeSet<>(savedAttachments));
                newLogEntry = logRepository.update(newLogEntry);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to persist attachments for log entry " + newLogEntry.getId(), e);
                 throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist attachments", e);
            }
        }

        Long id = newLogEntry.getId();
        webSocketService.sendMessageToClients(new WebSocketMessage(MessageType.NEW_LOG_ENTRY, null));

        logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.LOG_ENTRY_ID_CREATED_FROM, id, clientInfo));

        return newLogEntry;
    }

    /**
     * Saves the content of the {@link MultipartFile} to the database.
     *
     * @param file                    A {@link MultipartFile} representing the attachment contents
     * @param filename                The file name as defined by the client, e.g. file name on disk.
     * @param id                      A unique id for the attachment.
     * @param fileMetadataDescription Description of the content
     * @return A {@link Attachment} object representing the saved file.
     */
    private Attachment saveAttachment(MultipartFile file,
                                      String filename,
                                      String id,
                                      String fileMetadataDescription) {
        filename = filename == null || filename.isEmpty() ? file.getName() : filename;
        fileMetadataDescription = fileMetadataDescription == null || fileMetadataDescription.isEmpty()
                ? file.getContentType()
                : fileMetadataDescription;
        Attachment attachment = new Attachment(id, file, filename, fileMetadataDescription);
        // Store the attachment
        return attachmentRepository.save(attachment);
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
     * @param logId           The log id of the entry subject to update. It must exist, i.e. it is not created of not found.
     * @param markup          Markup strategy, if any.
     * @param notifyWsClients Optional flag indicating if websocket clients should be notified when update is completed. Defaults
     *                        to <code>true</code> (in a REST call), but since this method is also called from another
     *                        endpoint it would be <code>false</code> to avoid multiple notifications.
     * @param log             The log record data as sent by client.
     * @param principal       The authenticated {@link Principal} of the request.
     * @return The updated log record, or HTTP status 404 if the log record does not exist. If the path
     * variable does not match the id in the log record, HTTP status 400 (bad request) is returned.
     */
    @SuppressWarnings("unused")
    @PostMapping("/{logId}")
    @Operation(summary = "Update a log entry",
	description = "Updates existing log record. Data sent by client is saved, i.e. if client specifies a shorter list\n"
			+ "of logbooks or tags, the updated log record will reflect that. However, the following data is NOT updated:\n"
			+ "<ul>\n"
			+ "    <li>Attachments</li>\n"
			+ "    <li>Created date</li>\n"
			+ "    <li>Events</li>\n"
			+ "</ul>\n"
			+ "Notifiers - if such have been registered - are not called.",
	operationId = "updateLog")
    public Log updateLog(@PathVariable(name = "logId") String logId,
                         @RequestParam(name = "markup", required = false) String markup,
                         @RequestParam(name = "notifyWsClients", required = false, defaultValue = "true") boolean notifyWsClients,
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

            // Attachments are not updated via this endpoint; use /logs/multipart for attachment changes.

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

            Log updatedLog = logRepository.update(persistedLog);
            eventPublisher.publishEvent(new LogEntryCreatedEvent(this, updatedLog));
            if (notifyWsClients) {
                webSocketService.sendMessageToClients(new WebSocketMessage(MessageType.LOG_ENTRY_UPDATED, persistedLog.getId().toString()));
            }
            return updatedLog;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MessageFormat.format(TextUtil.LOG_NOT_RETRIEVED, logId));
        }
    
    }

    @PostMapping("/multipart")
    public Log updateLog(@RequestParam(name = "markup", required = false) String markup,
                         @RequestPart("logEntry") Log logEntry,
                         @RequestPart(value = "files", required = false) MultipartFile[] files,
                         @AuthenticationPrincipal Principal principal) {
        if (logEntry == null || logEntry.getId() == null || logEntry.getId() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.LOG_ENTRY_ID_MISSING);
        }

        if (!attachmentsUploadUtil.isAttachmentUploadConsistent(logEntry, files)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.ATTACHMENT_DATA_INVALID);
        }

        Optional<Log> exitingLogEntryOptional = logRepository.findById(Long.toString(logEntry.getId()));
        if (exitingLogEntryOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    MessageFormat.format(TextUtil.LOG_NOT_FOUND, logEntry.getId()));
        }

        // Check if additional attachments are uploaded correctly
        List<MultipartFile> multipartFiles = null;

        if (files != null) {
            try {
                multipartFiles = attachmentsUploadUtil.checkSupportedAttachmentTypes(files);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TextUtil.ATTACHMENT_HEIC_NOT_SUPPORTED);
            }
        }

        Log updatedLog = updateLog(Long.toString(logEntry.getId()),
                markup,
                false,
                logEntry,
                principal);

         // Save uploaded files first (if any) so we don't persist attachment refs that don't exist yet.
        if (files != null) {
            saveAttachments(logEntry, multipartFiles);
         }
         // Persist the attachment list exactly as provided by the client (supports removal as well).
         updatedLog.setAttachments(logEntry.getAttachments() != null ? new TreeSet<>(logEntry.getAttachments()) : new TreeSet<>());
         updatedLog = logRepository.update(updatedLog);

        webSocketService.sendMessageToClients(new WebSocketMessage(MessageType.LOG_ENTRY_UPDATED, updatedLog.getId().toString()));

        return updatedLog;

    }

    @SuppressWarnings("unused")
    @PostMapping(value = "/group")
    @Operation(summary = "Grouf logs", operationId = "groupLogs")
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
        List<String> propertyNames = log.getProperties().stream().map(Property::getName).toList();
        List<CompletableFuture<Property>> completableFutures =
                propertyProviders.stream()
                        .map(propertyProvider -> CompletableFuture.supplyAsync(() -> propertyProvider.getProperty(log), executorService))
                        .toList();

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
    @Operation(summary = "Get RSS feed", description = "GET method for retrieving an RSS feed of channels", operationId = "getRssFeed")
    @Parameters({
        @Parameter(name = "text", description = "A list of keywords which are present in the log entry description", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "fuzzy", description = "Allow fuzzy searches", schema = @Schema(type = "string"), required = false, example = "true|false"),
        @Parameter(name = "phrase", description = "Finds log entries with the exact same word/s", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "owner", description = "Finds log entries with the given owner", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "start", description = "Search for log entries created after given time instant", schema = @Schema(type = "string"), required = false, example = "2021-01-20 12:00:00.123"),
        @Parameter(name = "end", description = "Search for log entries created before the given time instant", schema = @Schema(type = "string"), required = false, example = "2021-01-21 12:00:00.123"),
        @Parameter(name = "includeevents", description = "A flag to include log event times when", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "tags", description = "Search for log entries with at least one of the given tags", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "logbooks", description = "Search for log entries with at least one of the given logbooks", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "attachments", description = "To search for entries with at least one attachment", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "size", description = "The number of log entries to be returned within each page", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "attachments", description = "The page number, i.e page 1 is the 1 to 1+size log. entries matching the search", schema = @Schema(type = "string"), required = false, example = "*"),
        @Parameter(name = "sort", description = "Order the search results based on create time", schema = @Schema(type = "string"), required = false, example = "up|down"),
    })
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
     * Saves {@link Attachment}s to the repository.
     * <p>
     * <b>NOTE</b> before calling this callee needs to ensure that the provided parameters are
     * checked for consistency.
     * </p>
     *
     * @param logEntry       A log entry including {@link Attachment}s
     * @param multipartFiles The file uploaded by client
     * @return A {@link List} of {@link Attachment}s representing the persisted attachment files.
     */
    protected List<Attachment> saveAttachments(Log logEntry, List<MultipartFile> multipartFiles) {
        List<Attachment> savedAttachments = new ArrayList<>();

        for (MultipartFile multipartFile : multipartFiles) {
            String originalFileName = multipartFile.getOriginalFilename();
            Optional<Attachment> attachment =
                    logEntry.getAttachments().stream()
                            .filter(a -> a.getFilename() != null && a.getFilename().equals(originalFileName)).findFirst();
            savedAttachments.add(saveAttachment(multipartFile,
                    originalFileName,
                    attachment.get().getId(),
                    attachment.get().getFileMetadataDescription()));
        }

        return savedAttachments;
    }


}