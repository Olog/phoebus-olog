package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = AuthenticationResource.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
public class PropertyRepositoryIT {

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;


    @Autowired
    private PropertyRepository propertyRepository;


    Set<Attribute> attributes = Stream.of(
                    new Attribute("test-attribute-1"),
                    new Attribute("test-attribute-2"))
            .collect(Collectors.toSet());

    Property testProperty1 = new Property("test-property-1", testOwner, State.Active, attributes);
    Property testProperty2 = new Property("test-property-2", testOwner, State.Active, attributes);
    Property testProperty3 = new Property("test-property-3", testOwner, State.Active, attributes);
    Property testProperty4 = new Property("test-property-4", testOwner, State.Active, attributes);

    @Value("${elasticsearch.property.index:olog_properties}")
    private String ES_PROPERTY_INDEX;

    private static final String testOwner = "test-owner";

    /**
     * Test the creation of a test property
     */
    @Test
    public void createProperty() {
        propertyRepository.save(testProperty1);
        Optional<Property> result = propertyRepository.findById(testProperty1.getName());
        assertThat("Failed to create Property " + testProperty1,
                result.isPresent() && result.get().equals(testProperty1));

        // Manual cleanup since Olog does not delete things
        cleanupProperties(List.of(testProperty1));
    }

    /**
     * create a set of properties
     */
    @Test
    public void createProperties() {
        List<Property> properties = Arrays.asList(testProperty1, testProperty2, testProperty3, testProperty4);
        List<Property> result = new ArrayList<>();
        propertyRepository.saveAll(properties).forEach(result::add);
        assertThat("Failed to create all properties", result.containsAll(properties));
        List<Property> findAll = new ArrayList<>();
        propertyRepository.findAll().forEach(findAll::add);
        assertThat("Failed to list all properties", findAll.containsAll(properties));

        // Manual cleanup since Olog does not delete things
        cleanupProperties(properties);
    }

    /**
     * Test the deletion of a test property
     */
    @Test
    public void deleteProperty() {
        propertyRepository.save(testProperty2);
        Optional<Property> result = propertyRepository.findById(testProperty2.getName());
        assertThat("Failed to create Property " + testProperty2,
                result.isPresent() && result.get().equals(testProperty2));

        propertyRepository.delete(testProperty2);
        result = propertyRepository.findById(testProperty2.getName());
        testProperty2.setState(State.Inactive);
        assertThat("Failed to delete Property", result.isPresent() && result.get().equals(testProperty2));

        // Manual cleanup since Olog does not delete things
        cleanupProperties(List.of(testProperty2));
    }

    /**
     * Test the deletion of a test property
     */
    @Test
    public void deletePropertyAttribute() {
        propertyRepository.save(testProperty2);
        Optional<Property> result = propertyRepository.findById(testProperty2.getName());
        assertThat("Failed to create Property " + testProperty2,
                result.isPresent() && result.get().equals(testProperty2));

        propertyRepository.deleteAttribute(testProperty2.getName(), "test-attribute-1");
        result = propertyRepository.findById(testProperty2.getName());
        testProperty2.setAttributes(testProperty2.getAttributes().stream().map(p -> {
            if (p.getName().equals("test-attribute-1")) {
                p.setState(State.Inactive);
            }
            return p;
        }).collect(Collectors.toSet()));
        assertThat("Failed to delete Property", result.isPresent() && result.get().equals(testProperty2));

        // Manual cleanup since Olog does not delete things
        cleanupProperties(List.of(testProperty2));
    }

    /**
     * delete a set of properties
     */
    @Test
    public void deleteproperties() {
        List<Property> properties = Arrays.asList(testProperty1, testProperty2, testProperty3, testProperty4);
        try {
            List<Property> result = new ArrayList<>();
            propertyRepository.saveAll(properties).forEach(result::add);

            propertyRepository.deleteAll(properties);
            List<Property> inactiveproperties = new ArrayList<>();
            propertyRepository.findAllById(properties.stream().map(Property::getName).collect(Collectors.toList())).forEach(Property -> {
                if (Property.getState().equals(State.Inactive)) {
                    inactiveproperties.add(Property);
                }
            });
            assertThat("Failed to delete multiple properties ", inactiveproperties.containsAll(properties));
        } finally {
            // Manual cleanup
            cleanupProperties(properties);
        }
    }

    @Test
    public void findAllproperties() {
        List<Property> properties = Arrays.asList(testProperty1, testProperty2, testProperty3, testProperty4);
        try {
            propertyRepository.saveAll(properties);
            List<Property> findAll = new ArrayList<>();
            propertyRepository.findAll().forEach(findAll::add);
            assertThat("Failed to list all properties", findAll.containsAll(properties));
        } finally {
            // Manual cleanup
            cleanupProperties(properties);
        }
    }

    @Test
    public void findAllpropertiesByIds() {
        List<Property> properties = Arrays.asList(testProperty1, testProperty2, testProperty3, testProperty4);
        try {
            propertyRepository.saveAll(properties);

            List<Property> findAllById = new ArrayList<>();
            propertyRepository.findAllById(Arrays.asList("test-property-1", "test-property-2"))
                    .forEach(findAllById::add);
            assertTrue(
                    findAllById.size() == 2 && findAllById.contains(testProperty1) && findAllById.contains(testProperty2),
                    "Failed to search by id test-property-1 and test-property-2 ");
        } finally {
            // Manual cleanup
            cleanupProperties(properties);
        }
    }

    @Test
    public void findPropertyById() {
        List<Property> properties = Arrays.asList(testProperty1, testProperty2);
        try {
            propertyRepository.saveAll(properties);
            assertEquals(testProperty1, propertyRepository.findById(testProperty1.getName()).get(), "Failed to find by index Property: " + testProperty1);
            assertEquals(testProperty2, propertyRepository.findById(testProperty2.getName()).get(), "Failed to find by index Property: " + testProperty2);
        } finally {
            // Manual cleanup
            cleanupProperties(properties);
        }
    }

    @Test
    public void checkPropertyExists() {
        List<Property> properties = Arrays.asList(testProperty1, testProperty2);
        try {
            propertyRepository.saveAll(properties);

            assertTrue(propertyRepository.existsById(testProperty1.getName()),
                    "Failed to check if exists Property: " + testProperty1);
            assertTrue(propertyRepository.existsById(testProperty2.getName()),
                    "Failed to check if exists Property: " + testProperty2);

            assertFalse(propertyRepository.existsById("non-existant-Property"),
                    "Failed to check if exists Property: non-existant-Property");
        } finally {
            // Manual cleanup
            cleanupProperties(properties);
        }
    }


    /**
     * Clean up the properties
     *
     * @param properties
     */

    private void cleanupProperties(List<Property> properties) {
        List<BulkOperation> bulkOperations = new ArrayList<>();
        properties.forEach(property -> bulkOperations.add(DeleteOperation.of(i ->
                i.index(ES_PROPERTY_INDEX).id(property.getName()))._toBulkOperation()));
        BulkRequest bulkRequest =
                BulkRequest.of(r ->
                        r.operations(bulkOperations).refresh(Refresh.True));
        try {
            client.bulk(bulkRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
