package org.phoebus.olog;

import com.mongodb.client.gridfs.model.GridFSFile;
import junitx.framework.FileAssert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.olog.entity.Attachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.function.Consumer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ElasticConfig.class)
@SuppressWarnings("unused")
@TestPropertySource(locations = "classpath:test_application.properties")
public class AttachmentRepositoryIT {
    @Autowired
    private GridFsTemplate gridFsTemplate;
    @Autowired
    private GridFsOperations gridOperation;

    @Autowired
    private LogRepository logRepository;
    @Autowired
    private AttachmentRepository attachmentRepository;

    @BeforeClass
    public static void setup() {

    }

    @AfterClass
    public static void cleanup() {
    }

    /**
     * Test the creation of a image attachment
     */
    @Test
    public void createImageAttachment() {
        try {
            File testFile = new File("src/test/resources/Tulips.jpg");
            MockMultipartFile mock;
            mock = new MockMultipartFile(testFile.getName(), new FileInputStream(testFile));
            Attachment testAttachment = new Attachment(mock, "Tulips.jpg", "");

            Attachment createdAttachment = attachmentRepository.save(testAttachment);
            // Directly retrieve the attached file to verify if it was recorded correctly
            /*
            gridOperation.find(new Query(Criteria.where("_id").is(createdAttachment.getId()))).forEach(new Consumer<GridFSFile>() {

                @Override
                public void accept(GridFSFile t) {
                    try {
                        File createdFile = new File("test_attachment_" + createdAttachment.getId() + "_" + createdAttachment.getFilename());
                        InputStream st = gridOperation.getResource(t).getInputStream();
                        Files.copy(st, createdFile.toPath());
                        FileAssert.assertBinaryEquals("failed to create log entry with attachment", testFile, createdFile);
                        Files.delete(createdFile.toPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        gridOperation.delete(new Query(Criteria.where("_id").is(createdAttachment.getId())));
                    }
                }
            });

             */
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Test the creation of a simple text attachment
     */
    @Test
    public void createTextAttachment() {
        try {
            File testFile = new File("src/test/resources/SampleTextFile_100kb.txt");
            MockMultipartFile mock;
            mock = new MockMultipartFile(testFile.getName(), new FileInputStream(testFile));
            Attachment testAttachment = new Attachment(mock, "SampleTextFile_100kb.txt", "");

            Attachment createdAttachment = attachmentRepository.save(testAttachment);

            // Directly retrieve the attached file to verify if it was recorded correctly
            /*
            gridOperation.find(new Query(Criteria.where("_id").is(createdAttachment.getId()))).forEach(new Consumer<GridFSFile>() {

                @Override
                public void accept(GridFSFile t) {
                    try {
                        File createdFile = new File("test_attachment_" + createdAttachment.getId() + "_" + createdAttachment.getFilename());
                        InputStream st = gridOperation.getResource(t).getInputStream();
                        Files.copy(st, createdFile.toPath());
                        FileAssert.assertBinaryEquals("failed to create log entry with attachment", testFile, createdFile);
                        Files.delete(createdFile.toPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        gridOperation.delete(new Query(Criteria.where("_id").is(createdAttachment.getId())));
                    }
                }
            });

             */
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test the retrieval of an image attachment
     */
    @Test
    public void retrieveImageAttachment() {
        try {
            File testFile = new File("src/test/resources/SampleTextFile_100kb.txt");
            MockMultipartFile mock;
            mock = new MockMultipartFile(testFile.getName(), new FileInputStream(testFile));
            Attachment testAttachment = new Attachment(mock, "SampleTextFile_100kb.txt", "");

            Attachment createdAttachment = attachmentRepository.save(testAttachment);
            // Retrieve the attached file using the attachment repository
            Attachment foundAttachment = attachmentRepository.findById(createdAttachment.getId()).get();
            //assertEquals(createdAttachment, foundAttachment);

            File foundTestFile = new File("test_attachment_" + createdAttachment.getId() + "_" + createdAttachment.getFilename());
            Files.copy(foundAttachment.getAttachment().getInputStream(), foundTestFile.toPath());
            FileAssert.assertBinaryEquals("failed to create log entry with attachment", testFile, foundTestFile);
            Files.delete(foundTestFile.toPath());

            gridOperation.delete(new Query(Criteria.where("_id").is(createdAttachment.getId())));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
