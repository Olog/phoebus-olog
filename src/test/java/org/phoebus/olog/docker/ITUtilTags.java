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
import org.phoebus.olog.entity.Tag;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Utility class to help (Docker) integration tests for Olog and Elasticsearch with focus on support test of behavior for tag endpoints.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.olog.docker.ITUtil
 */
public class ITUtilTags {

	private static final Tag[] TAGS_NULL = null;
	private static final Tag   TAG_NULL  = null;

    /**
     * This class is not to be instantiated.
     */
    private ITUtilTags() {
        throw new IllegalStateException("Utility class");
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Return string for tag.
     *
     * @param value tag
     * @return string for tag
     */
    static String object2Json(Tag value) {
        try {
            return ITUtil.MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }
    /**
     * Return string for tag array.
     *
     * @param value tag array
     * @return string for tag array
     */
    static String object2Json(Tag[] value) {
        try {
            return ITUtil.MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilTags#assertRetrieveTag(String, int, Tag)
     */
    public static Tag assertRetrieveTag(String path, int expectedResponseCode) {
        return assertRetrieveTag(path, expectedResponseCode, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertRetrieveTag(String, int, Tag)
     */
    public static Tag assertRetrieveTag(String path, Tag expected) {
        return assertRetrieveTag(path, HttpURLConnection.HTTP_OK, expected);
    }
    /**
     * Utility method to return the tag with the given name.
     *
     * @param path path
     * @param expectedResponseCode expected response code
     * @param expected expected response tag
     */
    public static Tag assertRetrieveTag(String path, int expectedResponseCode, Tag expected) {
        Tag actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_OLOG_TAGS + path);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Tag.class);
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
     * @see ITUtilTags#assertListTags(int, int, int, Tag...)
     */
    public static Tag[] assertListTags(int expectedEqual, Tag... expected) {
        return assertListTags(HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    /**
     * Utility method to return the list of all tags in the directory.
     *
     * @param expectedResponseCode expected response code
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response tags
     * @return number of tags
     */
    public static Tag[] assertListTags(int expectedResponseCode, int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, Tag... expected) {
        Tag[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.HTTP_IP_PORT_OLOG_TAGS);

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Tag[].class);
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
                ITUtil.assertEqualsTags(actual, expected);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilTags#assertCreateTag(AuthorizationChoice, String, String, int, Tag)
     */
    public static Tag assertCreateTag(String path, Tag value) {
        return assertCreateTag(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateTag(AuthorizationChoice, String, String, int, Tag)
     */
    public static Tag assertCreateTag(AuthorizationChoice authorizationChoice, String path, Tag value) {
        return assertCreateTag(authorizationChoice, path, object2Json(value), HttpURLConnection.HTTP_OK, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateTag(AuthorizationChoice, String, String, int, Tag)
     */
    public static Tag assertCreateTag(AuthorizationChoice authorizationChoice, String path, Tag value, int expectedResponseCode) {
        return assertCreateTag(authorizationChoice, path, object2Json(value), expectedResponseCode, TAG_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateTag(AuthorizationChoice, String, String, int, Tag)
     */
    public static Tag assertCreateTag(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateTag(authorizationChoice, path, json, expectedResponseCode, TAG_NULL);
    }
    /**
     * Utility method to create or completely replace the existing tag name with the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response tag
     */
    public static Tag assertCreateTag(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Tag expected) {
        Tag actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.PUT, authorizationChoice, EndpointChoice.TAGS, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Tag.class);
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
     * @see ITUtilTags#assertCreateTags(AuthorizationChoice, String, String, int, Tag[])
     */
    public static Tag[] assertCreateTags(String path, Tag[] value) {
        return assertCreateTags(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, TAGS_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateTags(AuthorizationChoice, String, String, int, Tag[])
     */
    public static Tag[] assertCreateTags(String path, Tag[] value, int expectedResponseCode) {
        return assertCreateTags(AuthorizationChoice.ADMIN, path, object2Json(value), expectedResponseCode, TAGS_NULL);
    }
    /**
     * @see ITUtilTags#assertCreateTags(AuthorizationChoice, String, String, int, Tag[])
     */
    public static Tag[] assertCreateTags(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateTags(authorizationChoice, path, json, expectedResponseCode, TAGS_NULL);
    }
    /**
     * Utility method to add the tags in the payload to the directory.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response tags
     */
    public static Tag[] assertCreateTags(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Tag[] expected) {
        Tag[] actual = null;
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.PUT, authorizationChoice, EndpointChoice.TAGS, path, json));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = ITUtil.MAPPER.readValue(response[1], Tag[].class);
            }
            if (expected != null) {
                ITUtil.assertEqualsTags(expected, actual);
            }
        } catch (Exception e) {
            fail();
        }
        return actual;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilTags#assertRemoveTag(AuthorizationChoice, String, int)
     */
    public static void assertRemoveTag(String path) {
        assertRemoveTag(AuthorizationChoice.ADMIN, path, HttpURLConnection.HTTP_OK);
    }
    /**
     * Utility method to remove tag with the given name.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param expectedResponseCode expected response code
     */
    public static void assertRemoveTag(AuthorizationChoice authorizationChoice, String path, int expectedResponseCode) {
        try {
            String[] response = ITUtil.sendRequest(ITUtil.buildRequest(MethodChoice.DELETE, authorizationChoice, EndpointChoice.TAGS, path, null));

            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
        } catch (Exception e) {
            fail();
        }
    }

}
