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
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.State;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests for Olog and Elasticsearch that make use of existing dockerization
 * with docker-compose.yml / Dockerfile.
 *
 * <p>
 * Focus of this class is to have Olog and Elasticsearch up and running together with usage of
 * {@link org.phoebus.olog.OlogResourceDescriptors#PROPERTY_RESOURCE_URI}.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.olog.PropertiesResource
 */
@Testcontainers
class OlogPropertiesIT {

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
    //     OLOG API                                                PropertiesResource
    //     --------------------                                    --------------------
    //     Retrieve a Property        .../properties/<name>        (GET)           findByTitle(String)
    //     List Properties            .../properties               (GET)           findAll(boolean)
    //     Create a Property          .../properties/<name>        (PUT)           createProperty(String, Property, Principal)
    //     Create Properties          .../properties               (PUT)           updateProperty(List<Property>)
    //     Remove Property            .../properties/<name>        (DELETE)        deleteProperty(String)
    //     ------------------------------------------------------------------------------------------------

    // test data
    //     properties p1 - p10, owner admin, state Active - Inactive
    //     properties p1 - p2,  owner admin, state Inactive

    static Property[] default_properties;

    static Attribute a1;
    static Attribute a2;
    static Attribute a3;
    static Attribute a4;
    static Attribute a5;

    static Property property_p1_owner_a_state_a_attributes;
    static Property property_p2_owner_a_state_a_attributes;
    static Property property_p3_owner_a_state_a_attributes;
    static Property property_p4_owner_a_state_a_attributes;
    static Property property_p5_owner_a_state_a_attributes;

    static Property property_p1_owner_a_state_i_attributes;
    static Property property_p2_owner_a_state_i_attributes;
    static Property property_p3_owner_a_state_i_attributes;
    static Property property_p4_owner_a_state_i_attributes;
    static Property property_p5_owner_a_state_i_attributes;
    static Property property_p6_owner_a_state_i_attributes;
    static Property property_p7_owner_a_state_i_attributes;
    static Property property_p8_owner_a_state_i_attributes;
    static Property property_p9_owner_a_state_i_attributes;
    static Property property_p10_owner_a_state_i_attributes;

    @Container
    public static final ComposeContainer ENVIRONMENT = ITUtil.defaultComposeContainers();

    @BeforeAll
    public static void setupObjects() {
        default_properties = new Property[] {new Property("resource", null, State.Active, new HashSet<Attribute>())};
        default_properties[0].addAttributes(new Attribute("name", null, State.Active));
        default_properties[0].addAttributes(new Attribute("file", null, State.Active));

        a1 = new Attribute("a1", "v1", State.Active);
        a2 = new Attribute("a2", "v2", State.Active);
        a3 = new Attribute("a3", "v3", State.Active);
        a4 = new Attribute("a4", "v4", State.Active);
        a5 = new Attribute("a5", "v5", State.Active);

        property_p1_owner_a_state_a_attributes = new Property("p1", "admin", State.Active, new HashSet<Attribute>());
        property_p1_owner_a_state_a_attributes.addAttributes(a1);
        property_p2_owner_a_state_a_attributes = new Property("p2", "admin", State.Active, new HashSet<Attribute>());
        property_p2_owner_a_state_a_attributes.addAttributes(a1);
        property_p2_owner_a_state_a_attributes.addAttributes(a2);
        property_p3_owner_a_state_a_attributes = new Property("p3", "admin", State.Active, new HashSet<Attribute>());
        property_p3_owner_a_state_a_attributes.addAttributes(a1);
        property_p3_owner_a_state_a_attributes.addAttributes(a2);
        property_p3_owner_a_state_a_attributes.addAttributes(a3);
        property_p4_owner_a_state_a_attributes = new Property("p4", "admin", State.Active, new HashSet<Attribute>());
        property_p4_owner_a_state_a_attributes.addAttributes(a1);
        property_p4_owner_a_state_a_attributes.addAttributes(a2);
        property_p4_owner_a_state_a_attributes.addAttributes(a3);
        property_p4_owner_a_state_a_attributes.addAttributes(a4);
        property_p5_owner_a_state_a_attributes = new Property("p5", "admin", State.Active, new HashSet<Attribute>());
        property_p5_owner_a_state_a_attributes.addAttributes(a1);
        property_p5_owner_a_state_a_attributes.addAttributes(a2);
        property_p5_owner_a_state_a_attributes.addAttributes(a3);
        property_p5_owner_a_state_a_attributes.addAttributes(a4);
        property_p5_owner_a_state_a_attributes.addAttributes(a5);

        property_p1_owner_a_state_i_attributes = new Property("p1", "admin", State.Inactive, new HashSet<Attribute>());
        property_p1_owner_a_state_i_attributes.addAttributes(a1);
        property_p2_owner_a_state_i_attributes = new Property("p2", "admin", State.Inactive, new HashSet<Attribute>());
        property_p2_owner_a_state_i_attributes.addAttributes(a1);
        property_p2_owner_a_state_i_attributes.addAttributes(a2);
        property_p3_owner_a_state_i_attributes = new Property("p3", "admin", State.Active, new HashSet<Attribute>());
        property_p3_owner_a_state_i_attributes.addAttributes(a1);
        property_p3_owner_a_state_i_attributes.addAttributes(a2);
        property_p3_owner_a_state_i_attributes.addAttributes(a3);
        property_p4_owner_a_state_i_attributes = new Property("p4", "admin", State.Active, new HashSet<Attribute>());
        property_p4_owner_a_state_i_attributes.addAttributes(a1);
        property_p4_owner_a_state_i_attributes.addAttributes(a2);
        property_p4_owner_a_state_i_attributes.addAttributes(a3);
        property_p4_owner_a_state_i_attributes.addAttributes(a4);
        property_p5_owner_a_state_i_attributes = new Property("p5", "admin", State.Active, new HashSet<Attribute>());
        property_p5_owner_a_state_i_attributes.addAttributes(a1);
        property_p5_owner_a_state_i_attributes.addAttributes(a2);
        property_p5_owner_a_state_i_attributes.addAttributes(a3);
        property_p5_owner_a_state_i_attributes.addAttributes(a4);
        property_p5_owner_a_state_i_attributes.addAttributes(a5);
        property_p6_owner_a_state_i_attributes = new Property("p6", "admin", State.Inactive, new HashSet<Attribute>());
        property_p6_owner_a_state_i_attributes.addAttributes(a1);
        property_p7_owner_a_state_i_attributes = new Property("p7", "admin", State.Inactive, new HashSet<Attribute>());
        property_p7_owner_a_state_i_attributes.addAttributes(a1);
        property_p7_owner_a_state_i_attributes.addAttributes(a2);
        property_p8_owner_a_state_i_attributes = new Property("p8", "admin", State.Inactive, new HashSet<Attribute>());
        property_p8_owner_a_state_i_attributes.addAttributes(a1);
        property_p8_owner_a_state_i_attributes.addAttributes(a2);
        property_p8_owner_a_state_i_attributes.addAttributes(a3);
        property_p9_owner_a_state_i_attributes = new Property("p9", "admin", State.Inactive, new HashSet<Attribute>());
        property_p9_owner_a_state_i_attributes.addAttributes(a1);
        property_p9_owner_a_state_i_attributes.addAttributes(a2);
        property_p9_owner_a_state_i_attributes.addAttributes(a3);
        property_p9_owner_a_state_i_attributes.addAttributes(a4);
        property_p10_owner_a_state_i_attributes = new Property("p10", "admin", State.Inactive, new HashSet<Attribute>());
        property_p10_owner_a_state_i_attributes.addAttributes(a1);
        property_p10_owner_a_state_i_attributes.addAttributes(a2);
        property_p10_owner_a_state_i_attributes.addAttributes(a3);
        property_p10_owner_a_state_i_attributes.addAttributes(a4);
        property_p10_owner_a_state_i_attributes.addAttributes(a5);
    }

    @AfterAll
    public static void tearDownObjects() {
        default_properties = null;

        property_p1_owner_a_state_a_attributes = null;
        property_p2_owner_a_state_a_attributes = null;
        property_p3_owner_a_state_a_attributes = null;
        property_p4_owner_a_state_a_attributes = null;
        property_p5_owner_a_state_a_attributes = null;

        property_p1_owner_a_state_i_attributes = null;
        property_p2_owner_a_state_i_attributes = null;
        property_p3_owner_a_state_i_attributes = null;
        property_p4_owner_a_state_i_attributes = null;
        property_p5_owner_a_state_i_attributes = null;
        property_p6_owner_a_state_i_attributes = null;
        property_p7_owner_a_state_i_attributes = null;
        property_p8_owner_a_state_i_attributes = null;
        property_p9_owner_a_state_i_attributes = null;
        property_p10_owner_a_state_i_attributes = null;

        a1 = null;
        a2 = null;
        a3 = null;
        a4 = null;
        a5 = null;
    }

    @Test
    void ologUp() {
        try {
            String address = ITUtil.HTTP_IP_PORT_OLOG;
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handlePropertyRetrieveCheck() {
        // what
        //     check(s) for retrieve property
        //         e.g.
        //             retrieve non-existing property
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Property
        //         List Properties
        //         Create a Property
        //         Create Properties
        //         Remove Property

        try {
            String[] response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p11");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_NOT_FOUND);
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handlePropertyRemoveCheck() {
        // what
        //     check(s) for remove property
        //         e.g.
        //             remove non-existing property
        //     --------------------------------------------------------------------------------
        //         Retrieve a Property
        //         List Properties
        //         Create a Property
        //         Create Properties
        //     x   Remove Property

        try {
            // might be both 401, 404
            //     401 UNAUTHORIZED
            //     404 NOT_FOUND
            String[] response = ITUtil.runShellCommand(deleteCurlPropertyForUser("p11"));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_NOT_FOUND);

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p11"));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_NOT_FOUND);
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handlePropertyCreateCheckJson() {
        // what
        //     check(s) for create property
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 state      - null (empty, incorrect value (ok: Active, Inactive))
        //                 attributes - null
        //                     name       - null, empty
        //                     state      - null (empty, incorrect value (ok: Active, Inactive))
        //     --------------------------------------------------------------------------------
        //         Retrieve a Property
        //     x   List Properties
        //     x   Create a Property
        //         Create Properties
        //         Remove Property

        String json_incomplete1 = "{\"incomplete\"}";
        String json_incomplete2 = "{\"incomplete\"";
        String json_incomplete3 = "{\"incomplete}";
        String json_incomplete4 = "{\"\"}";
        String json_incomplete5 = "{incomplete\"}";
        String json_incomplete7 = "{";
        String json_incomplete8 = "}";
        String json_incomplete9 = "\"";

        String json_property_p1_name_na     = "{\"na\":\"p1\",\"owner\":\"admin\",\"state\":\"Active\",\"attributes\":[]}";

        String json_property_p1_attribute_0 = "{\"name\":\"p1\",\"owner\":\"admin\",\"state\":\"Active\",\"attributes\":[\"name\":\"a1\",\"value\":\"v1\",\"state\":\"Active\"]}";

        String json_property_p1_attribute_2_state_hyperactive = "{\"name\":\"p1\",\"owner\":\"admin\",\"state\":\"Active\",\"attributes\":[{\"name\":\"a1\",\"value\":\"v1\",\"state\":\"Active\"},{\"name\":\"a1\",\"value\":\"v1\",\"state\":\"Hyperactive\"}]}";

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", json_incomplete1));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", json_incomplete2));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", json_incomplete3));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", json_incomplete4));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", json_incomplete5));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", json_incomplete7));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", json_incomplete8));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", json_incomplete9));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", json_property_p1_name_na));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", json_property_p1_attribute_0));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", json_property_p1_attribute_2_state_hyperactive));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handlePropertyCreateCheck() {
        // what
        //     check(s) for create property
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 state      - null (empty, incorrect value (ok: Active, Inactive))
        //                 attributes - null
        //                     name       - null, empty
        //                     state      - null (empty, incorrect value (ok: Active, Inactive))
        //     --------------------------------------------------------------------------------
        //         Retrieve a Property
        //     x   List Properties
        //     x   Create a Property
        //         Create Properties
        //         Remove Property

        Property property_check = new Property();
        Attribute attribute_check = new Attribute();

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);

            // response = ITUtil.runShellCommand(createCurlPropertyForUser("p1", mapper.writeValueAsString(property_p1_owner_a_state_a_attributes)));
            // ITUtil.assertResponseLength2Code(HttpURLConnection.HTTP_UNAUTHORIZED, response);

            response = ITUtil.runShellCommand(createCurlPropertyForUser("asdf", mapper.writeValueAsString(property_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("asdf", mapper.writeValueAsString(property_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            property_check.setName(null);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("asdf", mapper.writeValueAsString(property_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            property_check.setName("");

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("asdf", mapper.writeValueAsString(property_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            property_check.setName("asdf");
            property_check.setOwner("zxcv");
            property_check.setState(State.Active);
            property_check.setAttributes(null);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("asdf", mapper.writeValueAsString(property_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_INTERNAL_ERROR);

            property_check = new Property();
            property_check.setName("asdf");
            property_check.setOwner("zxcv");
            property_check.setState(State.Active);
            property_check.addAttributes(attribute_check);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("asdf", mapper.writeValueAsString(property_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            attribute_check.setName(null);
            property_check.getAttributes().clear();
            property_check.addAttributes(attribute_check);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("asdf", mapper.writeValueAsString(property_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            attribute_check.setName("");
            property_check.getAttributes().clear();
            property_check.addAttributes(attribute_check);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("asdf", mapper.writeValueAsString(property_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperty() {
        // what
        //     user with required role
        //     create property
        //     --------------------------------------------------------------------------------
        //     list, create, list/retrieve, remove (unauthorized), remove, retrieve/list
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Property
        //     x   List Properties
        //     x   Create a Property
        //         Create Properties
        //     x   Remove Property

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", mapper.writeValueAsString(property_p1_owner_a_state_a_attributes)));
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(property_p1_owner_a_state_a_attributes.equals(mapper.readValue(response[1], Property.class)));

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p1_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=false");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p1_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=true");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p1_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(property_p1_owner_a_state_a_attributes.equals(mapper.readValue(response[1], Property.class)));

            // response = ITUtil.runShellCommand(deleteCurlPropertyForUser("p1"));
            // ITUtil.assertResponseLength2Code(HttpURLConnection.HTTP_UNAUTHORIZED, response);

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p1"));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertTrue(property_p1_owner_a_state_i_attributes.equals(mapper.readValue(response[1], Property.class)));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=false");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=true");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p1_owner_a_state_i_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperty2() {
        // what
        //     create properties, one by one
        //     --------------------------------------------------------------------------------
        //     list, create (2), list/retrieve, remove, list/retrieve, remove, retrieve/list
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Property
        //     x   List Properties
        //     x   Create a Property
        //         Create Properties
        //     x   Remove Property

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", mapper.writeValueAsString(property_p1_owner_a_state_a_attributes)));
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p1_owner_a_state_a_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p2", mapper.writeValueAsString(property_p2_owner_a_state_a_attributes)));
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p2_owner_a_state_a_attributes, mapper.readValue(response[1], Property.class));

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p1_owner_a_state_a_attributes,
                    property_p2_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=false");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p1_owner_a_state_a_attributes,
                    property_p2_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=true");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p1_owner_a_state_a_attributes,
                    property_p2_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p1_owner_a_state_a_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p2");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p2_owner_a_state_a_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p1"));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p2_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=false");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p2_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=true");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p1_owner_a_state_i_attributes,
                    property_p2_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p1_owner_a_state_i_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p2"));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p2");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p2_owner_a_state_i_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=false");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=true");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p1_owner_a_state_i_attributes,
                    property_p2_owner_a_state_i_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperty3ChangeState() {
        // what
        //     replace property, change state
        //     --------------------------------------------------------------------------------
        //     list, create, list/retrieve, update, list/retrieve, remove, retrieve/list
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Property
        //     x   List Properties
        //     x   Create a Property
        //         Create Properties
        //     x   Remove Property

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", mapper.writeValueAsString(property_p1_owner_a_state_a_attributes)));
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p1_owner_a_state_a_attributes, mapper.readValue(response[1], Property.class));

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p1_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p1_owner_a_state_a_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.runShellCommand(createCurlPropertyForAdmin("p1", mapper.writeValueAsString(property_p1_owner_a_state_i_attributes)));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p1_owner_a_state_i_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p1"));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p1_owner_a_state_i_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handlePropertiesCreateCheck() {
        // what
        //     check(s) for create properties
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 state      - null (empty, incorrect value (ok: Active, Inactive))
        //                 attributes - null
        //                     name       - null, empty
        //                     state      - null (empty, incorrect value (ok: Active, Inactive))
        //     --------------------------------------------------------------------------------
        //         Retrieve a Property
        //     x   List Properties
        //         Create a Property
        //     x   Create Properties
        //         Remove Property

        Property property_check = new Property();
        Attribute attribute_check = new Attribute();

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);

            Property[] properties = new Property[] {
                    property_p1_owner_a_state_a_attributes,
                    property_p2_owner_a_state_a_attributes,
                    property_p3_owner_a_state_a_attributes,
                    property_p4_owner_a_state_a_attributes,
                    property_p5_owner_a_state_a_attributes,
                    property_p6_owner_a_state_i_attributes,
                    property_p7_owner_a_state_i_attributes,
                    property_p8_owner_a_state_i_attributes,
                    property_p9_owner_a_state_i_attributes,
                    property_p10_owner_a_state_i_attributes,
                    property_check
            };

            response = ITUtil.runShellCommand(createCurlPropertiesForAdmin(mapper.writeValueAsString(properties)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            property_check.setName(null);
            properties[10] = property_check;

            response = ITUtil.runShellCommand(createCurlPropertiesForAdmin(mapper.writeValueAsString(properties)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            property_check.setName("");
            properties[10] = property_check;

            response = ITUtil.runShellCommand(createCurlPropertiesForAdmin(mapper.writeValueAsString(properties)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            property_check.setName("asdf");
            property_check.setOwner("zxcv");
            property_check.setState(State.Active);
            property_check.setAttributes(null);
            properties[10] = property_check;

            response = ITUtil.runShellCommand(createCurlPropertiesForAdmin(mapper.writeValueAsString(properties)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_INTERNAL_ERROR);

            property_check = new Property();
            property_check.setName("asdf");
            property_check.setOwner("zxcv");
            property_check.setState(State.Active);
            property_check.addAttributes(attribute_check);
            properties[10] = property_check;

            response = ITUtil.runShellCommand(createCurlPropertiesForAdmin(mapper.writeValueAsString(properties)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            attribute_check.setName(null);
            property_check.getAttributes().clear();
            property_check.addAttributes(attribute_check);
            properties[10] = property_check;

            response = ITUtil.runShellCommand(createCurlPropertiesForAdmin(mapper.writeValueAsString(properties)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            attribute_check.setName("");
            property_check.getAttributes().clear();
            property_check.addAttributes(attribute_check);
            properties[10] = property_check;

            response = ITUtil.runShellCommand(createCurlPropertiesForAdmin(mapper.writeValueAsString(properties)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#PROPERTY_RESOURCE_URI}.
     */
    @Test
    void handleProperties() {
        // what
        //     create properties
        //     --------------------------------------------------------------------------------
        //     list, create (10), list/retrieve, delete (5), list/retrieve, delete (5), retrieve/list
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Property
        //     x   List Properties
        //         Create a Property
        //     x   Create Properties
        //     x   Remove Property

        Property[] properties_active_inactive = new Property[] {
                property_p1_owner_a_state_a_attributes,
                property_p10_owner_a_state_i_attributes,
                property_p2_owner_a_state_a_attributes,
                property_p3_owner_a_state_a_attributes,
                property_p4_owner_a_state_a_attributes,
                property_p5_owner_a_state_a_attributes,
                property_p6_owner_a_state_i_attributes,
                property_p7_owner_a_state_i_attributes,
                property_p8_owner_a_state_i_attributes,
                property_p9_owner_a_state_i_attributes
        };

        Property[] properties_inactive = new Property[] {
                property_p1_owner_a_state_i_attributes,
                property_p10_owner_a_state_i_attributes,
                property_p2_owner_a_state_i_attributes,
                property_p3_owner_a_state_i_attributes,
                property_p4_owner_a_state_i_attributes,
                property_p5_owner_a_state_i_attributes,
                property_p6_owner_a_state_i_attributes,
                property_p7_owner_a_state_i_attributes,
                property_p8_owner_a_state_i_attributes,
                property_p9_owner_a_state_i_attributes
        };

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);

            response = ITUtil.runShellCommand(createCurlPropertiesForAdmin(mapper.writeValueAsString(properties_active_inactive)));
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    properties_active_inactive);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p1_owner_a_state_a_attributes,
                    property_p2_owner_a_state_a_attributes,
                    property_p3_owner_a_state_a_attributes,
                    property_p4_owner_a_state_a_attributes,
                    property_p5_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=false");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p1_owner_a_state_a_attributes,
                    property_p2_owner_a_state_a_attributes,
                    property_p3_owner_a_state_a_attributes,
                    property_p4_owner_a_state_a_attributes,
                    property_p5_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=true");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    properties_active_inactive);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p1");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p1_owner_a_state_a_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p2");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p2_owner_a_state_a_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p3");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p3_owner_a_state_a_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p4");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p4_owner_a_state_a_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p5");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p5_owner_a_state_a_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p6");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p6_owner_a_state_i_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p7");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p7_owner_a_state_i_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p8");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p8_owner_a_state_i_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p9");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p9_owner_a_state_i_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "/p10");
            ITUtil.assertResponseLength2CodeOK(response);
            assertEquals(property_p10_owner_a_state_i_attributes, mapper.readValue(response[1], Property.class));

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p1"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p2"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p3"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p9"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p10"));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p4_owner_a_state_a_attributes,
                    property_p5_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=false");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    property_p4_owner_a_state_a_attributes,
                    property_p5_owner_a_state_a_attributes,
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=true");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    properties_active_inactive);

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p4"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p5"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p6"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p7"));
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.runShellCommand(deleteCurlPropertyForAdmin("p8"));
            ITUtil.assertResponseLength2CodeOK(response);

            // refresh elastic indices
            response = ITUtil.refreshElasticIndices();
            ITUtil.assertResponseLength2CodeOK(response);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=false");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + "?inactive=true");
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    properties_inactive);

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES);
            ITUtil.assertResponseLength2CodeOK(response);
            ITUtil.assertEqualsProperties(
                    mapper.readValue(response[1], Property[].class),
                    default_properties[0]);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Utility method to return curl to create property for regular user.
     *
     * @param propertyName property name
     * @param propertyJson property json
     * @return curl to create property
     */
    private static String createCurlPropertyForUser(String propertyName, String propertyJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_USER_IP_PORT_OLOG_PROPERTIES + "/" + propertyName + " -d '" + propertyJson + "'";
    }

    /**
     * Utility method to return curl to create property for admin user.
     *
     * @param propertyName property name
     * @param propertyJson property json
     * @return curl to create property
     */
    private static String createCurlPropertyForAdmin(String propertyName, String propertyJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_PROPERTIES + "/" + propertyName + " -d '" + propertyJson + "'";
    }

    /**
     * Utility method to return curl to create properties for admin user.
     *
     * @param propertiesJson properties json
     * @return curl to create properties
     */
    private static String createCurlPropertiesForAdmin(String propertiesJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_PROPERTIES + " -d '" + propertiesJson + "'";
    }

    /**
     * Utility method to return curl to delete property for regular user.
     *
     * @param propertyName property name
     * @return curl to delete property
     */
    private static String deleteCurlPropertyForUser(String propertyName) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XDELETE -i " + ITUtil.HTTP_AUTH_USER_IP_PORT_OLOG_PROPERTIES + "/" + propertyName;
    }

    /**
     * Utility method to return curl to delete property for admin user.
     *
     * @param propertyName property name
     * @return curl to delete property
     */
    private static String deleteCurlPropertyForAdmin(String propertyName) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XDELETE -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_PROPERTIES + "/" + propertyName;
    }

}
