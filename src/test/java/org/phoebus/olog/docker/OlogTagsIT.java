/*
 * Copyright (C) 2021 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.olog.docker;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.docker.ITUtil.AuthorizationChoice;
import org.phoebus.olog.entity.State;
import org.phoebus.olog.entity.Tag;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests for Olog and Elasticsearch that make use of existing dockerization
 * with docker-compose.yml / Dockerfile.
 *
 * <p>
 * Focus of this class is to have Olog and Elasticsearch up and running together with usage of
 * {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.olog.TagsResource
 */
@Testcontainers
class OlogTagsIT {

    // Note
    //     ------------------------------------------------------------------------------------------------
    //     About
    //         requires
    //             elastic indices for Olog, ensured at start-up
    //             environment
    //                 default ports, 8080 for Olog, 9200 for Elasticsearch
    //                 demo_auth enabled
    //         json
    //             check(s) for json and objects written as json
    //             objects representing data/entities sent/received - serialized/deserialized
    //         docker containers shared for tests
    //             each test to leave Olog, Elasticsearch in clean state - not disturb other tests
    //             clean state may be content with status inactive
    //         each test uses multiple endpoints in Olog API
    //     ------------------------------------------------------------------------------------------------
    //     Olog - Service Documentation
    //         https://olog.readthedocs.io/en/latest/
    //     ------------------------------------------------------------------------------------------------
    //     OLOG API                                     TagResource
    //     --------------------                         --------------------
    //     Retrieve a Tag        .../tags/<name>        (GET)           findByTitle(String)
    //     List Tags             .../tags               (GET)           findAll()
    //     Create a Tag          .../tags/<name>        (PUT)           createTag(String, Tag)
    //     Create Tags           .../tags               (PUT)           updateTag(List<Tag>)
    //     Remove Tag            .../tags/<name>        (DELETE)        deleteTag(String)
    //     ------------------------------------------------------------------------------------------------

    // test data
    //     tags t1 - t10, state Active - Inactive
    //     tags t1 - t2,  state Inactive

    static Tag[] default_tags;

    static Tag tag_t1_state_a;
    static Tag tag_t2_state_a;
    static Tag tag_t3_state_a;
    static Tag tag_t4_state_a;
    static Tag tag_t5_state_a;
    static Tag tag_t6_state_i;
    static Tag tag_t7_state_i;
    static Tag tag_t8_state_i;
    static Tag tag_t9_state_i;
    static Tag tag_t10_state_i;

    static Tag tag_t1_state_i;
    static Tag tag_t2_state_i;

    @Container
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @BeforeAll
    public static void setupObjects() {
        default_tags = new Tag[] {new Tag("alarm", State.Active)};

        tag_t1_state_a = new Tag("t1", State.Active);
        tag_t2_state_a = new Tag("t2", State.Active);
        tag_t3_state_a = new Tag("t3", State.Active);
        tag_t4_state_a = new Tag("t4", State.Active);
        tag_t5_state_a = new Tag("t5", State.Active);
        tag_t6_state_i = new Tag("t6", State.Inactive);
        tag_t7_state_i = new Tag("t7", State.Inactive);
        tag_t8_state_i = new Tag("t8", State.Inactive);
        tag_t9_state_i = new Tag("t9", State.Inactive);
        tag_t10_state_i = new Tag("t10", State.Inactive);

        tag_t1_state_i = new Tag("t1", State.Inactive);
        tag_t2_state_i = new Tag("t2", State.Inactive);
    }

    @AfterAll
    public static void tearDownObjects() {
        default_tags = null;

        tag_t1_state_a = null;
        tag_t2_state_a = null;
        tag_t3_state_a = null;
        tag_t4_state_a = null;
        tag_t5_state_a = null;
        tag_t6_state_i = null;
        tag_t7_state_i = null;
        tag_t8_state_i = null;
        tag_t9_state_i = null;
        tag_t10_state_i = null;

        tag_t1_state_i = null;
        tag_t2_state_i = null;
    }

