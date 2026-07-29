/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 */

package org.phoebus.olog;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.phoebus.olog.entity.Attachment;
import org.phoebus.olog.entity.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A set of tools for the purpose of verifying that attachments uploaded by clients are valid.
 */
public class AttachmentsUploadUtil {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private Detector detector;

    private final Logger logger = Logger.getLogger(AttachmentsUploadUtil.class.getName());

    /**
     * Checks that attachment upload is consistent with the log entry's attachments part:
     * <ul>
     *     <li>For each multipart file the original file name must match exactly one {@link Attachment}'s filename.</li>
     *     <li>Each {@link MultipartFile} must have a counterpart in the {@link Attachment}s list of the log entry.</li>
     *     <li>{@link Attachment}s listed in the log entry that do not have a {@link MultipartFile} counterpart must
     *      exist in the {@link Attachment}'s repository.</li>
     * </ul>
     *
     * @param logEntry       Log entry with attachments
     * @param multipartFiles An array of {@link MultipartFile}s as provided by the web layer.
     * @return <code>true</code> if attachments data is consistent, otherwise <code>false</code>
     */
    protected boolean isAttachmentUploadConsistent(Log logEntry, MultipartFile[] multipartFiles) {

        Set<Attachment> attachmentsWithFileCounterpart = getAttachmentsWithFileCounterpart(logEntry, multipartFiles);
        Collection<Attachment> attachmentsWithoutFileCounterpart =
                CollectionUtils.removeAll(logEntry.getAttachments(), attachmentsWithFileCounterpart);
        if (!areAttachmentsPersisted(attachmentsWithoutFileCounterpart)) {
            return false;
        }

        if (areMultipartFilesOrphaned(logEntry, multipartFiles)) {
            return false;
        }

        if (!attachmentsWithFileCounterpart.isEmpty()) {
            for (MultipartFile multipartFile : multipartFiles) {
                String originalFileName = multipartFile.getOriginalFilename();
                List<Attachment> attachment =
                        attachmentsWithFileCounterpart.stream()
                                .filter(a -> a.getFilename() != null && a.getFilename().equals(originalFileName)).toList();
                if (attachment.size() != 1) { // Should not happen if client behaves correctly
                    logger.log(Level.WARNING, () -> MessageFormat.format(TextUtil.ATTACHMENT_FILE_NOT_MATCHED_META_DATA, originalFileName));
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    /**
     * Checks if all the {@link Attachment}s provided exist in the {@link Attachment}s storage.
     *
     * @param attachments {@link Collection} of {@link Attachment}s
     * @return <code>false</code> if any of the {@link Attachment}s is not already stored.
     */
    protected boolean areAttachmentsPersisted(Collection<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            if (!attachmentRepository.existsById(attachment.getId())) {
                logger.log(Level.WARNING, "Attachment does not exist: " + attachment);
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the log entry submissions specifies {@link MultipartFile}s without a counterpart in the
     * {@link Attachment}s list
     *
     * @param logEntry       The log entry as uploaded by client.
     * @param multipartFiles The {@link MultipartFile}s as uploaded by client.
     * @return <code>true</code> if at least one {@link MultipartFile} does not have an {@link Attachment} counterpart.
     */
    protected boolean areMultipartFilesOrphaned(Log logEntry, MultipartFile[] multipartFiles) {
        if (multipartFiles == null || multipartFiles.length == 0) {
            return false;
        }
        for (MultipartFile multipartFile : multipartFiles) {
            String originalFileName = multipartFile.getOriginalFilename();
            Optional<Attachment> attachmentOptional =
                    logEntry.getAttachments().stream()
                            .filter(a -> a.getFilename() != null && a.getFilename().equals(originalFileName)).findFirst();
            if (attachmentOptional.isEmpty()) {
                logger.log(Level.WARNING, "Orphaned file: " + originalFileName + " not listed in attachments list");
                return true;
            }
        }
        return false;
    }

    /**
     * Determines which of the {@link Attachment}s that have a counterpart in the {@link MultipartFile} array.
     *
     * @param logEntry       The log entry as uploaded by client.
     * @param multipartFiles The {@link MultipartFile}s as uploaded by client.
     * @return An empty {@link SortedSet} if <code>multipartFiles</code> is <code>null</code> or empty, otherwise
     * a {@link Set} containing the attachments that do have a {@link MultipartFile} counterpart.
     */
    protected Set<Attachment> getAttachmentsWithFileCounterpart(Log logEntry, MultipartFile[] multipartFiles) {
        if (multipartFiles == null || multipartFiles.length == 0) {
            return Collections.emptySet();
        }
        SortedSet<Attachment> attachmentsWithFileCounterpart = new TreeSet<>();
        for (MultipartFile multipartFile : multipartFiles) {
            String originalFileName = multipartFile.getOriginalFilename();
            Optional<Attachment> attachmentOptional =
                    logEntry.getAttachments().stream()
                            .filter(a -> a.getFilename() != null && a.getFilename().equals(originalFileName)).findFirst();
            attachmentOptional.ifPresent(attachmentsWithFileCounterpart::add);
        }
        return attachmentsWithFileCounterpart;
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
     * @param multipartFiles Non-null array of {@link MultipartFile}s from client as intercepted by the endpoint.
     * @return A {@link List} of {@link MultipartFile}s where the {@link InputStream} can be consumed again.
     * @throws IllegalArgumentException if an unsupported content type is encontered.
     *
     */
    protected List<MultipartFile> checkSupportedAttachmentTypes(MultipartFile[] multipartFiles) {

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
    private record OlogMultipartFile(MultipartFile originalMultipartFile,
                                     InputStream inputStream) implements MultipartFile {

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
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            originalMultipartFile.transferTo(dest);
        }
    }
}
