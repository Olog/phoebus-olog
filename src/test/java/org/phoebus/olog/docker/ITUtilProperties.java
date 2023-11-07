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
 * Utility class to help (Docker) integration tests for Olog and Elasticsearch with focus on support test of behavior for property endpoints.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.olog.docker.ITUtil
 */
public class ITUtilProperties {

    /**
     * This class is not to be instantiated.
     */
    private ITUtilProperties() {
        throw new IllegalStateException("Utility class");
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Utility method to return curl to create property for regular user.
     *
     * @param propertyName property name
     * @param propertyJson property json
     * @return curl to create property
     */
    public static String createCurlPropertyForUser(String propertyName, String propertyJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_USER_IP_PORT_OLOG_PROPERTIES + "/" + propertyName + " -d '" + propertyJson + "'";
    }

    /**
     * Utility method to return curl to create property for admin user.
     *
     * @param propertyName property name
     * @param propertyJson property json
     * @return curl to create property
     */
    public static String createCurlPropertyForAdmin(String propertyName, String propertyJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_PROPERTIES + "/" + propertyName + " -d '" + propertyJson + "'";
    }

    /**
     * Utility method to return curl to create properties for admin user.
     *
     * @param propertiesJson properties json
     * @return curl to create properties
     */
    public static String createCurlPropertiesForAdmin(String propertiesJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_PROPERTIES + " -d '" + propertiesJson + "'";
    }

    /**
     * Utility method to return curl to delete property for regular user.
     *
     * @param propertyName property name
     * @return curl to delete property
     */
    public static String deleteCurlPropertyForUser(String propertyName) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XDELETE -i " + ITUtil.HTTP_AUTH_USER_IP_PORT_OLOG_PROPERTIES + "/" + propertyName;
    }

    /**
     * Utility method to return curl to delete property for admin user.
     *
     * @param propertyName property name
     * @return curl to delete property
     */
    public static String deleteCurlPropertyForAdmin(String propertyName) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XDELETE -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_PROPERTIES + "/" + propertyName;
    }

}
