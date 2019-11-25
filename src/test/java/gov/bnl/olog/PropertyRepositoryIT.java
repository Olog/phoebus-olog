package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_PROPERTY_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_PROPERTY_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import gov.bnl.olog.entity.Attribute;
import gov.bnl.olog.entity.Property;
import gov.bnl.olog.entity.State;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations="classpath:test_application.properties")
public class PropertyRepositoryIT
{

    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient client;

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

    private static final String testOwner = "test-owner";

    /**
     * Test the creation of a test property
     * @throws IOException 
     */
    @Test
    public void createProperty() throws IOException
    {
        propertyRepository.save(testProperty1);
        Optional<Property> result = propertyRepository.findById(testProperty1.getName());
        assertThat("Failed to create Property " + testProperty1,
                result.isPresent() && result.get().equals(testProperty1));

        // Manual cleanup since Olog does not delete things
        cleanupProperties(Arrays.asList(testProperty1));
    }

    /**
     * create a set of properties
     * @throws IOException 
     */
    @Test
    public void createProperties() throws IOException
    {
        List<Property> properties = Arrays.asList(testProperty1, testProperty2, testProperty3, testProperty4);
        List<Property> result = new ArrayList<Property>();
        propertyRepository.saveAll(properties).forEach(property -> {
            result.add(property);
        });
        assertThat("Failed to create all properties", result.containsAll(properties));
        List<Property> findAll = new ArrayList<Property>();
        propertyRepository.findAll().forEach(property -> {
            findAll.add(property);
        });
        assertThat("Failed to list all properties", findAll.containsAll(properties));

        // Manual cleanup since Olog does not delete things
        cleanupProperties(properties);
    }

    /**
     * Test the deletion of a test property
     * @throws IOException 
     */
    @Test
    public void deleteProperty() throws IOException
    {
        propertyRepository.save(testProperty2);
        Optional<Property> result = propertyRepository.findById(testProperty2.getName());
        assertThat("Failed to create Property " + testProperty2,
                result.isPresent() && result.get().equals(testProperty2));

        propertyRepository.delete(testProperty2);
        result = propertyRepository.findById(testProperty2.getName());
        testProperty2.setState(State.Inactive);
        assertThat("Failed to delete Property", result.isPresent() && result.get().equals(testProperty2));

        // Manual cleanup since Olog does not delete things
        cleanupProperties(Arrays.asList(testProperty2));
    }
    
    /**
     * Test the deletion of a test property
     * @throws IOException 
     */
    @Test
    public void deletePropertyAttribute() throws IOException
    {
        propertyRepository.save(testProperty2);
        Optional<Property> result = propertyRepository.findById(testProperty2.getName());
        assertThat("Failed to create Property " + testProperty2,
                result.isPresent() && result.get().equals(testProperty2));

        propertyRepository.deleteAttribute(testProperty2.getName(), "test-attribute-1");
        result = propertyRepository.findById(testProperty2.getName());
        testProperty2.setAttributes(testProperty2.getAttributes().stream().map(p -> {
            if (p.getName().equals("test-attribute-1"))
            {
                p.setState(State.Inactive);
            }
            return p;
        }).collect(Collectors.toSet()));
        assertThat("Failed to delete Property", result.isPresent() && result.get().equals(testProperty2));

        // Manual cleanup since Olog does not delete things
        cleanupProperties(Arrays.asList(testProperty2));
    }

    /**
     * delete a set of properties
     * 
     * @throws IOException
     */
    @Test
    public void deleteproperties() throws IOException {
        List<Property> properties = Arrays.asList(testProperty1, testProperty2, testProperty3, testProperty4);
        try
        {
            List<Property> result = new ArrayList<Property>();
            propertyRepository.saveAll(properties).forEach(Property -> {
                result.add(Property);
            });

            propertyRepository.deleteAll(properties);
            List<Property> inactiveproperties = new ArrayList<Property>();
            propertyRepository.findAllById(properties.stream().map(Property::getName).collect(Collectors.toList())).forEach(Property -> {
                if (Property.getState().equals(State.Inactive))
                {
                    inactiveproperties.add(Property);
                }
            });
            assertThat("Failed to delete multiple properties ", inactiveproperties.containsAll(properties));
        } finally
        {
            // Manual cleanup
            cleanupProperties(properties);
        }
    }

    @Test
    public void findAllproperties() throws IOException
    {
        List<Property> properties = Arrays.asList(testProperty1, testProperty2, testProperty3, testProperty4);
        try
        {
            propertyRepository.saveAll(properties);
            List<Property> findAll = new ArrayList<Property>();
            propertyRepository.findAll().forEach(Property -> {
                findAll.add(Property);
            });
            assertThat("Failed to list all properties", findAll.containsAll(properties));
        } finally
        {
            // Manual cleanup
            cleanupProperties(properties);
        }
    }

    @Test
    public void findAllpropertiesByIds() throws IOException
    {
        List<Property> properties = Arrays.asList(testProperty1, testProperty2, testProperty3, testProperty4);
        try
        {
            propertyRepository.saveAll(properties);

            List<Property> findAllById = new ArrayList<Property>();
            propertyRepository.findAllById(Arrays.asList("test-property-1", "test-property-2"))
                                        .forEach(Property -> {
                                            findAllById.add(Property);
                                        });
            assertTrue("Failed to search by id test-property-1 and test-property-2 ",
                    findAllById.size() == 2 && findAllById.contains(testProperty1) && findAllById.contains(testProperty2));
        } finally
        {
            // Manual cleanup
            cleanupProperties(properties);
        }
    }
    @Test
    public void findPropertyById() throws IOException
    {
        List<Property> properties = Arrays.asList(testProperty1, testProperty2);
        try
        {
            propertyRepository.saveAll(properties);
            assertTrue("Failed to find by index Property: " + testProperty1,
                    testProperty1.equals(propertyRepository.findById(testProperty1.getName()).get()));
            assertTrue("Failed to find by index Property: " + testProperty2,
                    testProperty2.equals(propertyRepository.findById(testProperty2.getName()).get()));
        } finally
        {
            // Manual cleanup
            cleanupProperties(properties);
        }
    }

    @Test
    public void checkPropertyExists() throws IOException {
        List<Property> properties = Arrays.asList(testProperty1, testProperty2);
        try
        {
            propertyRepository.saveAll(properties);

            assertTrue("Failed to check if exists Property: " + testProperty1, propertyRepository.existsById(testProperty1.getName()));
            assertTrue("Failed to check if exists Property: " + testProperty2, propertyRepository.existsById(testProperty2.getName()));

            assertFalse("Failed to check if exists Property: non-existant-Property", propertyRepository.existsById("non-existant-Property"));
        } finally
        {
            // Manual cleanup
            cleanupProperties(properties);
        }
    }


    /**
     * Clean up the properties
     * @param properties
     */
    private void cleanupProperties(List<Property> properties)
    {
        try
        {
            BulkRequest bulk = new BulkRequest();
            properties.forEach(property -> {
                bulk.add(new DeleteRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, property.getName()));
            });
            client.bulk(bulk, RequestOptions.DEFAULT);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