    @AfterAll
    public static void extractJacocoReport() {
        // extract jacoco report from container file system
        ITUtil.extractJacocoReport(ENVIRONMENT,
                ITUtil.JACOCO_TARGET_PREFIX + OlogTagsIT.class.getSimpleName() + ITUtil.JACOCO_TARGET_SUFFIX);
    }

    @Test
    void ologUp() {
        try {
            int responseCode = ITUtil.sendRequestStatusCode(ITUtil.HTTP_IP_PORT_OLOG);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    void handleTagRetrieveCheck() {
        // what
        //     check(s) for retrieve tag
        //         e.g.
        //             retrieve non-existing tag
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Tag
        //         List Tags
        //         Create a Tag
        //         Create Tags
        //         Remove Tag

        ITUtilTags.assertRetrieveTag("/t11", HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    void handleTagRemoveCheck() {
        // what
        //     check(s) for remove tag
        //         e.g.
        //             remove non-existing tag
        //     --------------------------------------------------------------------------------
        //         Retrieve a Tag
        //         List Tags
        //         Create a Tag
        //         Create Tags
        //     x   Remove Tag

        // might be both 401, 404
        //     401 UNAUTHORIZED
        //     404 NOT_FOUND

        // check permissions

        ITUtilTags.assertRemoveTag(AuthorizationChoice.USER,  "/t11", HttpURLConnection.HTTP_NOT_FOUND);
        ITUtilTags.assertRemoveTag(AuthorizationChoice.ADMIN, "/t11", HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    void handleTagCreateCheckJson() {
        // what
        //     check(s) for create tag
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 state      - null, (empty, incorrect value (ok: Active, Inactive))
        //     --------------------------------------------------------------------------------
        //         Retrieve a Tag
        //     x   List Tags
        //     x   Create a Tag
        //         Create Tags
        //         Remove Tag

        String json_incomplete1 = "{\"incomplete\"}";
        String json_incomplete2 = "{\"incomplete\"";
        String json_incomplete3 = "{\"incomplete}";
        String json_incomplete4 = "{\"\"}";
        String json_incomplete5 = "{incomplete\"}";
        String json_incomplete7 = "{";
        String json_incomplete8 = "}";
        String json_incomplete9 = "\"";

        String json_tag_t1_name_na     = "{\"na\":\"t1\",\"state\":\"Active\"}";
        String json_tag_t1_state_empty = "{\"name\":\"t1\",\"state\":\"\"}";
        String json_tag_t1_state_asdf  = "{\"name\":\"t1\",\"state\":\"asdf\"}";

        ITUtilTags.assertListTags(1, default_tags[0]);

        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/t1", json_incomplete1, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/t1", json_incomplete2, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/t1", json_incomplete3, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/t1", json_incomplete4, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/t1", json_incomplete5, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/t1", json_incomplete7, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/t1", json_incomplete8, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/t1", json_incomplete9, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/t1", json_tag_t1_name_na,     HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/t1", json_tag_t1_state_empty, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/t1", json_tag_t1_state_asdf,  HttpURLConnection.HTTP_BAD_REQUEST);

        ITUtilTags.assertListTags(1, default_tags[0]);
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    void handleTagCreateCheck() {
        // what
        //     check(s) for create tag
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 state      - null, (empty, incorrect value (ok: Active, Inactive))
        //     --------------------------------------------------------------------------------
        //         Retrieve a Tag
        //     x   List Tags
        //     x   Create a Tag
        //         Create Tags
        //         Remove Tag

        Tag tag_check = new Tag();

        ITUtilTags.assertListTags(1, default_tags[0]);

        // check permissions
        // ITUtilTags.assertCreateTag(AuthorizationChoice.USER,  "/t1", tag_t1_state_a, HttpURLConnection.HTTP_UNAUTHORIZED);

        ITUtilTags.assertCreateTag(AuthorizationChoice.USER,  "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);

        tag_check.setName(null);

        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);

        tag_check.setName("");

        ITUtilTags.assertCreateTag(AuthorizationChoice.ADMIN, "/asdf", tag_check, HttpURLConnection.HTTP_BAD_REQUEST);

        ITUtilTags.assertListTags(1, default_tags[0]);
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    void handleTag() {
        // what
        //     user with required role TagMod
        //     create tag
        //     --------------------------------------------------------------------------------
        //     list, create, list/retrieve, remove (unauthorized), remove, retrieve/list
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Tag
        //     x   List Tags
        //     x   Create a Tag
        //         Create Tags
        //     x   Remove Tag

        try {
            ITUtilTags.assertListTags(1, default_tags[0]);

            ITUtilTags.assertCreateTag("/t1", tag_t1_state_a);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilTags.assertListTags(2,
                    default_tags[0],
                    tag_t1_state_a);

            ITUtilTags.assertRetrieveTag("/t1", tag_t1_state_a);

            // check permissions
            // ITUtilTags.assertRemoveTag(AuthorizationChoice.USER, "/t1", HttpURLConnection.HTTP_UNAUTHORIZED);

            ITUtilTags.assertRemoveTag("/t1");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilTags.assertRetrieveTag("/t1", tag_t1_state_i);

            ITUtilTags.assertListTags(1, default_tags[0]);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    void handleTag2() {
        // what
        //     create tags, one by one
        //     --------------------------------------------------------------------------------
        //     list, create (2), list/retrieve, remove, list/retrieve, remove, retrieve/list
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Tag
        //     x   List Tags
        //     x   Create a Tag
        //         Create Tags
        //     x   Remove Tag

        try {
            ITUtilTags.assertListTags(1, default_tags[0]);

            ITUtilTags.assertCreateTag("/t1", tag_t1_state_a);
            ITUtilTags.assertCreateTag("/t2", tag_t2_state_a);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilTags.assertListTags(3,
                    default_tags[0],
                    tag_t1_state_a,
                    tag_t2_state_a);

            ITUtilTags.assertRetrieveTag("/t1", tag_t1_state_a);
            ITUtilTags.assertRetrieveTag("/t2", tag_t2_state_a);

            ITUtilTags.assertRemoveTag("/t1");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilTags.assertListTags(2,
                    default_tags[0],
                    tag_t2_state_a);

            ITUtilTags.assertRetrieveTag("/t1", tag_t1_state_i);

            ITUtilTags.assertRemoveTag("/t2");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilTags.assertRetrieveTag("/t2", tag_t2_state_i);

            ITUtilTags.assertListTags(1, default_tags[0]);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    void handleTag3ChangeState() {
        // what
        //     replace tag, change state
        //     --------------------------------------------------------------------------------
        //     list, create, list/retrieve, update, list/retrieve, remove, retrieve/list
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Tag
        //     x   List Tags
        //     x   Create a Tag
        //         Create Tags
        //     x   Remove Tag

        try {
            ITUtilTags.assertListTags(1, default_tags[0]);

            ITUtilTags.assertCreateTag("/t1", tag_t1_state_a);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilTags.assertListTags(2,
                    default_tags[0],
                    tag_t1_state_a);

            ITUtilTags.assertRetrieveTag("/t1", tag_t1_state_a);

            ITUtilTags.assertCreateTag("/t1", tag_t1_state_i);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilTags.assertListTags(1, default_tags[0]);

            ITUtilTags.assertRetrieveTag("/t1", tag_t1_state_i);

            ITUtilTags.assertRemoveTag("/t1");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilTags.assertRetrieveTag("/t1", tag_t1_state_i);

            ITUtilTags.assertListTags(1, default_tags[0]);
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    void handleTagsCreateCheck() {
        // what
        //     check(s) for create tags
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 state      - null, (empty, incorrect value (ok: Active, Inactive))
        //     --------------------------------------------------------------------------------
        //         Retrieve a Tag
        //     x   List Tags
        //         Create a Tag
        //     x   Create Tags
        //         Remove Tag

        Tag tag_check = new Tag();
        Tag[] tags = new Tag[] {
                tag_t1_state_a,
                tag_t2_state_a,
                tag_t3_state_a,
                tag_t4_state_a,
                tag_t5_state_a,
                tag_t6_state_i,
                tag_t7_state_i,
                tag_t8_state_i,
                tag_t9_state_i,
                tag_t10_state_i,
                tag_check
        };

        ITUtilTags.assertListTags(1, default_tags[0]);

        ITUtilTags.assertCreateTags("", tags, HttpURLConnection.HTTP_BAD_REQUEST);

        tag_check.setName(null);
        tags[10] = tag_check;

        ITUtilTags.assertCreateTags("", tags, HttpURLConnection.HTTP_BAD_REQUEST);

        tag_check.setName("");
        tags[10] = tag_check;

        ITUtilTags.assertCreateTags("", tags, HttpURLConnection.HTTP_BAD_REQUEST);

        ITUtilTags.assertListTags(1, default_tags[0]);
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    void handleTags() {
        // what
        //     create tags
        //     --------------------------------------------------------------------------------
        //     list, create (10), list/retrieve, delete (5), list/retrieve, delete (5), retrieve/list
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Tag
        //     x   List Tags
        //         Create a Tag
        //     x   Create Tags
        //     x   Remove Tag

        Tag[] tags_active_inactive = new Tag[] {
                tag_t1_state_a,
                tag_t2_state_a,
                tag_t3_state_a,
                tag_t4_state_a,
                tag_t5_state_a,
                tag_t6_state_i,
                tag_t7_state_i,
                tag_t8_state_i,
                tag_t9_state_i,
                tag_t10_state_i
        };

        try {
            ITUtilTags.assertListTags(1, default_tags[0]);

            ITUtilTags.assertCreateTags("", tags_active_inactive);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilTags.assertListTags(6,
                    default_tags[0],
                    tag_t1_state_a,
                    tag_t2_state_a,
                    tag_t3_state_a,
                    tag_t4_state_a,
                    tag_t5_state_a);

            ITUtilTags.assertRetrieveTag("/t1", tag_t1_state_a);
            ITUtilTags.assertRetrieveTag("/t2", tag_t2_state_a);
            ITUtilTags.assertRetrieveTag("/t3", tag_t3_state_a);
            ITUtilTags.assertRetrieveTag("/t4", tag_t4_state_a);
            ITUtilTags.assertRetrieveTag("/t5", tag_t5_state_a);
            ITUtilTags.assertRetrieveTag("/t6", tag_t6_state_i);
            ITUtilTags.assertRetrieveTag("/t7", tag_t7_state_i);
            ITUtilTags.assertRetrieveTag("/t8", tag_t8_state_i);
            ITUtilTags.assertRetrieveTag("/t9", tag_t9_state_i);
            ITUtilTags.assertRetrieveTag("/t10", tag_t10_state_i);

            ITUtilTags.assertRemoveTag("/t1");
            ITUtilTags.assertRemoveTag("/t2");
            ITUtilTags.assertRemoveTag("/t3");
            ITUtilTags.assertRemoveTag("/t9");
            ITUtilTags.assertRemoveTag("/t10");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilTags.assertListTags(3,
                    default_tags[0],
                    tag_t4_state_a,
                    tag_t5_state_a);

            ITUtilTags.assertRemoveTag("/t4");
            ITUtilTags.assertRemoveTag("/t5");
            ITUtilTags.assertRemoveTag("/t6");
            ITUtilTags.assertRemoveTag("/t7");
            ITUtilTags.assertRemoveTag("/t8");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilTags.assertListTags(1, default_tags[0]);
        } catch (Exception e) {
            fail();
        }
    }

}
