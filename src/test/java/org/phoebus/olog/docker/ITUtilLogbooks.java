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
 * Utility class to help (Docker) integration tests for Olog and Elasticsearch with focus on support test of behavior for logbook endpoints.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.olog.docker.ITUtil
 */
public class ITUtilLogbooks {

    /**
     * This class is not to be instantiated.
     */
    private ITUtilLogbooks() {
        throw new IllegalStateException("Utility class");
    }

    // ----------------------------------------------------------------------------------------------------

    /**
     * Utility method to return curl to create logbook for regular user.
     *
     * @param logbookName logbook name
     * @param logbookJson logbook json
     * @return curl to create logbook
     */
    public static String createCurlLogbookForUser(String logbookName, String logbookJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_USER_IP_PORT_OLOG_LOGBOOKS + "/" + logbookName + " -d '" + logbookJson + "'";
    }

    /**
     * Utility method to return curl to create logbook for admin user.
     *
     * @param logbookName logbook name
     * @param logbookJson logbook json
     * @return curl to create logbook
     */
    public static String createCurlLogbookForAdmin(String logbookName, String logbookJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_LOGBOOKS + "/" + logbookName + " -d '" + logbookJson + "'";
    }

    /**
     * Utility method to return curl to create logbooks for admin user.
     *
     * @param logbooksJson logbooks json
     * @return curl to create logbooks
     */
    public static String createCurlLogbooksForAdmin(String logbooksJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_LOGBOOKS + " -d '" + logbooksJson + "'";
    }

    /**
     * Utility method to return curl to delete logbook for regular user.
     *
     * @param logbookName logbook name
     * @return curl to delete logbook
     */
    public static String deleteCurlLogbookForUser(String logbookName) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XDELETE -i " + ITUtil.HTTP_AUTH_USER_IP_PORT_OLOG_LOGBOOKS + "/" + logbookName;
    }

    /**
     * Utility method to return curl to delete logbook for admin user.
     *
     * @param logbookName logbook name
     * @return curl to delete logbook
     */
    public static String deleteCurlLogbookForAdmin(String logbookName) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XDELETE -i " + ITUtil.HTTP_AUTH_ADMIN_IP_PORT_OLOG_LOGBOOKS + "/" + logbookName;
    }

}
