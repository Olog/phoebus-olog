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

import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Event;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.State;
import org.phoebus.olog.entity.Tag;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Purpose to provide test fixture that can be used in multiple test classes and tests.
 *
 * <p>
 * Class is tightly coupled to Olog and Elasticsearch, and requires those to be up and running.
 * Intended usage is by docker integration tests for Olog and Elasticsearch.
 *
 * @author Lars Johansson
 */
public class ITTestFixture {

    // Note
    //     ------------------------------------------------------------------------------------------------
    //     About
    //         set up, tear down and tests (assert statements) to ensure test fixture as expected
    //     ->  no API for removal of log entries, tests using test fixture to be separate from other tests
    //     ------------------------------------------------------------------------------------------------
    //     Content
    //         logs
    //             log
    //                 data
    //                 logbooks
    //                 tags
    //                 properties
    //                 attachments
    //             ...
    //     ------------------------------------------------------------------------------------------------
    //     Olog - Service Documentation
    //         https://olog.readthedocs.io/en/latest/
    //     ------------------------------------------------------------------------------------------------

    static final String MAINTENANCE      = "Maintenance";
    static final String NORMAL_OPERATION = "Normal operation";
    static final String SHIFT_START      = "Shift Start";
    static final String SHIFT_UPDATE     = "Shift Update";
    static final String SHIFT_END        = "Shift End";

    // test data
    static Logbook[]  default_logbooks;
    static Tag[]      default_tags;
    static Property[] default_properties;

    static Logbook logbookBuildings;
    static Logbook logbookCommunication;
    static Logbook logbookExperiments;
    static Logbook logbookFacilities;
    static Logbook logbookMaintenance;
    static Logbook logbookOperations;
    static Logbook logbookPower;
    static Logbook logbookServices;
    static Logbook logbookWater;

    static Tag tagCryo;
    static Tag tagPower;
    static Tag tagSafety;
    static Tag tagSource;
    static Tag tagInitial;
    static Tag tagRadio;
    static Tag tagMagnet;
    static Tag tagSupra;

    static Property propertyShiftInfoCrewEmpty;
    static Property propertyShiftInfoACrew1;
    static Property propertyShiftInfoBCrew2;
    static Property propertyShiftInfoCCrew3;

    static Log logShiftA1001;
    static Log logShiftA1011;
    static Log logShiftA1021;
    static Log logShiftA1031;
    static Log logShiftA1041;
    static Log logShiftA1051;
    static Log logShiftA1061;
    static Log logShiftA1071;
    static Log logShiftA1081;
    static Log logShiftA1091;
    static Log logShiftA1101;
    static Log logShiftA1111;
    static Log logShiftA1121;
    static Log logShiftA1131;
    static Log logShiftA1141;
    static Log logShiftA1151;
    static Log logShiftA1161;
    static Log logShiftA1171;
    static Log logShiftA1181;
    static Log logShiftA1191;

    static Log logShiftB2001;
    static Log logShiftB2011;
    static Log logShiftB2021;
    static Log logShiftB2031;
    static Log logShiftB2041;
    static Log logShiftB2051;
    static Log logShiftB2061;
    static Log logShiftB2071;
    static Log logShiftB2081;
    static Log logShiftB2091;
    static Log logShiftB2101;
    static Log logShiftB2111;
    static Log logShiftB2121;
    static Log logShiftB2131;
    static Log logShiftB2141;
    static Log logShiftB2151;
    static Log logShiftB2161;
    static Log logShiftB2171;
    static Log logShiftB2181;
    static Log logShiftB2191;

    static Log logShiftC3001;
    static Log logShiftC3011;
    static Log logShiftC3021;
    static Log logShiftC3031;
    static Log logShiftC3041;
    static Log logShiftC3051;
    static Log logShiftC3061;
    static Log logShiftC3071;
    static Log logShiftC3081;
    static Log logShiftC3091;
    static Log logShiftC3101;
    static Log logShiftC3111;
    static Log logShiftC3121;
    static Log logShiftC3131;
    static Log logShiftC3141;
    static Log logShiftC3151;
    static Log logShiftC3161;
    static Log logShiftC3171;
    static Log logShiftC3181;
    static Log logShiftC3191;

