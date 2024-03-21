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
import java.util.List;

import org.phoebus.olog.docker.ITUtil.AuthorizationChoice;
import org.phoebus.olog.docker.ITUtil.EndpointChoice;
import org.phoebus.olog.docker.ITUtil.MethodChoice;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.SearchResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class to help (Docker) integration tests for Olog and Elasticsearch with focus on support test of behavior for log endpoints.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.olog.docker.ITUtil
 */
public class ITUtilLogs {

    static final ObjectMapper mapper = new ObjectMapper();

    static final Log[] LOGS_NULL = null;
    static final Log   LOG_NULL  = null;

    /**
     * This class is not to be instantiated.
     */
    private ITUtilLogs() {
        throw new IllegalStateException("Utility class");
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Return string for log.
     *
     * @param value log
     * @return string for log
     */
    static String object2Json(Log value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }
    /**
     * Return string for log array.
     *
     * @param value log array
     * @return string for log array
     */
    static String object2Json(Log[] value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            fail();
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * @see ITUtilLogs#assertRetrieveLog(String, int, Log)
     */
    public static Log assertRetrieveLog(String path) {
    	return assertRetrieveLog(path, HttpURLConnection.HTTP_OK, LOG_NULL);
    }
    /**
     * Utility method to return the log with the given name.
     *
     * @param path path
     * @param expectedResponseCode expected response code
     * @param expected expected response log
     */
    public static Log assertRetrieveLog(String path, int expectedResponseCode, Log expected) {
    	try {
    		String[] response = null;
    		Log actual = null;

    		response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_LOGS + path);
    		ITUtil.assertResponseLength2Code(response, expectedResponseCode);
    		if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
    			actual = mapper.readValue(response[1], Log.class);
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
     * @see ITUtilLogs#assertListLogs(String, int, int, int, Log...)
     */
    public static Log[] assertListLogs(int expectedEqual, Log... expected) {
        return assertListLogs("", HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    /**
     * @see ITUtilLogs#assertListLogs(String, int, int, int, Log...)
     */
    public static Log[] assertListLogs(String queryString, int expectedEqual, Log... expected) {
        return assertListLogs(queryString, HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    /**
     * Utility method to return the list of all logs in the directory.
     *
     * @param queryString query string
     * @param expectedResponseCode expected response code
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response logs
     * @return number of logs
     */
    public static Log[] assertListLogs(String queryString, int expectedResponseCode, int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, Log... expected) {
        try {
            String[] response = null;
            Log[] actual = null;

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_LOGS + queryString);
            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Log[].class);
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
                ITUtil.assertEqualsLogs(actual, expected);
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
     * @see ITUtilLogs#assertSearchLogs(String, int, int, int, Log...)
     */
    public static SearchResult assertSearchLogs(int expectedEqual, Log... expected) {
        return assertSearchLogs("", HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    /**
     * @see ITUtilLogs#assertSearchLogs(String, int, int, int, Log...)
     */
    public static SearchResult assertSearchLogs(String queryString, int expectedEqual, Log... expected) {
        return assertSearchLogs(queryString, HttpURLConnection.HTTP_OK, expectedEqual, expectedEqual, expected);
    }
    /**
     * Utility method to return the list of all logs in the directory.
     *
     * @param queryString query string
     * @param expectedResponseCode expected response code
     * @param expectedGreaterThanOrEqual (if non-negative number) greater than or equal to this number of items
     * @param expectedLessThanOrEqual (if non-negative number) less than or equal to this number of items
     * @param expected expected response logs
     * @return number of logs
     */
    public static SearchResult assertSearchLogs(String queryString, int expectedResponseCode, int expectedGreaterThanOrEqual, int expectedLessThanOrEqual, Log... expected) {
        try {
            String[] response = null;
            SearchResult actual = null;

            response = ITUtil.doGetJson(ITUtil.HTTP_IP_PORT_OLOG_LOGS + "/search" + queryString);
            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], SearchResult.class);
            }

            // expected number of items in list
            //     (if non-negative number)
            //     expectedGreaterThanOrEqual <= nbr of items <= expectedLessThanOrEqual
            if (expectedGreaterThanOrEqual >= 0) {
                assertTrue(actual.getHitCount() >= expectedGreaterThanOrEqual);
            }
            if (expectedLessThanOrEqual >= 0) {
                assertTrue(actual.getHitCount() <= expectedLessThanOrEqual);
            }

            // expected content
            if (expected != null && expected.length > 0) {
                ITUtil.assertEqualsLogs((Log[]) actual.getLogs().toArray(), expected);
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
     * @see ITUtilLogs#assertCreateLog(AuthorizationChoice, String, String, int, Log)
     */
    public static Log assertCreateLog(String path, Log value) {
        return assertCreateLog(AuthorizationChoice.ADMIN, path, object2Json(value), HttpURLConnection.HTTP_OK, LOG_NULL);
    }
    /**
     * @see ITUtilLogs#assertCreateLog(AuthorizationChoice, String, String, int, Log)
     */
    public static Log assertCreateLog(AuthorizationChoice authorizationChoice, String path, Log value) {
        return assertCreateLog(authorizationChoice, path, object2Json(value), HttpURLConnection.HTTP_OK, LOG_NULL);
    }
    /**
     * @see ITUtilLogs#assertCreateLog(AuthorizationChoice, String, String, int, Log)
     */
    public static Log assertCreateLog(AuthorizationChoice authorizationChoice, String path, Log value, int expectedResponseCode) {
        return assertCreateLog(authorizationChoice, path, object2Json(value), expectedResponseCode, LOG_NULL);
    }
    /**
     * @see ITUtilLogs#assertCreateLog(AuthorizationChoice, String, String, int, Log)
     */
    public static Log assertCreateLog(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode) {
        return assertCreateLog(authorizationChoice, path, json, expectedResponseCode, LOG_NULL);
    }
    /**
     * Utility method to create or completely replace the existing log name with the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response log
     */
    public static Log assertCreateLog(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Log expected) {
        try {
            String[] response = null;
            Log actual = null;

            response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.PUT, authorizationChoice, EndpointChoice.LOGS, path, json));
            ITUtil.assertResponseLength2Code(response, expectedResponseCode);
            if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
                actual = mapper.readValue(response[1], Log.class);
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
     * @see ITUtilLogs#assertUpdateLog(AuthorizationChoice, String, String, int, Log)
     */
    public static Log assertUpdateLog(Log value) {
    	return assertUpdateLog(AuthorizationChoice.ADMIN, "/" + value.getId(), object2Json(value), HttpURLConnection.HTTP_OK, LOG_NULL);
    }
    /**
     * Utility method to update an existing log with the payload data.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param path path
     * @param json json
     * @param expectedResponseCode expected response code
     * @param expected expected response log
     * @return
     */
    public static Log assertUpdateLog(AuthorizationChoice authorizationChoice, String path, String json, int expectedResponseCode, Log expected) {
    	try {
    		String[] response = null;
    		Log actual = null;

    		response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.POST, authorizationChoice, EndpointChoice.LOGS, path, json));
    		ITUtil.assertResponseLength2Code(response, expectedResponseCode);
    		if (HttpURLConnection.HTTP_OK == expectedResponseCode) {
    			actual = mapper.readValue(response[1], Log.class);
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
     * @see ITUtilLogs#assertGroupLogs(AuthorizationChoice, List, int)
     */
    public static void assertGroupLogs(List<Long> logEntryIds) {
    	assertGroupLogs(AuthorizationChoice.ADMIN, logEntryIds, HttpURLConnection.HTTP_OK);
    }
    /**
     * Utility method to group log entries, with given id values, together with a joint log entry group property.
     *
     * @param authorizationChoice authorization choice (none, user, admin)
     * @param logEntryIds log entry id values
     * @param expectedResponseCode expected response code
     */
    public static void assertGroupLogs(AuthorizationChoice authorizationChoice, List<Long> logEntryIds, int expectedResponseCode) {
    	try {
    		String[] response = null;

    		response = ITUtil.runShellCommand(ITUtil.curlMethodAuthEndpointPathJson(MethodChoice.POST, authorizationChoice, EndpointChoice.LOGS, "/group", mapper.writeValueAsString(logEntryIds)));
    		ITUtil.assertResponseLength2Code(response, expectedResponseCode);
    	} catch (IOException e) {
    		fail();
    	} catch (Exception e) {
    		fail();
    	}
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Utility method to return curl to create log for regular user.
     *
     * @param logJson log json
     * @return curl to create log
     */
    public static String createCurlLogForUser(String logJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_USER_IP_PORT_OLOG_LOGS + " -d '" + logJson + "'";
    }

    /**
     * Utility method to return curl to create log for admin user.
     *
     * @param logJson log json
     * @return curl to create log
     */
    public static String createCurlLogForAdmin(String logJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_LOGS + " -d '" + logJson + "'";
    }

}
