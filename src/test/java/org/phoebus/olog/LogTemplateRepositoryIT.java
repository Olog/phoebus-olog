package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.LogTemplate;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.State;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = AuthenticationResource.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@SuppressWarnings("unused")
class LogTemplateRepositoryIT {

    @Autowired
    private LogTemplateRepository logTemplateRepository;
    @Autowired
    private LogbookRepository logbookRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    // Read the elastic index and type from the application.properties
    @Value("${elasticsearch.tag.index:olog_tags}")
    private String ES_TAG_INDEX;
    @Value("${elasticsearch.tag.type:olog_tag}")
    private String ES_TAG_TYPE;
    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    private String ES_LOGBOOK_INDEX;
    @Value("${elasticsearch.logbook.type:olog_logbook}")
    private String ES_LOGBOOK_TYPE;
    @Value("${elasticsearch.property.index:olog_properties}")
    private String ES_PROPERTY_INDEX;
    @Value("${elasticsearch.property.type:olog_property}")
    private String ES_PROPERTY_TYPE;
    @Value("${elasticsearch.log.index:olog_logs}")
    private String ES_LOG_INDEX;
    @Value("${elasticsearch.log.archive.index:olog_archived_logs}")
    private String ES_LOG_ARCHIVE_INDEX;
    @Value("${elasticsearch.template.index:olog_templates}")
    public String ES_LOG_TEMPLATE_INDEX;

    @Value("${elasticsearch.log.type:olog_log}")
    private String ES_LOG_TYPE;

    private static final String TEST_OWNER = "test-owner";
    private static final Logbook TEST_LOGBOOK_1 = new Logbook("test-logbook-1", TEST_OWNER, State.Active);
    private static final Tag TEST_TAG_1 = new Tag("test-tag-1", State.Active);

    private static final Attribute ATTRIBUTE_1 = new Attribute("test-attribute-1");
    private static final Attribute ATTRIBUTE_2 = new Attribute("test-attribute-2");
    private static final Set<Attribute> ATTRIBUTES = new HashSet<>(List.of(ATTRIBUTE_1, ATTRIBUTE_2));
    private static final Property TEST_PROPERTY_1 = new Property("test-property-1", TEST_OWNER, State.Active, ATTRIBUTES);


    @Test
    void createAndUpdateTemplate() throws IOException {
        try {
            logbookRepository.save(TEST_LOGBOOK_1);
            tagRepository.save(TEST_TAG_1);
            propertyRepository.save(TEST_PROPERTY_1);

            LogTemplate logTemplate = new LogTemplate();
            logTemplate.setName("template1");
            logTemplate.setLogbooks(Set.of(TEST_LOGBOOK_1));
            logTemplate.setTags(Set.of(TEST_TAG_1));
            logTemplate.setOwner(TEST_OWNER);
            logTemplate.setProperties(Set.of(TEST_PROPERTY_1));

            LogTemplate savedTemplate = logTemplateRepository.save(logTemplate);
            assertNull(savedTemplate.getModifyDate());

            savedTemplate.setName("template2");
            LogTemplate updatedTemplate = logTemplateRepository.update(savedTemplate);
            assertNotNull(updatedTemplate.getModifyDate());
            assertEquals("template2", updatedTemplate.getName());

            client.delete(DeleteRequest.of(d -> d.index(ES_LOG_TEMPLATE_INDEX).id(savedTemplate.getId().toString()).refresh(Refresh.True)));
        } finally {
            client.delete(DeleteRequest.of(d -> d.index(ES_LOGBOOK_INDEX).id(TEST_LOGBOOK_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_TAG_INDEX).id(TEST_TAG_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_PROPERTY_INDEX).id(TEST_PROPERTY_1.getName()).refresh(Refresh.True)));
        }
    }


    @Test
    void findTemplateById() throws IOException {

        LogTemplate logTemplate = new LogTemplate();
        logTemplate.setName("template1");
        logTemplate.setOwner(TEST_OWNER);

        LogTemplate savedTemplate = logTemplateRepository.save(logTemplate);

        assertNotNull(logTemplateRepository.findById(String.valueOf(savedTemplate.getId())));

        client.delete(DeleteRequest.of(d -> d.index(ES_LOG_TEMPLATE_INDEX).id(savedTemplate.getId().toString()).refresh(Refresh.True)));
    }

    @Test
    void findLogsByNonExistingId() {
        assertThrows(ResponseStatusException.class, () -> logTemplateRepository.findById("666"));
    }

    @Test
    void deleteById() {
        LogTemplate logTemplate = new LogTemplate();
        logTemplate.setName("template1");
        logTemplate.setOwner(TEST_OWNER);

        LogTemplate savedTemplate = logTemplateRepository.save(logTemplate);

        logTemplateRepository.deleteById(String.valueOf(savedTemplate.getId()));

        assertThrows(ResponseStatusException.class, () -> logTemplateRepository.findById(String.valueOf(savedTemplate.getId())));

        // Delete non-existing template should succeed silently
        logTemplateRepository.deleteById("666");
    }

    @Test
    void findAll() {
        LogTemplate logTemplate = new LogTemplate();
        logTemplate.setName("template1");
        logTemplate.setOwner(TEST_OWNER);

        LogTemplate savedTemplate = logTemplateRepository.save(logTemplate);

        Iterable<LogTemplate> iterable = logTemplateRepository.findAll();

        Iterator<LogTemplate> iterator = iterable.iterator();
        AtomicBoolean found = new AtomicBoolean();
        iterator.forEachRemaining(t -> {
            if (t.getName().equals("template1") && t.getOwner().equals(TEST_OWNER)) {
                found.set(true);
            }
        });
        if (!found.get()) {
            fail("Did not find expected template");
        }
    }
}
