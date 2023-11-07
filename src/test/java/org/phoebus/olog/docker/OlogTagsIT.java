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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.entity.State;
import org.phoebus.olog.entity.Tag;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
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
public class OlogTagsIT {

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

    static final String TAGS = "/tags";

    static final String HTTP_IP_PORT_OLOG_TAGS            = ITUtil.HTTP +                           ITUtil.IP_PORT_OLOG + TAGS;
    static final String HTTP_AUTH_USER_IP_PORT_OLOG_TAGS  = ITUtil.HTTP + ITUtil.AUTH_USER  + "@" + ITUtil.IP_PORT_OLOG + TAGS;
    static final String HTTP_AUTH_ADMIN_IP_PORT_OLOG_TAGS = ITUtil.HTTP + ITUtil.AUTH_ADMIN + "@" + ITUtil.IP_PORT_OLOG + TAGS;

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

    @Test
    public void ologUp() {
        try {
            String address = ITUtil.HTTP_IP_PORT_OLOG;
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    public void handleTagRetrieveCheck() {
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

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t11");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_NOT_FOUND);
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    public void handleTagRemoveCheck() {
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

        try {
            // might be both 401, 404
            //     401 UNAUTHORIZED
            //     404 NOT_FOUND
            String[] response = ITUtil.runShellCommand(deleteCurlTagForUser("t11"));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_NOT_FOUND);

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t11"));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_NOT_FOUND);
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    public void handleTagCreateCheckJson() {
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

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", json_incomplete1));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", json_incomplete2));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", json_incomplete3));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", json_incomplete4));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", json_incomplete5));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", json_incomplete7));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", json_incomplete8));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", json_incomplete9));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", json_tag_t1_name_na));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", json_tag_t1_state_empty));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", json_tag_t1_state_asdf));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    public void handleTagCreateCheck() {
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

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);

            // response = ITUtil.runShellCommand(createCurlTagForUser("t1", mapper.writeValueAsString(tag_t1_state_a)));
            // ITUtil.assertResponseLength2Code(HttpURLConnection.HTTP_UNAUTHORIZED, response);

            response = ITUtil.runShellCommand(createCurlTagForUser("asdf", mapper.writeValueAsString(tag_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("asdf", mapper.writeValueAsString(tag_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            tag_check.setName(null);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("asdf", mapper.writeValueAsString(tag_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            tag_check.setName("");

            response = ITUtil.runShellCommand(createCurlTagForAdmin("asdf", mapper.writeValueAsString(tag_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    public void handleTag() {
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

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", mapper.writeValueAsString(tag_t1_state_a)));
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t1_state_a, mapper.readValue(response[1], Tag.class));

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0],
                    tag_t1_state_a);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t1_state_a, mapper.readValue(response[1], Tag.class));

            // response = ITUtil.runShellCommand(deleteCurlTagForUser("t1"));
            // ITUtil.assertResponseLength2Code(HttpURLConnection.HTTP_UNAUTHORIZED, response);

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t1"));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t1_state_i, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    public void handleTag2() {
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

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", mapper.writeValueAsString(tag_t1_state_a)));
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t1_state_a, mapper.readValue(response[1], Tag.class));

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t2", mapper.writeValueAsString(tag_t2_state_a)));
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t2_state_a, mapper.readValue(response[1], Tag.class));

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0],
                    tag_t1_state_a,
                    tag_t2_state_a);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t1_state_a, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t2");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t2_state_a, mapper.readValue(response[1], Tag.class));

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t1"));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0],
                    tag_t2_state_a);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t1_state_i, mapper.readValue(response[1], Tag.class));

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t2"));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t2");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t2_state_i, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    public void handleTag3ChangeState() {
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

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", mapper.writeValueAsString(tag_t1_state_a)));
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t1_state_a, mapper.readValue(response[1], Tag.class));

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0],
                    tag_t1_state_a);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t1_state_a, mapper.readValue(response[1], Tag.class));

            response = ITUtil.runShellCommand(createCurlTagForAdmin("t1", mapper.writeValueAsString(tag_t1_state_i)));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t1_state_i, mapper.readValue(response[1], Tag.class));

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t1"));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t1_state_i, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    public void handleTagsCreateCheck() {
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

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);

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

            response = ITUtil.runShellCommand(createCurlTagsForAdmin(mapper.writeValueAsString(tags)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            tag_check.setName(null);
            tags[10] = tag_check;

            response = ITUtil.runShellCommand(createCurlTagsForAdmin(mapper.writeValueAsString(tags)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            tag_check.setName("");
            tags[10] = tag_check;

            response = ITUtil.runShellCommand(createCurlTagsForAdmin(mapper.writeValueAsString(tags)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#TAG_RESOURCE_URI}.
     */
    @Test
    public void handleTags() {
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

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0]);

            response = ITUtil.runShellCommand(createCurlTagsForAdmin(mapper.writeValueAsString(tags_active_inactive)));
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    tags_active_inactive);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0],
                    tag_t1_state_a,
                    tag_t2_state_a,
                    tag_t3_state_a,
                    tag_t4_state_a,
                    tag_t5_state_a);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t1_state_a, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t2");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t2_state_a, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t3");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t3_state_a, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t4");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t4_state_a, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t5");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t5_state_a, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t6");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t6_state_i, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t7");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t7_state_i, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t8");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t8_state_i, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t9");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t9_state_i, mapper.readValue(response[1], Tag.class));

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS + "/t10");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(tag_t10_state_i, mapper.readValue(response[1], Tag.class));

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t1"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t2"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t3"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t9"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t10"));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsTags(
                    mapper.readValue(response[1], Tag[].class),
                    default_tags[0],
                    tag_t4_state_a,
                    tag_t5_state_a);

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t4"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t5"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t6"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t7"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlTagForAdmin("t8"));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_TAGS);
            ITUtil.assertResponseLength2CodeOK(response);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Utility method to return curl to create tag for regular user.
     *
     * @param tagName tag name
     * @param tagJson tag json
     * @return curl to create tag
     */
    private static String createCurlTagForUser(String tagName, String tagJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + HTTP_AUTH_USER_IP_PORT_OLOG_TAGS + "/" + tagName + " -d '" + tagJson + "'";
    }

    /**
     * Utility method to return curl to create tag for admin user.
     *
     * @param tagName tag name
     * @param tagJson tag json
     * @return curl to create tag
     */
    private static String createCurlTagForAdmin(String tagName, String tagJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + HTTP_AUTH_ADMIN_IP_PORT_OLOG_TAGS + "/" + tagName + " -d '" + tagJson + "'";
    }

    /**
     * Utility method to return curl to create tags for admin user.
     *
     * @param tagsJson tags json
     * @return curl to create tags
     */
    private static String createCurlTagsForAdmin(String tagsJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + HTTP_AUTH_ADMIN_IP_PORT_OLOG_TAGS + " -d '" + tagsJson + "'";
    }

    /**
     * Utility method to return curl to delete tag for regular user.
     *
     * @param tagName tag name
     * @return curl to delete tag
     */
    private static String deleteCurlTagForUser(String tagName) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XDELETE -i " + HTTP_AUTH_USER_IP_PORT_OLOG_TAGS + "/" + tagName;
    }

    /**
     * Utility method to return curl to delete tag for admin user.
     *
     * @param tagName tag name
     * @return curl to delete tag
     */
    private static String deleteCurlTagForAdmin(String tagName) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XDELETE -i " + HTTP_AUTH_ADMIN_IP_PORT_OLOG_TAGS + "/" + tagName;
    }

}
