package edu.msu.nscl.olog;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

public class PropertyManagerIT {

    /**
     * Test listing all active and inactive properties
     */
    @Test
    public void findAllProperties() {
        List<Property> result = PropertyManager.list();
        assertTrue("Failed to list all Properties ", result.containsAll(ResourceManagerTestSuite.initialProperties));
    }

    /**
     * List only the active properties
     */
    @Test
    public void findAllActiveProperties() {
        List<Property> result = PropertyManager.listActive();
        assertTrue("Failed to list all active Properties ",
                result.containsAll(ResourceManagerTestSuite.initialProperties.stream()
                        .filter(t -> t.getState().equals(State.Active)).collect(Collectors.toList())));
    }

    /**
     * Create a new property
     */
    @Test
    public void createProperty() {
        Set<Attribute> attributes = new HashSet<>(Arrays.asList(new Attribute("test-attribute1", "val1", State.Active),
                new Attribute("test-attribute2", "1234", State.Active)));
        Property property = new Property("create-property-test1", State.Active, attributes);

        Optional<Property> createdProperty = PropertyManager.create(property);
        assertTrue("Failed to create the test property " + Property.toLogger(property), createdProperty.isPresent());
        assertTrue("Failed to create the test property " + Property.toLogger(property),
                property.equals(createdProperty.get()));

    }

    /**
     * Delete a property
     */
    @Test
    public void deleteProperty() {
        Set<Attribute> attributes = new HashSet<>(Arrays.asList(new Attribute("test-attribute1", "val1", State.Active),
                new Attribute("test-attribute2", "1234", State.Active)));
        Property property = new Property("delete-property-test1", State.Active, attributes);
        Optional<Property> createdProperty = PropertyManager.create(property);
        Optional<Property> deletedProperty = PropertyManager.delete(property);

        assertTrue("Failed to properly delete tag 'delete-test-logbook1' ", !PropertyManager.listActive().contains(property));
        assertTrue("Failed to properly delete tag 'delete-test-logbook1' ", PropertyManager.list().contains(property));

    }
}
