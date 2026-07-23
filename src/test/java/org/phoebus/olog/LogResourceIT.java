package org.phoebus.olog;

import junitx.framework.FileAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.olog.entity.Attachment;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)

@ContextConfiguration(classes = {LogResource.class, LogRepository.class, ElasticConfig.class, GridFsOperations.class})
@WebMvcTest(LogResource.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@SuppressWarnings("unused")
class LogResourceIT extends ResourcesTestBase {

    @Autowired
    private LogResource logResource;

    @Autowired
    private LogbookRepository logbookRepository;
    @Autowired
    private LogRepository logRepository;
    @Autowired
    private GridFsOperations gridOperation;
    @Autowired
    private AttachmentRepository attachmentRepository;


    private static final String testOwner = "log-resource-test";

    // Read the elatic index and type from the application.properties
    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    private String ES_LOGBOOK_INDEX;
    @Value("${elasticsearch.logbook.type:olog_logbook}")
    private String ES_LOGBOOK_TYPE;
    @Value("${elasticsearch.log.index:olog_logs}")
    private String ES_LOG_INDEX;
    @Value("${elasticsearch.log.type:olog_log}")
    private String ES_LOG_TYPE;

    private static Logbook logbook1;
    private static Logbook logbook2;

    private static MultipartFile multipartFile1;
    private static MultipartFile multipartFile2;

    @BeforeAll
    public static void init(@Autowired LogbookRepository logbookRepository, @Autowired LogRepository logRepository) {
        logbook1 = new Logbook("name1", "user");
        logbook2 = new Logbook("name2", "user");

        multipartFile1 = new MultipartFile() {
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

        multipartFile2 = new MultipartFile() {
            @Override
            public String getName() {
                return "Magnolia.jpg";
            }

            @Override
            public String getOriginalFilename() {
                return "Magnolia.jpg";
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
                return getClass().getResourceAsStream("/Magnolia.jpg");
            }

            @Override
            public void transferTo(File dest) throws IllegalStateException {

            }
        };
        logbookRepository.save(logbook1);
        logbookRepository.save(logbook2);
    }

    @AfterAll
    public static void cleanup(@Autowired LogbookRepository logbookRepository, @Autowired LogRepository logRepository) {
        logbookRepository.delete(logbook1);
        logbookRepository.delete(logbook2);
    }

    @Test
    void createLogEntryAndRetrieveAttachment() {
        File testFile = new File("src/test/resources/SampleTextFile_100kb.txt");

        try {
            MockMultipartFile mock = new MockMultipartFile(testFile.getName(), new FileInputStream(testFile));
            Attachment testAttachment = new Attachment(mock, "SampleTextFile_100kb.txt", "");

            Logbook testLogbook = new Logbook("test-logbook-1", testOwner, State.Active);
            logbookRepository.save(testLogbook);

            Log log = Log.LogBuilder.createLog("This is a test entry")
                    .owner(testOwner)
                    .withLogbook(testLogbook)
                    .withAttachment(testAttachment)
                    .build();

            Log createdLog = logRepository.save(log);

            String attachmentId = createdLog.getAttachments().iterator().next().getId();
            Resource a = logResource.getAttachment(createdLog.getId().toString(), testFile.getName()).getBody();

            File foundTestFile = new File("LogResourceIT_attachment_" + testAttachment.getId() + "_" + testAttachment.getFilename());
            Files.copy(a.getInputStream(), foundTestFile.toPath());
            FileAssert.assertBinaryEquals("failed to create log entry with attachment", testFile, foundTestFile);
            Files.delete(foundTestFile.toPath());
            gridOperation.delete(new Query(Criteria.where("_id").is(attachmentId)));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a log entry with one attachment and then update the log entry with an additional attachment.
     * @throws Exception
     */
    @Test
    void testCreateLogEntryWithAttachmentAndUpdateWithNewAttachment() throws Exception {

        Instant now = Instant.now();
        String uuid1 = UUID.randomUUID().toString();
        Attachment attachment1 = new Attachment();
        attachment1.setId(uuid1);
        attachment1.setFilename(uuid1 + "_tulips.jpg");
        String uuid11 = UUID.randomUUID().toString();
        Attachment attachment11 = new Attachment();
        attachment11.setId(uuid11);
        attachment11.setFilename(uuid11 + "_SampleTextFile_100kb.txt");

        Log log = Log.LogBuilder.createLog()
                .title("title")
                .withLogbooks(Set.of(logbook1))
                .source("description1")
                .description("description1")
                .level("Urgent")
                .build();
        SortedSet<Attachment> attachments = new TreeSet<>();
        attachments.add(attachment1);
        attachments.add(attachment11);
        log.setAttachments(attachments);

        MockMultipartFile file1 =
                new MockMultipartFile("files", uuid1 + "_tulips.jpg", "image/jpeg",
                        getClass().getResourceAsStream("/Tulips.jpg").readAllBytes());
        MockMultipartFile file11 =
                new MockMultipartFile("files", uuid11 + "_SampleTextFile_100kb.txt", "text/plain",
                        getClass().getResourceAsStream("/SampleTextFile_100kb.txt").readAllBytes());
        MockMultipartFile log1 = new MockMultipartFile("logEntry", "", "application/json", objectMapper.writeValueAsString(log).getBytes());

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.multipart(HttpMethod.PUT,
                                "/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/multipart")
                        .file(file1)
                        .file(file11)
                        .file(log1)
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data")
                        .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        Log savedLog = objectMapper.readValue(result.getResponse().getContentAsByteArray(), Log.class);
        attachmentRepository.findById(uuid1).orElseThrow(AssertionError::new);
        attachmentRepository.findById(uuid11).orElseThrow(AssertionError::new);
        assertEquals(2, savedLog.getAttachments().size());

        // Update the log entry with one additional attachment
        String uuid2 = UUID.randomUUID().toString();
        Attachment attachment2 = new Attachment();
        attachment2.setId(uuid2);
        attachment2.setFilename(uuid2 + "_magnolia.jpg");

        Log log2 = Log.LogBuilder.createLog()
                .id(savedLog.getId())
                .title("title2")
                .withLogbooks(Set.of(logbook1, logbook2))
                .source("description2")
                .description("description2")
                .level("Urgent")
                .build();
        SortedSet<Attachment> attachments2 = new TreeSet<>();
        attachments2.add(attachment1);
        attachments2.add(attachment11);
        attachments2.add(attachment2);
        log2.setAttachments(attachments2);

        MockMultipartFile file2 =
                new MockMultipartFile("files", uuid2 + "_magnolia.jpg", "image/jpeg",
                        getClass().getResourceAsStream("/Magnolia.jpg").readAllBytes());

        MockMultipartFile log22 = new MockMultipartFile("logEntry", "", "application/json", objectMapper.writeValueAsString(log2).getBytes());

        request =
                MockMvcRequestBuilders.multipart(HttpMethod.POST,
                                "/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/multipart")
                        .file(file2)
                        .file(log22)
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data")
                        .contentType(JSON);
        result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        savedLog = objectMapper.readValue(result.getResponse().getContentAsByteArray(), Log.class);
        assertEquals(3, savedLog.getAttachments().size());
        attachmentRepository.findById(uuid2).orElseThrow(AssertionError::new);

        attachmentRepository.deleteById(uuid1);
        attachmentRepository.deleteById(uuid11);
        attachmentRepository.deleteById(uuid2);
    }

    /**
     * Creates a log entry with one attachment and then update the log entry with an additional attachment,
     * but first attachment is removed from log entry in the update request.
     * @throws Exception
     */
    @Test
    void testCreateLogEntryWithAttachmentAndUpdateWithNewAttachmentRemoveExisting() throws Exception {

        Instant now = Instant.now();
        String uuid1 = UUID.randomUUID().toString();
        Attachment attachment1 = new Attachment();
        attachment1.setId(uuid1);
        attachment1.setFilename(uuid1 + "_tulips.jpg");
        String uuid11 = UUID.randomUUID().toString();
        Attachment attachment11 = new Attachment();
        attachment11.setId(uuid11);
        attachment11.setFilename(uuid11 + "_SampleTextFile_100kb.txt");
        Log log = Log.LogBuilder.createLog()
                .title("title")
                .withLogbooks(Set.of(logbook1))
                .source("description1")
                .description("description1")
                .level("Urgent")
                .build();
        SortedSet<Attachment> attachments = new TreeSet<>();
        attachments.add(attachment1);
        attachments.add(attachment11);
        log.setAttachments(attachments);

        MockMultipartFile file1 =
                new MockMultipartFile("files", uuid1 + "_tulips.jpg", "image/jpeg",
                        getClass().getResourceAsStream("/Tulips.jpg").readAllBytes());
        MockMultipartFile file11 =
                new MockMultipartFile("files", uuid11 + "_SampleTextFile_100kb.txt", "text/plain",
                        getClass().getResourceAsStream("/SampleTextFile_100kb.txt").readAllBytes());

        MockMultipartFile log1 = new MockMultipartFile("logEntry", "", "application/json", objectMapper.writeValueAsString(log).getBytes());

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.multipart(HttpMethod.PUT,
                                "/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/multipart")
                        .file(file1)
                        .file(file11)
                        .file(log1)
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data")
                        .contentType(JSON);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        Log savedLog = objectMapper.readValue(result.getResponse().getContentAsByteArray(), Log.class);
        assertEquals(2, savedLog.getAttachments().size());
        attachmentRepository.findById(uuid1).orElseThrow(AssertionError::new);
        attachmentRepository.findById(uuid11).orElseThrow(AssertionError::new);

        // Update the log entry with one additional attachment
        String uuid2 = UUID.randomUUID().toString();
        Attachment attachment2 = new Attachment();
        attachment2.setId(uuid2);
        attachment2.setFilename(uuid2 + "_magnolia.jpg");

        Log log2 = Log.LogBuilder.createLog()
                .id(savedLog.getId())
                .title("title2")
                .withLogbooks(Set.of(logbook1, logbook2))
                .source("description2")
                .description("description2")
                .level("Urgent")
                .build();
        SortedSet<Attachment> attachments2 = new TreeSet<>();
        attachments2.add(attachment2);
        attachments2.add(attachment11);
        log2.setAttachments(attachments2);

        MockMultipartFile file2 =
                new MockMultipartFile("files", uuid2 + "_magnolia.jpg", "image/jpeg",
                        getClass().getResourceAsStream("/Magnolia.jpg").readAllBytes());

        MockMultipartFile log22 = new MockMultipartFile("logEntry", "", "application/json", objectMapper.writeValueAsString(log2).getBytes());

        request =
                MockMvcRequestBuilders.multipart(HttpMethod.POST,
                                "/" + OlogResourceDescriptors.LOG_RESOURCE_URI + "/multipart")
                        .file(file2)
                        .file(log22)
                        .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION)
                        .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data")
                        .contentType(JSON);
        result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        savedLog = objectMapper.readValue(result.getResponse().getContentAsByteArray(), Log.class);
        assertEquals(2, savedLog.getAttachments().size());
        attachmentRepository.findById(uuid2).orElseThrow(AssertionError::new);

        attachmentRepository.deleteById(uuid1);
        attachmentRepository.deleteById(uuid11);
        attachmentRepository.deleteById(uuid2);
    }
}
