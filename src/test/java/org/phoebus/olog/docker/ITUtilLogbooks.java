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

import java.net.HttpURLConnection;

import org.phoebus.olog.docker.ITUtil.AuthorizationChoice;
import org.phoebus.olog.docker.ITUtil.EndpointChoice;
import org.phoebus.olog.docker.ITUtil.MethodChoice;
import org.phoebus.olog.entity.Logbook;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Utility class to help (Docker) integration tests for Olog and Elasticsearch with focus on support test of behavior for logbook endpoints.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.olog.docker.ITUtil
 */
public class ITUtilLogbooks {

    private static final Logbook[] LOGBOOKS_NULL = null;
    private static final Logbook   LOGBOOK_NULL  = null;

    /**
     * This class is not to be instantiated.
     */
    private ITUtilLogbooks() {
        throw new IllegalStateException("Utility class");
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Return string for logbook.
     *
     * @param value logbook
     * @return string for logbook
     */
    static String object2Json(Logbook value) {
        try {
            return ITUtil.MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }
    /**
     * Return string for logbook array.
     *
     * @param value logbook array
     * @return string for logbook array
     */
    static String object2Json(Logbook[] value) {
        try {
            return ITUtil.MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilLogbooks#assertRetrieveLogbook(String, int, Logbook)
     */
    public static Logbook assertRetrieveLogbook(String path, int expectedResponseCode) {
        return assertRetrieveLogbook(path, expectedResponseCode, LOGBOOK_NULL);
    }
    /**
     * @see ITUtilLogbooks#assertRetrieveLogbook(String, int, Logbook)
     */
    public static Logbook assertRetrieveLogbook(String path, Logbook expected) {
        return assertRetrieveLogbook(path, HttpURLConnection.HTTP_OK, expected);
    }
    /**
     * Utility method to return the logbook with the given name.
     *
     * @param path path
     * @param expectedResponseCode expected response code
     * @param expected expected response logbook
     */
    public static Logbook assertRetrieveLogbook(String path, int expectedResponseCode, Logbook expected) {
        Logbook actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_OLOG_LOGBOOKS + path);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Logbook.class);
            }
            if (expected != null) {
                assertEquals(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilLogbooks#assertListLogbooks(int, int, int, Logbook...)
     */
    public static Logbook[] assertListLogbooks(int expectedEqual, Logbook... expected) {
        return assertListLogbooks(HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    /**
     * Utility method to return the list of all logbooks in the directory.
     *
     * @param expectedResponseCode expected response code
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response logbooks
     * @return number of logbooks
     */
    public static Logbook[] assertListLogbooks(int expectedResponseCode, int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, Logbook... expected) {
        Logbook[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_OLOG_LOGBOOKS);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Logbook[].class);
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
            if (expected != null && expected.length > 0) {
                ITUtil.assertEqualsLogbooks(actual, expected);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilLogbooks#assertCreateLogbook(AuthorizationChoice, String, String, int, Logbook)
     */
    public static Logbook assertCreateLogbook(String path, Logbook value) {
        return assertCreateLogbook(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, LOGBOOK_NULL);
    }
    /**
     * @see ITUtilLogbooks#assertCreateLogbook(AuthorizationChoice, String, String, int, Logbook)
     */
    public static Logbook assertCreateLogbook(AuthorizationChoice authorizationChoice, String path, Logbook value) {
        return assertCreateLogbook(authorizationChoice, path, object2Json(value), HttpURLConnection.HTTP_OK, LOGBOOK_NULL);
    }
    /**
     * @see ITUtilLogbooks#assertCreateLogbook(AuthorizationChoice, String, String, int, Logbook)
     */
    public static Logbook assertCreateLogbook(AuthorizationChoice authorizationChoice, String path, Logbook value, int expectedResponseCode) {
        return assertCreateLogbook(authorizationChoice, path, object2Json(value), expectedResponseCode, LOGBOOK_NULL);
    }
    /**
     * @see ITUtilLogbooks#assertCreateLogbook(AuthorizationChoice, String, String, int, Logbook)
     */
    public static Logbook assertCreateLogbook(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateLogbook(authorizationChoice, path, json, expectedResponseCode, LOGBOOK_NULL);
    }
    /**
     * Utility method to create or completely replace the existing logbook name with the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response logbook
     */
    public static Logbook assertCreateLogbook(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Logbook expected) {
        Logbook actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.PUT, authorizationChoice, EndpointChoice.LOGBOOKS, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Logbook.class);
            }
            if (expected != null) {
                assertEquals(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilLogbooks#assertCreateLogbooks(AuthorizationChoice, String, String, int, Logbook[])
     */
    public static Logbook[] assertCreateLogbooks(String path, Logbook[] value) {
        return assertCreateLogbooks(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, LOGBOOKS_NULL);
    }
    /**
     * @see ITUtilLogbooks#assertCreateLogbooks(AuthorizationChoice, String, String, int, Logbook[])
     */
    public static Logbook[] assertCreateLogbooks(String path, Logbook[] value, int expectedResponseCode) {
        return assertCreateLogbooks(AuthorizationChoice.ADMIN, path, object2Json(value), expectedResponseCode, LOGBOOKS_NULL);
    }
    /**
     * @see ITUtilLogbooks#assertCreateLogbooks(AuthorizationChoice, String, String, int, Logbook[])
     */
    public static Logbook[] assertCreateLogbooks(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateLogbooks(authorizationChoice, path, json, expectedResponseCode, LOGBOOKS_NULL);
    }
    /**
     * Utility method to add the logbooks in the payload to the directory.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response logbooks
     */
    public static Logbook[] assertCreateLogbooks(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Logbook[] expected) {
        Logbook[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.PUT, authorizationChoice, EndpointChoice.LOGBOOKS, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Logbook[].class);
            }
            if (expected != null) {
                ITUtil.assertEqualsLogbooks(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilLogbooks#assertRemoveLogbook(AuthorizationChoice, String, int)
     */
    public static void assertRemoveLogbook(String path) {
        assertRemoveLogbook(AuthorizationChoice.ADMIN, path, HttpURLConnection.HTTP_OK);
    }
    /**
     * Utility method to remove logbook with the given name.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param expectedResponseCode expected response code
     */
    public static void assertRemoveLogbook(AuthorizationChoice authorizationChoice, String path, int expectedResponseCode) {
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.DELETE, authorizationChoice, EndpointChoice.LOGBOOKS, path, null));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
        } catch (Exception e) {
            fail();
        }
    }

}
