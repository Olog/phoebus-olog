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

/**
 * Utility class to help (Docker) integration tests for Olog and Elasticsearch with focus on support test of behavior for tag endpoints.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.olog.docker.ITUtil
 */
public class ITUtilTags {

    /**
     * This class is not to be instantiated.
     */
    private ITUtilTags() {
        throw new IllegalStateException("Utility class");
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Utility method to return curl to create tag for regular user.
     *
     * @param tagName tag name
     * @param tagJson tag json
     * @return curl to create tag
     */
    public static String createCurlTagForUser(String tagName, String tagJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_USER_IP_PORT_OLOG_TAGS + "/" + tagName + " -d '" + tagJson + "'";
    }

    /**
     * Utility method to return curl to create tag for admin user.
     *
     * @param tagName tag name
     * @param tagJson tag json
     * @return curl to create tag
     */
    public static String createCurlTagForAdmin(String tagName, String tagJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_TAGS + "/" + tagName + " -d '" + tagJson + "'";
    }

    /**
     * Utility method to return curl to create tags for admin user.
     *
     * @param tagsJson tags json
     * @return curl to create tags
     */
    public static String createCurlTagsForAdmin(String tagsJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_TAGS + " -d '" + tagsJson + "'";
    }

    /**
     * Utility method to return curl to delete tag for regular user.
     *
     * @param tagName tag name
     * @return curl to delete tag
     */
    public static String deleteCurlTagForUser(String tagName) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XDELETE -i " + ITUtil.HTTP_AUTH_USER_IP_PORT_OLOG_TAGS + "/" + tagName;
    }

    /**
     * Utility method to return curl to delete tag for admin user.
     *
     * @param tagName tag name
     * @return curl to delete tag
     */
    public static String deleteCurlTagForAdmin(String tagName) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XDELETE -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_TAGS + "/" + tagName;
    }

}
