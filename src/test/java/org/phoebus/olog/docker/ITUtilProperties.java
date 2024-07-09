/*
 * Copyright (C) 2023 European Spallation Source ERIC.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.phoebus.olog.docker.ITUtil.AuthorizationChoice;
import org.phoebus.olog.docker.ITUtil.EndpointChoice;
import org.phoebus.olog.docker.ITUtil.MethodChoice;
import org.phoebus.olog.entity.Property;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class to help (Docker) integration tests for Olog and Elasticsearch with focus on support test of behavior for property endpoints.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.olog.docker.ITUtil
 */
public class ITUtilProperties {

    static final ObjectMapper mapper = new ObjectMapper();

    static final Property[] PROPERTIES_NULL = null;
    static final Property   PROPERTY_NULL   = null;

    /**
     * This class is not to be instantiated.
     */
    private ITUtilProperties() {
        throw new IllegalStateException("Utility class");
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Return string for property.
     *
     * @param value property
     * @return string for property
     */
    static String object2Json(Property value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }
    /**
     * Return string for property array.
     *
     * @param value property array
     * @return string for property array
     */
    static String object2Json(Property[] value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertRetrieveProperty(String, int, Property)
     */
    public static Property assertRetrieveProperty(String path, int expectedResponseCode) {
        return assertRetrieveProperty(path, expectedResponseCode, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertRetrieveProperty(String, int, Property)
     */
    public static Property assertRetrieveProperty(String path, Property expected) {
        return assertRetrieveProperty(path, HttpURLConnection.HTTP_OK, expected);
    }
    /**
     * Utility method to return the property with the given name.
     *
     * @param path path
     * @param expectedResponseCode expected response code
     * @param expected expected response property
     */
    public static Property assertRetrieveProperty(String path, int expectedResponseCode, Property expected) {
        try {
            String[] response = null;
            Property actual = null;

            response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + path);
            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Property.class);
            }

            if (expected != null) {
                assertEquals(expected, actual);
            }

            return actual;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertListProperties(String, int, int, int, Property...)
     */
    public static Property[] assertListProperties(int expectedEqual, Property... expected) {
        return assertListProperties("", HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    /**
     * @see ITUtilProperties#assertListProperties(String, int, int, int, Property...)
     */
    public static Property[] assertListProperties(String queryString, int expectedEqual, Property... expected) {
        return assertListProperties(queryString, HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    /**
     * Utility method to return the list of all properties in the directory.
     *
     * @param queryString query string
     * @param expectedResponseCode expected response code
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response properties
     * @return number of properties
     */
    public static Property[] assertListProperties(String queryString, int expectedResponseCode, int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, Property... expected) {
        try {
            String[] response = null;
            Property[] actual = null;

            response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_OLOG_PROPERTIES + queryString);
            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Property[].class);
            }

            // expected number of items in list
            //     (if non-negative number)
            //     expectedGreaterThanOrEqual <= nbr of items <= expectedLessThanOrEqual
            if (expectedGreaterThanOrEqual >= 0) {
                assertTrue(actual.length >= expectedGreaterThanOrEqual);
            }
            if (expectedLessThanOrEqual >= 0) {
                assertTrue(actual.length <= expectedLessThanOrEqual);
            }

            // expected content
            if (expected != null && expected.length > 0) {
                ITUtil.assertEqualsProperties(actual, expected);
            }

            return actual;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertCreateProperty(AuthorizationChoice, String, String, int, Property)
     */
    public static Property assertCreateProperty(String path, Property value) {
        return assertCreateProperty(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateProperty(AuthorizationChoice, String, String, int, Property)
     */
    public static Property assertCreateProperty(AuthorizationChoice authorizationChoice, String path, Property value) {
        return assertCreateProperty(authorizationChoice, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateProperty(AuthorizationChoice, String, String, int, Property)
     */
    public static Property assertCreateProperty(AuthorizationChoice authorizationChoice, String path, Property value, int expectedResponseCode) {
        return assertCreateProperty(authorizationChoice, path, object2Json(value), expectedResponseCode, PROPERTY_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateProperty(AuthorizationChoice, String, String, int, Property)
     */
    public static Property assertCreateProperty(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateProperty(authorizationChoice, path, json, expectedResponseCode, PROPERTY_NULL);
    }
    /**
     * Utility method to create or completely replace the existing property name with the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response property
     */
    public static Property assertCreateProperty(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Property expected) {
        try {
            String[] response = null;
            Property actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.PUT, authorizationChoice, EndpointChoice.PROPERTIES, path, json));
            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Property.class);
            }

            if (expected != null) {
                assertEquals(expected, actual);
            }

            return actual;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertCreateProperties(AuthorizationChoice, String, String, int, Property[])
     */
    public static Property[] assertCreateProperties(String path, Property[] value) {
        return assertCreateProperties(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, PROPERTIES_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateProperties(AuthorizationChoice, String, String, int, Property[])
     */
    public static Property[] assertCreateProperties(String path, Property[] value, int expectedResponseCode) {
        return assertCreateProperties(AuthorizationChoice.ADMIN, path, object2Json(value), expectedResponseCode, PROPERTIES_NULL);
    }
    /**
     * @see ITUtilProperties#assertCreateProperties(AuthorizationChoice, String, String, int, Property[])
     */
    public static Property[] assertCreateProperties(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateProperties(authorizationChoice, path, json, expectedResponseCode, PROPERTIES_NULL);
    }
    /**
     * Utility method to add the properties in the payload to the directory.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response properties
     */
    public static Property[] assertCreateProperties(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Property[] expected) {
        try {
            String[] response = null;
            Property[] actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.PUT, authorizationChoice, EndpointChoice.PROPERTIES, path, json));
            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Property[].class);
            }

            if (expected != null) {
                ITUtil.assertEqualsProperties(expected, actual);
            }

            return actual;
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilProperties#assertRemoveProperty(AuthorizationChoice, String, int)
     */
    public static void assertRemoveProperty(String path) {
        assertRemoveProperty(AuthorizationChoice.ADMIN, path, HttpURLConnection.HTTP_OK);
    }
    /**
     * Utility method to remove property with the given name.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param expectedResponseCode expected response code
     */
    public static void assertRemoveProperty(AuthorizationChoice authorizationChoice, String path, int expectedResponseCode) {
        try {
            String[] response = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.DELETE, authorizationChoice, EndpointChoice.PROPERTIES, path, null));
            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
        } catch (IOException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

}
