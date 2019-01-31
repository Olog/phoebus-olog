package edu.msu.nscl.olog;

import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOGBOOK_INDEX;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOGBOOK_TYPE;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOG_INDEX;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOG_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.Resource;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;

import edu.msu.nscl.olog.entity.Attachment;
import edu.msu.nscl.olog.entity.Log;
import edu.msu.nscl.olog.entity.Logbook;
import edu.msu.nscl.olog.entity.State;
import junitx.framework.FileAssert;

@RunWith(SpringRunner.class)
@WebMvcTest(LogResource.class)
public class LogResourceIT
{

    @Autowired
    LogResource logResource;
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;
    @Autowired
    private LogbookRepository logbookRepository;
    @Autowired
    private LogRepository logRepository;
    @Autowired
    private GridFsOperations gridOperation;

    private static String testOwner = "log-resource-test";
    private static Logbook testLogbook;

    @Test
    public void retrieveAttachment()
    {
        File testFile = new File("src/test/resources/SampleTextFile_100kb.txt");

        try
        {
            MockMultipartFile mock = new MockMultipartFile(testFile.getName(), new FileInputStream(testFile));
            Attachment testAttachment = new Attachment(mock, "SampleTextFile_100kb.txt", "");

            testLogbook = new Logbook("test-logbook-1", testOwner , State.Active);
            logbookRepository.index(testLogbook);

            Log log = Log.LogBuilder.createLog("This is a test entry")
                    .owner(testOwner)
                    .withLogbook(testLogbook)
                    .withAttachment(testAttachment)
                    .build();

            Log createdLog = logRepository.index(log);

            String attachmentId = createdLog.getAttachments().iterator().next().getId();
            Resource a = logResource.findResources(createdLog.getId().toString(), attachmentId);

            File foundTestFile = new File("LogResourceIT_attachment_" + testAttachment.getId() + "_" + testAttachment.getFilename());
            Files.copy(a.getInputStream(), foundTestFile.toPath());
            FileAssert.assertBinaryEquals("failed to create log entry with attachment", testFile, foundTestFile);
            Files.delete(foundTestFile.toPath());
            gridOperation.delete(new Query(Criteria.where("_id").is(attachmentId)));

            // Manual cleanup since Olog does not delete things
            elasticsearchTemplate.getClient().prepareDelete(ES_LOG_INDEX, ES_LOG_TYPE, createdLog.getId().toString()).get("10s");
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally 
        {
            // Manual cleanup since Olog does not delete things
            elasticsearchTemplate.getClient().prepareDelete(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, testLogbook.getName()).get("10s");
        }
    }
}
