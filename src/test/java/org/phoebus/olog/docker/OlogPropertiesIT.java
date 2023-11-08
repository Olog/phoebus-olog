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

        ITUtilProperties.assertRetrieveProperty("/p11", HttpURLConnection.HTTP_NOT_FOUND);
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

        // might be both 401, 404
        //     401 UNAUTHORIZED
        //     404 NOT_FOUND

        // check permissions

        ITUtilProperties.assertRemoveProperty(AuthorizationChoice.USER,  "/p11", HttpURLConnection.HTTP_NOT_FOUND);
        ITUtilProperties.assertRemoveProperty(AuthorizationChoice.ADMIN, "/p11", HttpURLConnection.HTTP_NOT_FOUND);
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

        ITUtilProperties.assertListProperties(1, default_properties[0]);

        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete1, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete2, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete3, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete4, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete5, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete7, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete8, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/p1", json_incomplete9, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/p1", json_property_p1_name_na,     HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/p1", json_property_p1_attribute_0, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/p1", json_property_p1_attribute_2_state_hyperactive, HttpURLConnection.HTTP_BAD_REQUEST);

        ITUtilProperties.assertListProperties(1, default_properties[0]);
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

        ITUtilProperties.assertListProperties(1, default_properties[0]);

        // check permissions
        // ITUtilProperties.assertCreateProperty(AuthorizationChoice.USER,  "/p1", property_p1_owner_a_state_a_attributes, HttpURLConnection.HTTP_UNAUTHORIZED);

        ITUtilProperties.assertCreateProperty(AuthorizationChoice.USER,  "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);
        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);

        property_check.setName(null);

        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);

        property_check.setName("");

        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);

        property_check.setName("asdf");
        property_check.setOwner("zxcv");
        property_check.setState(State.Active);
        property_check.setAttributes(null);

        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_INTERNAL_ERROR);

        property_check = new Property();
        property_check.setName("asdf");
        property_check.setOwner("zxcv");
        property_check.setState(State.Active);
        property_check.addAttributes(attribute_check);

        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);

        attribute_check.setName(null);
        property_check.getAttributes().clear();
        property_check.addAttributes(attribute_check);

        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);

        attribute_check.setName("");
        property_check.getAttributes().clear();
        property_check.addAttributes(attribute_check);

        ITUtilProperties.assertCreateProperty(AuthorizationChoice.ADMIN, "/asdf", property_check, HttpURLConnection.HTTP_BAD_REQUEST);

        ITUtilProperties.assertListProperties(1, default_properties[0]);
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

        try {
            ITUtilProperties.assertListProperties(1, default_properties[0]);

            ITUtilProperties.assertCreateProperty("/p1", property_p1_owner_a_state_a_attributes);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertListProperties(2,
                    property_p1_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=false", 2,
                    property_p1_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=true", 2,
                    property_p1_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_a_state_a_attributes);

            // check permissions
            // ITUtilProperties.assertRemoveProperty(AuthorizationChoice.USER, "/p1", HttpURLConnection.HTTP_UNAUTHORIZED);

            ITUtilProperties.assertRemoveProperty("/p1");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_a_state_i_attributes);

            ITUtilProperties.assertListProperties("?inactive=false", 1,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=true", 2,
                    property_p1_owner_a_state_i_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties(1, default_properties[0]);
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

        try {
            ITUtilProperties.assertListProperties(1, default_properties[0]);

            ITUtilProperties.assertCreateProperty("/p1", property_p1_owner_a_state_a_attributes);
            ITUtilProperties.assertCreateProperty("/p2", property_p2_owner_a_state_a_attributes);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertListProperties(3,
                    property_p1_owner_a_state_a_attributes,
                    property_p2_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=false", 3,
                    property_p1_owner_a_state_a_attributes,
                    property_p2_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=true", 3,
                    property_p1_owner_a_state_a_attributes,
                    property_p2_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_a_state_a_attributes);
            ITUtilProperties.assertRetrieveProperty("/p2", property_p2_owner_a_state_a_attributes);

            ITUtilProperties.assertRemoveProperty("/p1");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertListProperties(2,
                    property_p2_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=false", 2,
                    property_p2_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=true", 3,
                    property_p1_owner_a_state_i_attributes,
                    property_p2_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_a_state_i_attributes);

            ITUtilProperties.assertRemoveProperty("/p2");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertRetrieveProperty("/p2", property_p2_owner_a_state_i_attributes);

            ITUtilProperties.assertListProperties("?inactive=false", 1,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=true", 3,
                    property_p1_owner_a_state_i_attributes,
                    property_p2_owner_a_state_i_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties(1, default_properties[0]);
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

        try {
            ITUtilProperties.assertListProperties(1, default_properties[0]);

            ITUtilProperties.assertCreateProperty("/p1", property_p1_owner_a_state_a_attributes);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertListProperties(2,
                    property_p1_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_a_state_a_attributes);

            ITUtilProperties.assertCreateProperty("/p1", property_p1_owner_a_state_i_attributes);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertListProperties(1,
                    default_properties[0]);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_a_state_i_attributes);

            ITUtilProperties.assertRemoveProperty("/p1");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_a_state_i_attributes);

            ITUtilProperties.assertListProperties(1, default_properties[0]);
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
        Attribute attribute_check = new Attribute();

        ITUtilProperties.assertListProperties(1, default_properties[0]);

        ITUtilProperties.assertCreateProperties("", properties, HttpURLConnection.HTTP_BAD_REQUEST);

        property_check.setName(null);
        properties[10] = property_check;

        ITUtilProperties.assertCreateProperties("", properties, HttpURLConnection.HTTP_BAD_REQUEST);

        property_check.setName("");
        properties[10] = property_check;

        ITUtilProperties.assertCreateProperties("", properties, HttpURLConnection.HTTP_BAD_REQUEST);

        property_check.setName("asdf");
        property_check.setOwner("zxcv");
        property_check.setState(State.Active);
        property_check.setAttributes(null);
        properties[10] = property_check;

        ITUtilProperties.assertCreateProperties("", properties, HttpURLConnection.HTTP_INTERNAL_ERROR);

        property_check = new Property();
        property_check.setName("asdf");
        property_check.setOwner("zxcv");
        property_check.setState(State.Active);
        property_check.addAttributes(attribute_check);
        properties[10] = property_check;

        ITUtilProperties.assertCreateProperties("", properties, HttpURLConnection.HTTP_BAD_REQUEST);

        attribute_check.setName(null);
        property_check.getAttributes().clear();
        property_check.addAttributes(attribute_check);
        properties[10] = property_check;

        ITUtilProperties.assertCreateProperties("", properties, HttpURLConnection.HTTP_BAD_REQUEST);

        attribute_check.setName("");
        property_check.getAttributes().clear();
        property_check.addAttributes(attribute_check);
        properties[10] = property_check;

        ITUtilProperties.assertCreateProperties("", properties, HttpURLConnection.HTTP_BAD_REQUEST);

        ITUtilProperties.assertListProperties(1, default_properties[0]);
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

        try {
            ITUtilProperties.assertListProperties(1, default_properties[0]);

            ITUtilProperties.assertCreateProperties("", properties_active_inactive);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertListProperties(6,
                    property_p1_owner_a_state_a_attributes,
                    property_p2_owner_a_state_a_attributes,
                    property_p3_owner_a_state_a_attributes,
                    property_p4_owner_a_state_a_attributes,
                    property_p5_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=false", 6,
                    property_p1_owner_a_state_a_attributes,
                    property_p2_owner_a_state_a_attributes,
                    property_p3_owner_a_state_a_attributes,
                    property_p4_owner_a_state_a_attributes,
                    property_p5_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=true", 10,
                    properties_active_inactive);

            ITUtilProperties.assertRetrieveProperty("/p1", property_p1_owner_a_state_a_attributes);
            ITUtilProperties.assertRetrieveProperty("/p2", property_p2_owner_a_state_a_attributes);
            ITUtilProperties.assertRetrieveProperty("/p3", property_p3_owner_a_state_a_attributes);
            ITUtilProperties.assertRetrieveProperty("/p4", property_p4_owner_a_state_a_attributes);
            ITUtilProperties.assertRetrieveProperty("/p5", property_p5_owner_a_state_a_attributes);
            ITUtilProperties.assertRetrieveProperty("/p6", property_p6_owner_a_state_i_attributes);
            ITUtilProperties.assertRetrieveProperty("/p7", property_p7_owner_a_state_i_attributes);
            ITUtilProperties.assertRetrieveProperty("/p8", property_p8_owner_a_state_i_attributes);
            ITUtilProperties.assertRetrieveProperty("/p9", property_p9_owner_a_state_i_attributes);
            ITUtilProperties.assertRetrieveProperty("/p10", property_p10_owner_a_state_i_attributes);

            ITUtilProperties.assertRemoveProperty("/p1");
            ITUtilProperties.assertRemoveProperty("/p2");
            ITUtilProperties.assertRemoveProperty("/p3");
            ITUtilProperties.assertRemoveProperty("/p9");
            ITUtilProperties.assertRemoveProperty("/p10");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertListProperties(3,
                    property_p4_owner_a_state_a_attributes,
                    property_p5_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=false", 3,
                    property_p4_owner_a_state_a_attributes,
                    property_p5_owner_a_state_a_attributes,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=true", 10,
                    properties_active_inactive);

            ITUtilProperties.assertRemoveProperty("/p4");
            ITUtilProperties.assertRemoveProperty("/p5");
            ITUtilProperties.assertRemoveProperty("/p6");
            ITUtilProperties.assertRemoveProperty("/p7");
            ITUtilProperties.assertRemoveProperty("/p8");

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            ITUtilProperties.assertListProperties("?inactive=false", 1,
                    default_properties[0]);

            ITUtilProperties.assertListProperties("?inactive=true", 10,
                    properties_inactive);

            ITUtilProperties.assertListProperties(1, default_properties[0]);
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

}
