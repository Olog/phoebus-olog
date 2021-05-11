package org.phoebus.olog;

import gov.bnl.olog.LogRepository;
import gov.bnl.olog.LogResource;
import gov.bnl.olog.LogbookRepository;
import gov.bnl.olog.entity.Attachment;
import gov.bnl.olog.entity.Log;
import gov.bnl.olog.entity.Logbook;
import gov.bnl.olog.entity.State;
import junitx.framework.FileAssert;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

@RunWith(SpringRunner.class)
@WebMvcTest(LogResource.class)
@TestPropertySource(locations = "classpath:test_application.properties")
public class LogResourceIT {

    @Autowired
    LogResource logResource;

    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient client;

    @Autowired
    private LogbookRepository logbookRepository;
    @Autowired
    private LogRepository logRepository;
    @Autowired
    private GridFsOperations gridOperation;

    private static String testOwner = "log-resource-test";
    private static Logbook testLogbook;

    // Read the elatic index and type from the application.properties
    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    private String ES_LOGBOOK_INDEX;
    @Value("${elasticsearch.logbook.type:olog_logbook}")
    private String ES_LOGBOOK_TYPE;
    @Value("${elasticsearch.log.index:olog_logs}")
    private String ES_LOG_INDEX;
    @Value("${elasticsearch.log.type:olog_log}")
    private String ES_LOG_TYPE;

    @Test
    public void retrieveAttachment() throws IOException {
        File testFile = new File("src/test/resources/SampleTextFile_100kb.txt");

        try {
            MockMultipartFile mock = new MockMultipartFile(testFile.getName(), new FileInputStream(testFile));
            Attachment testAttachment = new Attachment(mock, "SampleTextFile_100kb.txt", "");

            testLogbook = new Logbook("test-logbook-1", testOwner, State.Active);
            logbookRepository.save(testLogbook);

            Log log = Log.LogBuilder.createLog("This is a test entry")
                    .owner(testOwner)
                    .withLogbook(testLogbook)
                    .withAttachment(testAttachment)
                    .build();

            Log createdLog = logRepository.save(log);

            String attachmentId = createdLog.getAttachments().iterator().next().getId();
            Resource a = logResource.findResources(createdLog.getId().toString(), testFile.getName()).getBody();

            File foundTestFile = new File("LogResourceIT_attachment_" + testAttachment.getId() + "_" + testAttachment.getFilename());
            Files.copy(a.getInputStream(), foundTestFile.toPath());
            FileAssert.assertBinaryEquals("failed to create log entry with attachment", testFile, foundTestFile);
            Files.delete(foundTestFile.toPath());
            gridOperation.delete(new Query(Criteria.where("_id").is(attachmentId)));

            // Manual cleanup since Olog does not delete things
            client.delete(new DeleteRequest(ES_LOG_INDEX, ES_LOG_TYPE, createdLog.getId().toString()),
                    RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Manual cleanup since Olog does not delete things
            client.delete(new DeleteRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, testLogbook.getName()),
                    RequestOptions.DEFAULT);
        }
    }
}
