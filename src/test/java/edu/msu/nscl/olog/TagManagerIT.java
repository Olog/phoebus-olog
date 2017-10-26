package edu.msu.nscl.olog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

public class TagManagerIT {

    @Test
    public void findAllTag() {
        List<Tag> result = TagManager.list();
        assertTrue("Failed to list all tags ", result.containsAll(ResourceManagerTestSuite.initialTags));
    }
    

    @Test
    public void findAllActiveTag() {
        List<Tag> result = TagManager.listActive();
        assertTrue("Failed to list all active tags ", 
                result.containsAll(
                        ResourceManagerTestSuite.initialTags
                        .stream()
                        .filter(t -> t.getState().equals(State.Active))
                        .collect(Collectors.toList())));
    }

    /**
     * A basic test to create a simple tag
     */
    @Test
    public void createTag() {
        Tag tag = new Tag("create-test-tag1", State.Active);
        Optional<Tag> result = TagManager.createTag(tag);
        if (result.isPresent()) {
            assertEquals("Failed to created a new tag ", tag, result.get());
            assertNotNull("Failed to create a valid tag - no id detected", result.get().getId());
        } else {
            fail("Failed to create a valid tag");
        }
    }
//
//    /**
//     * A basic test to delete a simple tag
//     */
//    @Test
//    public void deleteTag() {
//        Tag tag = new Tag("delete-test-tag1", State.Active);
//        Optional<Tag> result = TagManager.deleteTag(tag);
//        if (result.isPresent()) {
//            assertEquals("Failed to created a new tag ", tag, result.get());
//            assertNotNull("Failed to create a valid tag - no id detected", result.get().getId());
//        } else {
//            fail("Failed to create a valid tag");
//        }
//    }

}
