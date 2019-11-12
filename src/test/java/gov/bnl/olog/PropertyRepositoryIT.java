package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_PROPERTY_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_PROPERTY_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import gov.bnl.olog.entity.Attribute;
import gov.bnl.olog.entity.Property;
import gov.bnl.olog.entity.State;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ElasticConfig.class)
public class PropertyRepositoryIT
{

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;
    @Autowired
    private PropertyRepository propertyRepository;

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
     */
    @Test
    public void createProperty()
    {
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(new Attribute("test-attribute-1"));
        attributes.add(new Attribute("test-attribute-2"));
        Property testProperty = new Property("test-property-1", testOwner, State.Active, attributes);
        propertyRepository.index(testProperty);
        Optional<Property> result = propertyRepository.findById(testProperty.getName());
        assertThat("Failed to create Property " + testProperty,
                result.isPresent() && result.get().equals(testProperty));

        // Manual cleanup since Olog does not delete things
        elasticsearchTemplate.getClient().prepareDelete(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, testProperty.getName())
                .get("10s");
    }

    /**
     * Test the deletion of a test property
     */
    @Test
    public void deleteProperty()
    {
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(new Attribute("test-attribute-1"));
        attributes.add(new Attribute("test-attribute-2"));
        Property testProperty = new Property("test-property-2", testOwner, State.Active, attributes);
        propertyRepository.index(testProperty);
        Optional<Property> result = propertyRepository.findById(testProperty.getName());
        assertThat("Failed to create Property " + testProperty,
                result.isPresent() && result.get().equals(testProperty));

        propertyRepository.delete(testProperty);
        result = propertyRepository.findById(testProperty.getName());
        testProperty.setState(State.Inactive);
        assertThat("Failed to delete Property", result.isPresent() && result.get().equals(testProperty));

        // Manual cleanup since Olog does not delete things
        elasticsearchTemplate.getClient().prepareDelete(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, testProperty.getName())
                .get("10s");
    }

    /**
     * Test the deletion of a test property
     */
    @Test
    public void deletePropertyAttribute()
    {
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(new Attribute("test-attribute-1"));
        attributes.add(new Attribute("test-attribute-2"));
        Property testProperty = new Property("test-property-2", testOwner, State.Active, attributes);
        propertyRepository.index(testProperty);
        Optional<Property> result = propertyRepository.findById(testProperty.getName());
        assertThat("Failed to create Property " + testProperty,
                result.isPresent() && result.get().equals(testProperty));

        propertyRepository.deleteAttribute(testProperty.getName(), "test-attribute-1");
        result = propertyRepository.findById(testProperty.getName());
        testProperty.setAttributes(testProperty.getAttributes().stream().map(p -> {
            if (p.getName().equals("test-attribute-1"))
            {
                p.setState(State.Inactive);
            }
            return p;
        }).collect(Collectors.toSet()));
        assertThat("Failed to delete Property", result.isPresent() && result.get().equals(testProperty));

        // Manual cleanup since Olog does not delete things
        elasticsearchTemplate.getClient().prepareDelete(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, testProperty.getName())
                .get("10s");
    }

    /**
     * create a set of properties
     */
    @Test
    public void createProperties()
    {
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(new Attribute("test-attribute-1"));
        attributes.add(new Attribute("test-attribute-2"));
        Property testProperty1 = new Property("test-property-1", testOwner, State.Active, attributes);
        Property testProperty2 = new Property("test-property-2", testOwner, State.Active, attributes);
        Property testProperty3 = new Property("test-property-3", testOwner, State.Active, attributes);
        Property testProperty4 = new Property("test-property-4", testOwner, State.Active, attributes);
        List<Property> propertys = Arrays.asList(testProperty1, testProperty2, testProperty3, testProperty4);
        List<Property> result = new ArrayList<Property>();
        propertyRepository.saveAll(propertys).forEach(property -> {
            result.add(property);
        });
        assertThat("Failed to create all properties", result.containsAll(propertys));

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
        assertThat("Failed to list all properties", findAll.containsAll(propertys));

        // Manual cleanup since Olog does not delete things
        BulkRequestBuilder bulk = elasticsearchTemplate.getClient().prepareBulk();
        propertys.forEach(property -> {
            bulk.add(elasticsearchTemplate.getClient().prepareDelete(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE,
                    property.getName()));
        });
        bulk.get("10s");
    }

}