    /**
     * This class is not to be instantiated.
     */
    private ITTestFixture() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Set up test fixture.
     */
    static void setup() {
        // note
        //     order of items is important
        //         to add items to logs (logbooks, tags, properties, attachments)
        //         for assert statements

        default_logbooks = new Logbook[] {
                new Logbook("operations", "olog-logs", State.Active),
                new Logbook("controls", null, State.Active)};
        default_tags = new Tag[] {new Tag("alarm", State.Active)};
        default_properties = new Property[] {new Property("resource", null, State.Active, new HashSet<Attribute>())};
        default_properties[0].addAttributes(new Attribute("name", null, State.Active));
        default_properties[0].addAttributes(new Attribute("file", null, State.Active));

        setupLogbooks();
        setupTags();
        setupProperties();
        setupLogs();

        createLogbooks();
        createTags();
        createProperties();
        createLogs();
    }

    /**
     * Tear down test fixture.
     */
    static void tearDown() {
        // note
        //     not necessary to remove items from logs in order to tear down (logbooks, tags, properties, attachments)
        //         items can be deleted regardless

        default_logbooks = null;
        default_tags = null;
        default_properties = null;

        tearDownLogbooks();
        tearDownTags();
        tearDownProperties();
        tearDownLogs();
    }

    /**
     * Set up test fixture, logbooks.
     */
    private static void setupLogbooks() {
        // owner not null or empty
        // logbooks - e.g. - activity, function, location
        logbookBuildings     = new Logbook("Buildings",     "admin", State.Active);
        logbookCommunication = new Logbook("Communication", "admin", State.Active);
        logbookExperiments   = new Logbook("Experiments",   "admin", State.Active);
        logbookFacilities    = new Logbook("Facilities",    "admin", State.Active);
        logbookMaintenance   = new Logbook("Maintenance",   "admin", State.Active);
        logbookOperations    = new Logbook("Operations",    "admin", State.Active);
        logbookPower         = new Logbook("Power",         "admin", State.Active);
        logbookServices      = new Logbook("Services",      "admin", State.Active);
        logbookWater         = new Logbook("Water",         "admin", State.Active);
    }

    /**
     * Set up test fixture, tags.
     */
    private static void setupTags() {
        // tags - e.g. - function, location
        tagCryo    = new Tag("Cryo",    State.Active);
        tagPower   = new Tag("Power",   State.Active);
        tagSafety  = new Tag("Safety",  State.Active);
        tagSource  = new Tag("Source",  State.Active);
        tagInitial = new Tag("Initial", State.Active);
        tagRadio   = new Tag("Radio",   State.Active);
        tagMagnet  = new Tag("Magnet",  State.Active);
        tagSupra   = new Tag("Supra",   State.Active);
    }

    /**
     * Set up test fixture, logs.
     */
    private static void setupProperties() {
        // owner not null or empty
        // properties - e.g. - Shift Info
        // attributes - e.g. - Shift ID, Shift Lead, Shift Lead Phone, Shift Lead Email, Operator, Operator Phone, Operator Email
        //            - values - e.g. null, empty, N/A, value

        propertyShiftInfoCrewEmpty = new Property("Shift Info", "admin", State.Active);
        propertyShiftInfoCrewEmpty.addAttributes(new Attribute("Shift ID",         "", State.Active));
        propertyShiftInfoCrewEmpty.addAttributes(new Attribute("Shift Lead",       "", State.Active));
        propertyShiftInfoCrewEmpty.addAttributes(new Attribute("Shift Lead Phone", "", State.Active));
        propertyShiftInfoCrewEmpty.addAttributes(new Attribute("Shift Lead Email", "", State.Active));
        propertyShiftInfoCrewEmpty.addAttributes(new Attribute("Operator",         "", State.Active));
        propertyShiftInfoCrewEmpty.addAttributes(new Attribute("Operator Phone",   "", State.Active));
        propertyShiftInfoCrewEmpty.addAttributes(new Attribute("Operator Email",   "", State.Active));

        // Shift Info A
        propertyShiftInfoACrew1 = new Property("A", "admin", State.Active);
        propertyShiftInfoACrew1.addAttributes(new Attribute("Shift ID",         "12345678A",               State.Active));
        propertyShiftInfoACrew1.addAttributes(new Attribute("Shift Lead",       "Sarah Taylor",            State.Active));
        propertyShiftInfoACrew1.addAttributes(new Attribute("Shift Lead Phone", "23456789",                State.Active));
        propertyShiftInfoACrew1.addAttributes(new Attribute("Shift Lead Email", "sarah.taylor@site.com",   State.Active));
        propertyShiftInfoACrew1.addAttributes(new Attribute("Operator",         "Alberto Silva",           State.Active));
        propertyShiftInfoACrew1.addAttributes(new Attribute("Operator Phone",   "34567890",                State.Active));
        propertyShiftInfoACrew1.addAttributes(new Attribute("Operator Email",   "alberto.silva@site.com",  State.Active));

        // Shift Info B
        propertyShiftInfoBCrew2 = new Property("Info B", "admin", State.Active);
        propertyShiftInfoBCrew2.addAttributes(new Attribute("Shift ID",         "12345678B",               State.Active));
        propertyShiftInfoBCrew2.addAttributes(new Attribute("Shift Lead",       "Maya Kobayashi",          State.Active));
        propertyShiftInfoBCrew2.addAttributes(new Attribute("Shift Lead Phone", "23456789",                State.Active));
        propertyShiftInfoBCrew2.addAttributes(new Attribute("Shift Lead Email", "maya.kobayashi@site.com", State.Active));
        propertyShiftInfoBCrew2.addAttributes(new Attribute("Operator",         "Karl Svensson",           State.Active));
        propertyShiftInfoBCrew2.addAttributes(new Attribute("Operator Phone",   "34567890",                State.Active));
        propertyShiftInfoBCrew2.addAttributes(new Attribute("Operator Email",   "karl.svensson@site.com",  State.Active));

        // Shift Info C
        propertyShiftInfoCCrew3 = new Property("Shift Info C", "admin", State.Active);
        propertyShiftInfoCCrew3.addAttributes(new Attribute("Shift ID",         "12345678C",               State.Active));
        propertyShiftInfoCCrew3.addAttributes(new Attribute("Shift Lead",       "Ali Mansour",             State.Active));
        propertyShiftInfoCCrew3.addAttributes(new Attribute("Shift Lead Phone", "23456789",                State.Active));
        propertyShiftInfoCCrew3.addAttributes(new Attribute("Shift Lead Email", "ali.mansour@site.com",    State.Active));
        propertyShiftInfoCCrew3.addAttributes(new Attribute("Operator",         "N/A",                     State.Active));
        propertyShiftInfoCCrew3.addAttributes(new Attribute("Operator Phone",   "",                        State.Active));
        propertyShiftInfoCCrew3.addAttributes(new Attribute("Operator Email",   "",                        State.Active));
    }

