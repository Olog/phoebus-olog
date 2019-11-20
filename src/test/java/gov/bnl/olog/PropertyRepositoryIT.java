package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_PROPERTY_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_PROPERTY_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
    
    @BeforeClass
    public static void setup()
    {
        
    }

    @AfterClass
    public static void cleanup()
    {

    }

    private static final String testOwner = "test-owner";

    /**
     * Test the creation of a test property
     * @throws IOException 
     */
    @Test
    public void createProperty() throws IOException
    {
        propertyRepository.index(testProperty1);
        Optional<Property> result = propertyRepository.findById(testProperty1.getName());
        assertThat("Failed to create Property " + testProperty1,
                result.isPresent() && result.get().equals(testProperty1));

        // Manual cleanup since Olog does not delete things
        cleanupProperties(Arrays.asList(testProperty1));
    }

    /**
     * Test the deletion of a test property
     * @throws IOException 
     */
    @Test
    public void deleteProperty() throws IOException
    {
        propertyRepository.index(testProperty2);
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
        propertyRepository.index(testProperty2);
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
     * create a set of properties
     * @throws IOException 
     */
    @Test
    public void createProperties() throws IOException
    {
        Property testProperty1 = new Property("test-property-1", testOwner, State.Active, attributes);
        Property testProperty2 = new Property("test-property-2", testOwner, State.Active, attributes);
        Property testProperty3 = new Property("test-property-3", testOwner, State.Active, attributes);
        Property testProperty4 = new Property("test-property-4", testOwner, State.Active, attributes);
        List<Property> properties = Arrays.asList(testProperty1, testProperty2, testProperty3, testProperty4);
        List<Property> result = new ArrayList<Property>();
        propertyRepository.saveAll(properties).forEach(property -> {
            result.add(property);
        });
        assertThat("Failed to create all properties", result.containsAll(properties));

        try
        {
            Thread.sleep(10000);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        List<Property> findAll = new ArrayList<Property>();
        propertyRepository.findAll().forEach(property -> {
            findAll.add(property);
        });
        assertThat("Failed to list all properties", findAll.containsAll(properties));

        // Manual cleanup since Olog does not delete things
        cleanupProperties(properties);
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
