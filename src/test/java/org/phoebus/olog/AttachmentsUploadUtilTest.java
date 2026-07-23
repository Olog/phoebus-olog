/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 */

package org.phoebus.olog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.olog.entity.Attachment;
import org.phoebus.olog.entity.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ContextHierarchy({@ContextConfiguration(classes = {ResourcesTestConfig.class, LogResourceTestConfig.class})})
@WebMvcTest(LogResource.class)
public class AttachmentsUploadUtilTest {

    @Autowired
    private AttachmentsUploadUtil attachmentsUploadUtil;

    @Test
    public void testAreMultipartFilesOrphaned() {
        Log log = Log.LogBuilder.createLog()
                .id(1L)
                .build();
        Attachment attachment1 = new Attachment();
        attachment1.setFilename("filename1.txt");
        SortedSet<Attachment> attachments = new TreeSet<>();
        attachments.add(attachment1);
        log.setAttachments(attachments);
        MockMultipartFile file1 =
                new MockMultipartFile("files", "filename1.txt", "text/plain", "some xml".getBytes());

        assertFalse(attachmentsUploadUtil.areMultipartFilesOrphaned(log, new MultipartFile[]{file1}));

        MockMultipartFile file2 =
                new MockMultipartFile("files", "filename2.txt", "text/plain", "some xml".getBytes());

        assertTrue(attachmentsUploadUtil.areMultipartFilesOrphaned(log, new MultipartFile[]{file1, file2}));

    }

    @Test
    public void testGetAttachmentsWithFileCounterpartZeroLengthMultipartFiles() {
        Log log = Log.LogBuilder.createLog()
                .id(1L)
                .build();

        Attachment attachment1 = new Attachment();
        attachment1.setFilename("filename1.txt");
        Attachment attachment2 = new Attachment();
        attachment2.setFilename("filename2.txt");
        SortedSet<Attachment> attachments = new TreeSet<>();
        attachments.add(attachment1);
        attachments.add(attachment2);
        log.setAttachments(attachments);

        Set<Attachment> attachmentsWithFileCounterpart = attachmentsUploadUtil.getAttachmentsWithFileCounterpart(log, new MultipartFile[]{});

        assertTrue(attachmentsWithFileCounterpart.isEmpty());

    }

    @Test
    public void testGetAttachmentsWithFileCounterpartNullMultipartFiles() {
        Log log = Log.LogBuilder.createLog()
                .id(1L)
                .build();

        Attachment attachment1 = new Attachment();
        attachment1.setFilename("filename1.txt");
        Attachment attachment2 = new Attachment();
        attachment2.setFilename("filename2.txt");
        SortedSet<Attachment> attachments = new TreeSet<>();
        attachments.add(attachment1);
        attachments.add(attachment2);
        log.setAttachments(attachments);

        Set<Attachment> attachmentsWithFileCounterpart = attachmentsUploadUtil.getAttachmentsWithFileCounterpart(log, null);

        assertTrue(attachmentsWithFileCounterpart.isEmpty());

    }

    @Test
    public void testGetAttachmentsWithFileCounterpartTooManyFiles() {
        Log log = Log.LogBuilder.createLog()
                .id(1L)
                .build();

        Attachment attachment1 = new Attachment();
        attachment1.setFilename("filename1.txt");
        SortedSet<Attachment> attachments = new TreeSet<>();
        attachments.add(attachment1);
        log.setAttachments(attachments);

        MockMultipartFile file1 =
                new MockMultipartFile("files", "filename1.txt", "text/plain", "some xml".getBytes());
        MockMultipartFile file2 =
                new MockMultipartFile("files", "filename2.txt", "text/plain", "some xml".getBytes());

        Set<Attachment> attachmentsWithFileCounterpart = attachmentsUploadUtil.getAttachmentsWithFileCounterpart(log, new MultipartFile[]{file1, file2});

        assertEquals(1, attachmentsWithFileCounterpart.size());

    }