    /**
     * Set up test fixture, logs.
     */
    private static void setupLogs() {
        logShiftA1001 = createLog(1001, "sarahtaylor", SHIFT_START, SHIFT_START,
                SHIFT_START, SHIFT_START,   State.Active, "2007-12-03T10:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoACrew1);
        logShiftA1011 = createLog(1011, "sarahtaylor", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T11:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoACrew1);
        logShiftA1021 = createLog(1021, "sarahtaylor", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T12:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoACrew1);
        logShiftA1031 = createLog(1031, "sarahtaylor", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T13:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoACrew1);
        logShiftA1041 = createLog(1041, "sarahtaylor", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T14:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoACrew1);
        logShiftA1051 = createLog(1051, "sarahtaylor", "Fluctuating cryo temperature", "Fluctuating cryo temperature. ",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T14:30:30.00Z", null, null, logbookOperations, tagCryo, propertyShiftInfoACrew1);
        logShiftA1061 = createLog(1061, "sarahtaylor", "Fluctuating cryo temperature", "Fluctuating cryo temperature. ",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T14:45:30.00Z", null, null, logbookOperations, tagCryo, propertyShiftInfoACrew1);
        logShiftA1071 = createLog(1071, "sarahtaylor", "Fluctuating cryo temperature", "Fluctuating cryo temperature. Sending technician to check cryo.",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T15:00:30.00Z", null, null, logbookOperations, tagCryo, propertyShiftInfoACrew1);
        logShiftA1081 = createLog(1081, "sarahtaylor", "Fluctuating cryo temperature", "Fluctuating cryo temperature. Checking power and cables.",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T15:15:30.00Z", null, null, logbookOperations, tagCryo, propertyShiftInfoACrew1);
        logShiftA1091 = createLog(1091, "sarahtaylor", "Fluctuating cryo temperature", "Fluctuating cryo temperature. Checking sensors and actuators.",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T15:20:30.00Z", null, null, logbookOperations, tagCryo, propertyShiftInfoACrew1);
        logShiftA1101 = createLog(1101, "sarahtaylor", "Fluctuating cryo temperature", "Fluctuating cryo temperature. Checking water and cleaning.",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T15:25:30.00Z", null, null, logbookOperations, tagCryo, propertyShiftInfoACrew1);
        logShiftA1111 = createLog(1111, "sarahtaylor", "Fluctuating cryo temperature", "Fluctuating cryo temperature. (Re)Checking power and cables.",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T15:30:30.00Z", null, null, logbookOperations, tagCryo, propertyShiftInfoACrew1);
        logShiftA1121 = createLog(1121, "sarahtaylor", "Fluctuating cryo temperature", "Fluctuating cryo temperature. Found cause of fluctuation. UPS unit for Cryo to Supra not working causing fluctuations in voltage. Temporary fix with other unit doubling for Cryo to Supra. To be handled at next maintenance window.",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T15:35:30.00Z", null, null, logbookOperations, tagCryo, propertyShiftInfoACrew1);
        logShiftA1121.getLogbooks().add(logbookPower);
        logShiftA1121.getTags().add(tagPower);
        logShiftA1121.getTags().add(tagSupra);
        logShiftA1131 = createLog(1131, "sarahtaylor", "Fluctuating cryo temperature", "Fluctuating cryo temperature. Verified temporary work-around ok.",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T15:40:30.00Z", null, null, logbookOperations, tagCryo, propertyShiftInfoACrew1);
        logShiftA1141 = createLog(1141, "sarahtaylor", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T15:45:30.00Z", null, null, logbookOperations, null, propertyShiftInfoACrew1);
        logShiftA1151 = createLog(1151, "sarahtaylor", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T16:00:30.00Z", null, null, logbookOperations, null, propertyShiftInfoACrew1);
        logShiftA1161 = createLog(1161, "sarahtaylor", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T16:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoACrew1);
        logShiftA1171 = createLog(1171, "sarahtaylor", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T17:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoACrew1);
        logShiftA1181 = createLog(1181, "sarahtaylor", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T18:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoACrew1);
        logShiftA1191 = createLog(1191, "sarahtaylor", SHIFT_END, SHIFT_END,
                SHIFT_END,   SHIFT_END,     State.Active, "2007-12-03T18:25:30.00Z", null, null, logbookOperations, null, propertyShiftInfoACrew1);

        logShiftB2001 = createLog(2001, "mayakobayashi", SHIFT_START, SHIFT_START,
                SHIFT_START, SHIFT_START,  State.Active, "2007-12-03T18:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2011 = createLog(2011, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T19:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2021 = createLog(2021, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T20:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2031 = createLog(2031, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION + ". Visitor Group 1. ",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T20:45:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2041 = createLog(2041, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION + ". Visitor Group 1. ",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T21:05:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2051 = createLog(2051, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T21:25:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2061 = createLog(2061, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION + ". Visitor Group 2. ",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T21:45:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2071 = createLog(2071, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION + ". Visitor Group 2. ",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T22:05:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2081 = createLog(2081, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T22:25:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2091 = createLog(2091, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION + ". Visitor Group 3. ",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T22:45:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2101 = createLog(2101, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION + ". Visitor Group 3. ",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T23:05:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2111 = createLog(2111, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T23:25:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2121 = createLog(2121, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION + ". Visitor Group 4. ",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T23:45:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2131 = createLog(2131, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION + ". Visitor Group 4. ",
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T00:05:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2141 = createLog(2141, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T00:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2151 = createLog(2151, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T00:45:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2161 = createLog(2161, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T01:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2171 = createLog(2171, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T01:45:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2181 = createLog(2181, "mayakobayashi", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-03T02:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);
        logShiftB2191 = createLog(2191, "mayakobayashi", SHIFT_END, SHIFT_END,
                SHIFT_END,   SHIFT_END,    State.Active, "2007-12-04T02:25:30.00Z", null, null, logbookOperations, null, propertyShiftInfoBCrew2);

        logShiftC3001 = createLog(3001, "alimansour", SHIFT_START, SHIFT_START,
                SHIFT_START,  SHIFT_START,  State.Active, "2007-12-04T02:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoCCrew3);
        logShiftC3011 = createLog(3011, "alimansour", "Shut-down for maintenance", "Initiating warm shut-down",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T03:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoCCrew3);
        logShiftC3011.getLogbooks().add(logbookMaintenance);
        logShiftC3021 = createLog(3021, "alimansour", "Shut-down for maintenance", "Shut-down safety check complete",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T03:35:30.00Z", null, null, logbookOperations, tagSafety, propertyShiftInfoCCrew3);
        logShiftC3021.getLogbooks().add(logbookMaintenance);
        logShiftC3031 = createLog(3031, "alimansour", "Shut-down for maintenance", "Warm shut-down complete",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T03:55:30.00Z", null, null, logbookOperations, null, propertyShiftInfoCCrew3);
        logShiftC3031.getLogbooks().add(logbookMaintenance);
        logShiftC3041 = createLog(3041, "alimansour", "Maintenance for Source",  "Check power, pressure, temperature, flow",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T04:15:30.00Z", null, null, logbookMaintenance, tagSource, propertyShiftInfoCCrew3);
        logShiftC3051 = createLog(3051, "alimansour", "Maintenance for Source",  "Visual inspection",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T04:35:30.00Z", null, null, logbookMaintenance, tagSource, propertyShiftInfoCCrew3);
        logShiftC3061 = createLog(3061, "alimansour", "Maintenance for Initial", "Check power, pressure, temperature, flow",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T04:55:30.00Z", null, null, logbookMaintenance, tagInitial, propertyShiftInfoCCrew3);
        logShiftC3071 = createLog(3071, "alimansour", "Maintenance for Initial", "Visual inspection",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T05:15:30.00Z", null, null, logbookMaintenance, tagInitial, propertyShiftInfoCCrew3);
        logShiftC3081 = createLog(3081, "alimansour", "Maintenance for Radio",   "Check power, pressure, temperature, flow",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T05:35:30.00Z", null, null, logbookMaintenance, tagRadio, propertyShiftInfoCCrew3);
        logShiftC3091 = createLog(3091, "alimansour", "Maintenance for Radio",   "Visual inspection",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T05:55:30.00Z", null, null, logbookMaintenance, tagRadio, propertyShiftInfoCCrew3);
        logShiftC3101 = createLog(3101, "alimansour", "Maintenance for Magnet",  "Check power, pressure, temperature, flow",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T06:15:30.00Z", null, null, logbookMaintenance, tagMagnet, propertyShiftInfoCCrew3);
        logShiftC3111 = createLog(3111, "alimansour", "Maintenance for Magnet",  "Visual inspection",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T06:35:30.00Z", null, null, logbookMaintenance, tagMagnet, propertyShiftInfoCCrew3);
        logShiftC3121 = createLog(3121, "alimansour", "Maintenance for Supra",   "Check power, pressure, temperature, flow. Replace UPS unit for Cryo to Supra.",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T06:55:30.00Z", null, null, logbookMaintenance, tagSupra, propertyShiftInfoCCrew3);
        logShiftC3121.getLogbooks().add(logbookPower);
        logShiftC3121.getTags().add(tagCryo);
        logShiftC3121.getTags().add(tagPower);
        logShiftC3131 = createLog(3131, "alimansour", "Maintenance for Supra",   "Visual inspection",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T07:15:30.00Z", null, null, logbookMaintenance, tagSupra, propertyShiftInfoCCrew3);
        logShiftC3141 = createLog(3131, "alimansour", "Maintenance end ok",      "End of maintenance",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T07:35:30.00Z", null, null, logbookMaintenance, null, propertyShiftInfoCCrew3);

        logShiftC3151 = createLog(3141, "alimansour", "Start-up after maintenance", "Initiating warm start-up",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T07:55:30.00Z", null, null, logbookOperations, null, propertyShiftInfoCCrew3);
        logShiftC3151.getLogbooks().add(logbookMaintenance);
        logShiftC3161 = createLog(3151, "alimansour", "Start-up after maintenance", "Warm start-up safety check complete",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T08:15:30.00Z", null, null, logbookOperations, tagSafety, propertyShiftInfoCCrew3);
        logShiftC3161.getLogbooks().add(logbookMaintenance);
        logShiftC3171 = createLog(3161, "alimansour", "Start-up after maintenance", "Warm start-up complete",
                MAINTENANCE, SHIFT_UPDATE,  State.Active, "2007-12-04T08:30:30.00Z", null, null, logbookOperations, null, propertyShiftInfoCCrew3);
        logShiftC3171.getLogbooks().add(logbookMaintenance);
        logShiftC3181 = createLog(3181, "alimansour", NORMAL_OPERATION, NORMAL_OPERATION,
                SHIFT_UPDATE, SHIFT_UPDATE, State.Active, "2007-12-04T09:15:30.00Z", null, null, logbookOperations, null, propertyShiftInfoCCrew3);
        logShiftC3191 = createLog(3191, "alimansour", SHIFT_END, SHIFT_END,
                SHIFT_END,    SHIFT_END,    State.Active, "2007-12-04T10:25:30.00Z", null, null, logbookOperations, null, propertyShiftInfoCCrew3);
    }

    /**
     * Create test fixture, logbooks.
     */
    private static void createLogbooks() {
        try {
            // --------------------------------------------------------------------------------
            // clean start
            // --------------------------------------------------------------------------------

            ITUtilLogbooks.assertListLogbooks(2,
                    default_logbooks[1],
                    default_logbooks[0]);

            // --------------------------------------------------------------------------------
            // create
            // --------------------------------------------------------------------------------

            String name = URLEncoder.encode(logbookBuildings.getName(), StandardCharsets.UTF_8);
            ITUtilLogbooks.assertCreateLogbook("/" + name, logbookBuildings);

            name = URLEncoder.encode(logbookCommunication.getName(), StandardCharsets.UTF_8);
            ITUtilLogbooks.assertCreateLogbook("/" + name, logbookCommunication);

            name = URLEncoder.encode(logbookExperiments.getName(), StandardCharsets.UTF_8);
            ITUtilLogbooks.assertCreateLogbook("/" + name, logbookExperiments);

            name = URLEncoder.encode(logbookFacilities.getName(), StandardCharsets.UTF_8);
            ITUtilLogbooks.assertCreateLogbook("/" + name, logbookFacilities);

            name = URLEncoder.encode(logbookMaintenance.getName(), StandardCharsets.UTF_8);
            ITUtilLogbooks.assertCreateLogbook("/" + name, logbookMaintenance);

            name = URLEncoder.encode(logbookOperations.getName(), ITUtil.UTF_8);
            ITUtilLogbooks.assertCreateLogbook("/" + name, logbookOperations);

            name = URLEncoder.encode(logbookPower.getName(), ITUtil.UTF_8);
            ITUtilLogbooks.assertCreateLogbook("/" + name, logbookPower);

            name = URLEncoder.encode(logbookServices.getName(), ITUtil.UTF_8);
            ITUtilLogbooks.assertCreateLogbook("/" + name, logbookServices);

            name = URLEncoder.encode(logbookWater.getName(), ITUtil.UTF_8);
            ITUtilLogbooks.assertCreateLogbook("/" + name, logbookWater);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            // --------------------------------------------------------------------------------
            // well defined state
            // --------------------------------------------------------------------------------

            ITUtilLogbooks.assertListLogbooks(10);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Create test fixture, tags.
     */
    private static void createTags() {
        try {
            // --------------------------------------------------------------------------------
            // clean start
            // --------------------------------------------------------------------------------

            ITUtilTags.assertListTags(1, default_tags[0]);

            // --------------------------------------------------------------------------------
            // create
            // --------------------------------------------------------------------------------

            String name = URLEncoder.encode(tagCryo.getName(), ITUtil.UTF_8);
            ITUtilTags.assertCreateTag("/" + name, tagCryo);

            name = URLEncoder.encode(tagPower.getName(), ITUtil.UTF_8);
            ITUtilTags.assertCreateTag("/" + name, tagPower);

            name = URLEncoder.encode(tagSafety.getName(), ITUtil.UTF_8);
            ITUtilTags.assertCreateTag("/" + name, tagSafety);

            name = URLEncoder.encode(tagSource.getName(), ITUtil.UTF_8);
            ITUtilTags.assertCreateTag("/" + name, tagSource);

            name = URLEncoder.encode(tagInitial.getName(), ITUtil.UTF_8);
            ITUtilTags.assertCreateTag("/" + name, tagInitial);

            name = URLEncoder.encode(tagRadio.getName(), ITUtil.UTF_8);
            ITUtilTags.assertCreateTag("/" + name, tagRadio);

            name = URLEncoder.encode(tagMagnet.getName(), ITUtil.UTF_8);
            ITUtilTags.assertCreateTag("/" + name, tagMagnet);

            name = URLEncoder.encode(tagSupra.getName(), ITUtil.UTF_8);
            ITUtilTags.assertCreateTag("/" + name, tagSupra);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            // --------------------------------------------------------------------------------
            // well defined state
            // --------------------------------------------------------------------------------

            ITUtilTags.assertListTags(9);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Create test fixture, properties.
     */
    private static void createProperties() {
        try {
            // --------------------------------------------------------------------------------
            // clean start
            // --------------------------------------------------------------------------------

            ITUtilProperties.assertListProperties(1, default_properties[0]);

            // --------------------------------------------------------------------------------
            // create
            // --------------------------------------------------------------------------------

            String name = URLEncoder.encode(propertyShiftInfoCrewEmpty.getName(), StandardCharsets.UTF_8);
            ITUtilProperties.assertCreateProperty("/" + name, propertyShiftInfoCrewEmpty);

            name = URLEncoder.encode(propertyShiftInfoACrew1.getName(), StandardCharsets.UTF_8);
            ITUtilProperties.assertCreateProperty("/" + name, propertyShiftInfoACrew1);

            name = URLEncoder.encode(propertyShiftInfoBCrew2.getName(), StandardCharsets.UTF_8);
            ITUtilProperties.assertCreateProperty("/" + name, propertyShiftInfoBCrew2);

            name = URLEncoder.encode(propertyShiftInfoCCrew3.getName(), StandardCharsets.UTF_8);
            ITUtilProperties.assertCreateProperty("/" + name, propertyShiftInfoCCrew3);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            // --------------------------------------------------------------------------------
            // well defined state
            // --------------------------------------------------------------------------------

            ITUtilProperties.assertListProperties(5);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Create test fixture, logs.
     */
    private static void createLogs() {
        // logbooks, tags, properties to be created before log is created
        try {
            // --------------------------------------------------------------------------------
            // clean start
            // --------------------------------------------------------------------------------

            ITUtilLogs.assertListLogs(0);

            // --------------------------------------------------------------------------------
            // create
            // --------------------------------------------------------------------------------

            ITUtilLogs.assertCreateLog("", logShiftA1001);
            ITUtilLogs.assertCreateLog("", logShiftA1011);
            ITUtilLogs.assertCreateLog("", logShiftA1021);
            ITUtilLogs.assertCreateLog("", logShiftA1031);
            ITUtilLogs.assertCreateLog("", logShiftA1041);
            ITUtilLogs.assertCreateLog("", logShiftA1051);
            ITUtilLogs.assertCreateLog("", logShiftA1061);
            ITUtilLogs.assertCreateLog("", logShiftA1071);
            ITUtilLogs.assertCreateLog("", logShiftA1081);
            ITUtilLogs.assertCreateLog("", logShiftA1091);
            ITUtilLogs.assertCreateLog("", logShiftA1101);
            ITUtilLogs.assertCreateLog("", logShiftA1111);
            ITUtilLogs.assertCreateLog("", logShiftA1121);
            ITUtilLogs.assertCreateLog("", logShiftA1131);
            ITUtilLogs.assertCreateLog("", logShiftA1141);
            ITUtilLogs.assertCreateLog("", logShiftA1151);
            ITUtilLogs.assertCreateLog("", logShiftA1161);
            ITUtilLogs.assertCreateLog("", logShiftA1171);
            ITUtilLogs.assertCreateLog("", logShiftA1181);
            ITUtilLogs.assertCreateLog("", logShiftA1191);

            ITUtilLogs.assertCreateLog("", logShiftB2001);
            ITUtilLogs.assertCreateLog("", logShiftB2011);
            ITUtilLogs.assertCreateLog("", logShiftB2021);
            ITUtilLogs.assertCreateLog("", logShiftB2031);
            ITUtilLogs.assertCreateLog("", logShiftB2041);
            ITUtilLogs.assertCreateLog("", logShiftB2051);
            ITUtilLogs.assertCreateLog("", logShiftB2061);
            ITUtilLogs.assertCreateLog("", logShiftB2071);
            ITUtilLogs.assertCreateLog("", logShiftB2081);
            ITUtilLogs.assertCreateLog("", logShiftB2091);
            ITUtilLogs.assertCreateLog("", logShiftB2101);
            ITUtilLogs.assertCreateLog("", logShiftB2111);
            ITUtilLogs.assertCreateLog("", logShiftB2121);
            ITUtilLogs.assertCreateLog("", logShiftB2131);
            ITUtilLogs.assertCreateLog("", logShiftB2141);
            ITUtilLogs.assertCreateLog("", logShiftB2151);
            ITUtilLogs.assertCreateLog("", logShiftB2161);
            ITUtilLogs.assertCreateLog("", logShiftB2171);
            ITUtilLogs.assertCreateLog("", logShiftB2181);
            ITUtilLogs.assertCreateLog("", logShiftB2191);

            ITUtilLogs.assertCreateLog("", logShiftC3001);
            ITUtilLogs.assertCreateLog("", logShiftC3011);
            ITUtilLogs.assertCreateLog("", logShiftC3021);
            ITUtilLogs.assertCreateLog("", logShiftC3031);
            ITUtilLogs.assertCreateLog("", logShiftC3041);
            ITUtilLogs.assertCreateLog("", logShiftC3051);
            ITUtilLogs.assertCreateLog("", logShiftC3061);
            ITUtilLogs.assertCreateLog("", logShiftC3071);
            ITUtilLogs.assertCreateLog("", logShiftC3081);
            ITUtilLogs.assertCreateLog("", logShiftC3091);
            ITUtilLogs.assertCreateLog("", logShiftC3101);
            ITUtilLogs.assertCreateLog("", logShiftC3111);
            ITUtilLogs.assertCreateLog("", logShiftC3121);
            ITUtilLogs.assertCreateLog("", logShiftC3131);
            ITUtilLogs.assertCreateLog("", logShiftC3141);
            ITUtilLogs.assertCreateLog("", logShiftC3151);
            ITUtilLogs.assertCreateLog("", logShiftC3161);
            ITUtilLogs.assertCreateLog("", logShiftC3171);
            ITUtilLogs.assertCreateLog("", logShiftC3181);
            ITUtilLogs.assertCreateLog("", logShiftC3191);

            // refresh elastic indices
            ITUtil.assertRefreshElasticIndices();

            // --------------------------------------------------------------------------------
            // well defined state
            // --------------------------------------------------------------------------------

            ITUtilLogs.assertListLogs(60);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Tear down test fixture, logbooks.
     */
    private static void tearDownLogbooks() {
        logbookBuildings     = null;
        logbookCommunication = null;
        logbookExperiments   = null;
        logbookFacilities    = null;
        logbookMaintenance   = null;
        logbookOperations    = null;
        logbookPower         = null;
        logbookServices      = null;
        logbookWater         = null;
    }

    /**
     * Tear down test fixture, tags.
     */
    private static void tearDownTags() {
        tagCryo    = null;
        tagPower   = null;
        tagSafety  = null;
        tagSource  = null;
        tagInitial = null;
        tagRadio   = null;
        tagMagnet  = null;
        tagSupra   = null;
    }

    /**
     * Tear down test fixture, properties.
     */
    private static void tearDownProperties() {
        propertyShiftInfoCrewEmpty = null;
        propertyShiftInfoACrew1    = null;
        propertyShiftInfoBCrew2    = null;
        propertyShiftInfoCCrew3    = null;
    }

    /**
     * Tear down test fixture, logs.
     */
    private static void tearDownLogs() {
        logShiftA1001 = null;
        logShiftA1011 = null;
        logShiftA1021 = null;
        logShiftA1031 = null;
        logShiftA1041 = null;
        logShiftA1051 = null;
        logShiftA1061 = null;
        logShiftA1071 = null;
        logShiftA1081 = null;
        logShiftA1091 = null;
        logShiftA1101 = null;
        logShiftA1111 = null;
        logShiftA1121 = null;
        logShiftA1131 = null;
        logShiftA1141 = null;
        logShiftA1151 = null;
        logShiftA1161 = null;
        logShiftA1171 = null;
        logShiftA1181 = null;
        logShiftA1191 = null;

        logShiftB2001 = null;
        logShiftB2011 = null;
        logShiftB2021 = null;
        logShiftB2031 = null;
        logShiftB2041 = null;
        logShiftB2051 = null;
        logShiftB2061 = null;
        logShiftB2071 = null;
        logShiftB2081 = null;
        logShiftB2091 = null;
        logShiftB2101 = null;
        logShiftB2111 = null;
        logShiftB2121 = null;
        logShiftB2131 = null;
        logShiftB2141 = null;
        logShiftB2151 = null;
        logShiftB2161 = null;
        logShiftB2171 = null;
        logShiftB2181 = null;
        logShiftB2191 = null;

        logShiftC3001 = null;
        logShiftC3011 = null;
        logShiftC3021 = null;
        logShiftC3031 = null;
        logShiftC3041 = null;
        logShiftC3051 = null;
        logShiftC3061 = null;
        logShiftC3071 = null;
        logShiftC3081 = null;
        logShiftC3091 = null;
        logShiftC3101 = null;
        logShiftC3111 = null;
        logShiftC3121 = null;
        logShiftC3131 = null;
        logShiftC3141 = null;
        logShiftC3151 = null;
        logShiftC3161 = null;
        logShiftC3171 = null;
        logShiftC3181 = null;
        logShiftC3191 = null;
    }

    /**
     * Utility method to create a log entry.
     *
     * @param id log entry id
     * @param owner log entry owner
     * @param source log entry source
     * @param description log entry description
     * @param title log entry title
     * @param level log entry level
     * @param state log entry state
     * @param createdDate log created date
     * @param modifyDate log modify date
     * @param events log events
     * @param logbook log logbook
     * @param tag log tag
     * @param property log property
     * @return log entry
     */
    private static Log createLog(
            long id, String owner, String source, String description, String title, String level, State state,
            String createdDate, String modifyDate, List<Event> events, Logbook logbook, Tag tag, Property property) {
        Log log = new Log.LogBuilder().build();
        log.setId(Long.valueOf(id));
        log.setOwner(owner);
        log.setSource(source);
        log.setDescription(description);
        log.setTitle(title);
        log.setLevel(level);
        log.setState(state);
        log.setCreatedDate(createdDate != null ? Instant.parse(createdDate) : null);
        log.setModifyDate(modifyDate != null ? Instant.parse(modifyDate) : null);
        log.setEvents(events);
        if (logbook != null) {
            log.getLogbooks().add(logbook);
        }
        if (tag != null) {
            log.getTags().add(tag);
        }
        if (property != null) {
            log.getProperties().add(property);
        }
        return log;
    }

}
