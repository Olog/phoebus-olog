package edu.msu.nscl.olog;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class PropertyManagerIT {

    @Test
    public void findAllProperties() {
        List<Property> result = PropertyManager.list();
        assertTrue("Failed to list all Properties ", result.containsAll(ResourceManagerTestSuite.initialProperties));
    }

    @Test
    public void findAllActiveProperties() {
        List<Property> result = PropertyManager.listActive();
        assertTrue("Failed to list all active Properties ", result.containsAll(ResourceManagerTestSuite.initialProperties.stream()
                .filter(t -> t.getState().equals(State.Active)).collect(Collectors.toList())));
    }
}