    @Test
    public void testGetAttachmentsWithFileCounterpartAllFiles() {
        Log log = Log.LogBuilder.createLog()
                .id(1L)
                .build();

        Attachment attachment1 = new Attachment();
        attachment1.setFilename("filename1.txt");
        Attachment attachment2 = new Attachment();
        attachment2.setFilename("filename2.txt");
        SortedSet<Attachment> attachments = new TreeSet<>();
        attachments.add(attachment1);
        attachments.add(attachment2);
        log.setAttachments(attachments);

        MockMultipartFile file1 =
                new MockMultipartFile("files", "filename1.txt", "text/plain", "some xml".getBytes());
        MockMultipartFile file2 =
                new MockMultipartFile("files", "filename2.txt", "text/plain", "some xml".getBytes());

        Set<Attachment> attachmentsWithFileCounterpart = attachmentsUploadUtil.getAttachmentsWithFileCounterpart(log, new MultipartFile[]{file1, file2});

        assertEquals(2, attachmentsWithFileCounterpart.size());

    }

    @Test
    public void testGetAttachmentsWithFileCounterpart() {
        Log log = Log.LogBuilder.createLog()
                .id(1L)
                .build();

        Attachment attachment1 = new Attachment();
        attachment1.setFilename("filename1.txt");
        Attachment attachment2 = new Attachment();
        attachment2.setFilename("filename2.txt");
        SortedSet<Attachment> attachments = new TreeSet<>();
        attachments.add(attachment1);
        attachments.add(attachment2);
        log.setAttachments(attachments);

        MockMultipartFile file1 =
                new MockMultipartFile("files", "filename1.txt", "text/plain", "some xml".getBytes());

        Set<Attachment> attachmentsWithFileCounterpart = attachmentsUploadUtil.getAttachmentsWithFileCounterpart(log, new MultipartFile[]{file1});

        assertEquals(1, attachmentsWithFileCounterpart.size());

    }

    @Test
    public void testIsAttachmentUploadConsistentNullFilesAndEmptyAttachment() {
        assertTrue(attachmentsUploadUtil.isAttachmentUploadConsistent(Log.LogBuilder.createLog().build(), null));
    }

    @Test
    public void testIsAttachmentUploadConsistentNonNullFilesAndEmptyAttachment() {
        MockMultipartFile file1 =
                new MockMultipartFile("files", "filename1.txt", "text/plain", "some xml".getBytes());

        assertFalse(attachmentsUploadUtil.isAttachmentUploadConsistent(Log.LogBuilder.createLog().build(), new MultipartFile[]{file1}));
    }

    @Test
    public void testIsAttachmentUploadConsistentNonMatchingAttachmentsFileNames() {
        MockMultipartFile file1 =
                new MockMultipartFile("files", "filename1.txt", "text/plain", "some xml".getBytes());
        Log log = Log.LogBuilder.createLog().build();
        Attachment attachment = new Attachment();
        attachment.setFilename("bad");
        log.getAttachments().add(attachment);

        assertFalse(attachmentsUploadUtil.isAttachmentUploadConsistent(log, new MultipartFile[]{file1}));
    }

    @Test
    public void testAnalyzeHeic() throws IOException {
        MultipartFile multipartFile = new MultipartFile() {
            @Override
            public String getName() {
                return "IMG_1.heic";
            }

            @Override
            public String getOriginalFilename() {
                return "IMG_1.heic";
            }

            @Override
            public String getContentType() {
                return "image/heic";
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return 0;
            }

            @Override
            public byte[] getBytes() {
                return new byte[0];
            }

            @Override
            public InputStream getInputStream() {
                return getClass().getResourceAsStream("/IMG_1.heic");
            }

            @Override
            public void transferTo(File dest) throws IllegalStateException {

            }
        };

        assertThrows(IllegalArgumentException.class, () -> {
            attachmentsUploadUtil.checkSupportedAttachmentTypes(new MultipartFile[]{multipartFile});
        });
    }

    @Test
    public void testAnalyzeNotHeic() {
        MultipartFile multipartFile = new MultipartFile() {
            @Override
            public String getName() {
                return "Tulips.jpg";
            }

            @Override
            public String getOriginalFilename() {
                return "Tulips.jpg";
            }

            @Override
            public String getContentType() {
                return "image/jpg";
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return 0;
            }

            @Override
            public byte[] getBytes() {
                return new byte[0];
            }

            @Override
            public InputStream getInputStream() {
                return getClass().getResourceAsStream("/Tulips.jpg");
            }

            @Override
            public void transferTo(File dest) throws IllegalStateException {

            }
        };
        List<MultipartFile> multipartFiles = attachmentsUploadUtil.checkSupportedAttachmentTypes(new MultipartFile[]{multipartFile});
        assertEquals(1, multipartFiles.size());
    }
}
