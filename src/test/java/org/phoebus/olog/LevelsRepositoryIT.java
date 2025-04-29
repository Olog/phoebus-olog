package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.olog.entity.Level;
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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = AuthenticationResource.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@SuppressWarnings("unused")
class LevelsRepositoryIT {

    @Autowired
    private LevelRepository levelRepository;

    private final Level level1 = new Level("level1", true);
    private final Level level2 = new Level("level2", false);
    private final Level level3 = new Level("level3", true);

    // Read the elastic index and type from the application.properties
    @Value("${elasticsearch.level.index:olog_levels}")
    private String ES_LEVEL_INDEX;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    @Test
    void createLevel() {
        Level l = levelRepository.save(level1);
        Optional<Level> result = levelRepository.findById(level1.name());
        assertThat("Failed to create Level " + level1.name(), result.isPresent() && result.get().equals(level1));

        cleanUp(List.of(level1));
    }

    @Test
    void deleteLevel()  {
        levelRepository.save(level2);
        Optional<Level> result = levelRepository.findById(level2.name());
        assertThat("Failed to create level " + level2.name(), result.isPresent() && result.get().equals(level2));

        levelRepository.delete(level2);
        result = levelRepository.findById(level2.name());
        assertThat("Failed to delete level ", result.isEmpty());

        cleanUp(List.of(level1));
    }

    @Test
    void deleteAll() {
        levelRepository.saveAll(List.of(level1, level2));
        levelRepository.deleteAll();

        Optional<Level> result =
                levelRepository.findById(level1.name());
        assertThat("Failed to delete level1 ", result.isEmpty());
        result =
                levelRepository.findById(level2.name());
        assertThat("Failed to delete level2 ", result.isEmpty());

    }

    @Test
    void deleteAllById() {
        levelRepository.saveAll(List.of(level1, level2));
        levelRepository.deleteAllById(List.of("level1", "level2"));

        Optional<Level> result =
                levelRepository.findById(level1.name());
        assertThat("Failed to delete level1 ", result.isEmpty());
        result =
                levelRepository.findById(level2.name());
        assertThat("Failed to delete level2 ", result.isEmpty());
    }

    /**
     * create a set of tags
     */
    @Test
    void createLevels() {
        List<Level> levels = Arrays.asList(level1, level2);
        try {
            List<Level> result = new ArrayList<>();
            levelRepository.saveAll(levels).forEach(result::add);
            assertThat("Failed to create multiple levels", result.containsAll(levels));

            List<Level> findAll = new ArrayList<>();
            levelRepository.findAll().forEach(findAll::add);
            assertThat("Failed to create multiple leevls ", findAll.containsAll(levels));
        } finally {
            // Manual cleanup
            cleanUp(levels);
        }
    }

    /**
     * delete a set of tags
     */
    @Test
    void deleteTags() {
        List<Level> tags = Arrays.asList(level1, level2);
        try {
            List<Level> result = new ArrayList<>();
            levelRepository.saveAll(tags).forEach(result::add);

            levelRepository.deleteAll(tags);

            Iterable<Level> levels = levelRepository.findAll();
            Iterator<Level> iterator = levels.iterator();
            while(iterator.hasNext()){
                Level level = iterator.next();
                if(level.name().equals(level1.name()) || level.name().equals(level2.name())){
                    fail("Found level that should be deleted");
                }
            }
        } finally {
            // Manual cleanup
            cleanUp(tags);
        }
    }

    @Test
    void findAllLevels() {
        List<Level> levels = Arrays.asList(level1, level2);
        try {
            levelRepository.saveAll(levels);
            List<Level> findAll = new ArrayList<>();
            levelRepository.findAll().forEach(findAll::add);
            assertThat("Failed to list all levels", findAll.containsAll(levels));
        } finally {
            // Manual cleanup
            cleanUp(levels);
        }
    }

    @Test
    void findAllLevelsByIds() {
        List<Level> levels = Arrays.asList(level1, level2);
        try {
            levelRepository.saveAll(levels);

            List<Level> findAllById = new ArrayList<>();
            levelRepository.findAllById(Arrays.asList("level1", "level2"))
                    .forEach(findAllById::add);
            assertTrue(
                    findAllById.size() == 2 && findAllById.contains(level1) && findAllById.contains(level2),
                    "Failed to search by id level1 and level2 ");
        } finally {
            // Manual cleanup
            cleanUp(levels);
        }
    }

    @Test
    void findLevelById() {
        List<Level> levels = Arrays.asList(level1, level2);
        try {
            levelRepository.saveAll(levels);
            assertEquals(level1, levelRepository.findById(level1.name()).get(), "Failed to find by index level: " + level1.name());
            assertEquals(level2, levelRepository.findById(level2.name()).get(), "Failed to find by index level: " + level2.name());
        } finally {
            // Manual cleanup
            cleanUp(levels);
        }
    }

    @Test
    void checkLevelExists() {
        List<Level> levels = Arrays.asList(level1, level2);
        try {
            levelRepository.saveAll(levels);

            assertTrue(levelRepository.existsById(level1.name()),
                    "Failed to check if exists level: " + level1.name());
            assertTrue(levelRepository.existsById(level2.name()),
                    "Failed to check if exists level: " + level2.name());

            assertFalse(levelRepository.existsById("non-existant-tag"),
                    "Failed to check if exists level: non-existant-tag");
        } finally {
            // Manual cleanup
            cleanUp(levels);
        }
    }

    /**
     * Cleanup the given levels
     *
     * @param levels
     */

    private void cleanUp(List<Level> levels) {
        List<BulkOperation> bulkOperations = new ArrayList<>();
        levels.forEach(level -> bulkOperations.add(DeleteOperation.of(i ->
                i.index(ES_LEVEL_INDEX).id(level.name()))._toBulkOperation()));
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
