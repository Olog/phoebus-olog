Olog Service Documentation!
===========================

Olog is an online logbook service which allows for the creation and retrieval of log entries.

The service was developed to address the needs of operators, engineers, and users of large scientific facilities.

Key features:
 - Integration with CS-Studio, Phoebus, Bluesky, and other controls and data acquisition tools.
 - Tags & Logbooks provide an effective way to organize and sort log entries
 - Categorization of a log entry supported through the "level" meta-data field.
 - Additional (searchable) meta-data offered through "properties".
 - Support for fuzzy searching
 - Markup support for creating rich text log entries. Markup is based on the Commonmark specification, extended
   with support for image size and tables. Clients may request a HTML formatted quick reference (maintained
   in the project) resource using an URL like http(s)://url.to.service/CommonmarkCheatsheet.html.


A Log Entry:
############

.. code-block:: json

   {
      "id":575,
      "owner":"testOwner1",
      "source":"**Beam Dump** due to Major power dip Current Alarms Booster transmitter switched back to lower state.
                PV : BR-RF{Xmtr-PLC}ICS:Down-Sts",
      "description":"Beam Dump due to Major power dip Current Alarms Booster transmitter switched back to lower state.
                     PV : BR-RF{Xmtr-PLC}ICS:Down-Sts",
      "level":"Info",
      "title":"Some title",
      "state":"Active",
      "createdDate":1577392617217,
      "logbooks":[
         {
            "name":"ControlsOperations",
            "owner":"operators"
         }
      ],
      "tags":[
         {
            "name":"Fault"
         }
      ],
      "properties":[
         {
            "name":"FaultReport",
            "owner":"testOwner1",
            "attributes":[
               {
                  "name":"id",
                  "value":"1234"
               },
               {
                  "name":"URL",
                  "value":"https://nsls2.bnl.gov/faults/1234"
               }
            ]
         }
      ],
      "attachments":[
         {
            "id":"5e21fe829fef8a53f0183d4a",
            "filename":"screenshot_image.png",
            "fileMetadataDescription":"image.png"
         }
      ],
      "events":[
         {
            "name":"faultTime",
            "instant":1577389011004
         }
      ]
   }

LogEntry
************
| The most important filed of any log entry are the description and owner.
| **description:** Is the text body of a log entry.
| **owner:** The creator of the log entry.
| **level** Info, Warning, 

| Other fields include
| **source:** When markup support is enabled the description text + the markup annotations are stored in this field.
| **id:** The unique identifier for a log entry, this is automatically generated at the time of creation.
| **createTime:** Another automatically generated field which stores the unix epoch millisecond at which the log entry was created.

Attachment
************

Each log entry can have a list attachments, these can be any type of files.

Logbooks & Tags
***************

Logbooks and Tags are useful organization tools. Each log entry can be associated with one or more logbooks and have zero or more tags.


Properties
************

A property is a list of key value pairs. The provide means of attaching meta data to a log entry,
this data can be used to integrate with other services or capture context information.
 
Some examples include

A property to link log entries to Tickets 

.. code-block:: json

   {
    "name":"ticket",
    "attributes":[
       {
          "name":"id",
          "value":"1234"
       },
       {
          "name":"URL",
          "value":"https://trac.nsls2.bnl.gov/ticket/1234"
       }]
   }

events
*******

There are instances when the log entry being created is actually associated with an event that happened some time ago. 
The users had higher priority tasks to address at that moment and is able to log the event after those tasks.
The using **events** allows users to associate log entries with different instances in time,
time based searches will ensure that these log entries are also found even if the create time might not fall in the search range.

Quick Start
############

Download and install elasticsearch (verision 8.3) from elastic.com
Download and install mongodb from mongodb

Configure the service
The configuration files for Phoebus Olog are present under phoebus-olog/tree/master/src/main/resources/applications.properties

Build
::

   mvn clean install

Start the service
::

   mvn org.springframework.boot:spring-boot-maven-plugin:run

Detailed Installation Instructions:
`Install Phoebus Olog <https://github.com/Olog/phoebus-olog#installation>`_.

REST API 
#########

Creating a Log Entry
***********************

Create a simple log entry 

NOTE: deprecated, will be removed in future commits. Replaced by https://localhost:8181/Olog/logs/multipart
**PUT** https://localhost:8181/Olog/logs

.. code-block:: json

 {
      "owner":"log",
      "description":"Beam Dump due to Major power dip Current Alarms Booster transmitter switched back to lower state.",
      "level":"Info",
      "title":"Some title",
      "logbooks":[
         {
            "name":"Operations"
         }
      ]
 }

Create a log entry, optionally with file attachments

**PUT** https://localhost:8181/Olog/logs/multipart

.. code-block:: json

 {
    "owner":"log",
      "description":"Beam Dump due to Major power dip Current Alarms Booster transmitter switched back to lower state.",
      "level":"Info",
      "title":"Some title",
      "logbooks":[
         {
            "name":"Operations"
         }
      ],
      "attachments":[
         {"id": "82dd67fa-09df-11ee-be56-0242ac120002", "filename":"MyScreenShot.png"},
         {"id": "c02948ad-4bbd-432f-aa4d-a687a54f8d40", "filename":"MySpreadsheet.xlsx"}
      ]
 }

**NOTE** Attachment ids must be unique, e.g. UUID. When creating a log entry - optionally with attachments - client **must**:

#. Use a multipart request and set the Content-Type to "multipart/form-data", even if no attachments are present.
#. If attachments are present: add one request part per attachment file, in the order they appear in the log entry. Each
file must be added using "files" as the name for the part.
#. Add the log entry as a request part with content type "application/json". The name of the part must be "logEntry".

Client must also be prepared to handle a HTTP 413 (payload too large) response in case the attached files exceed
file and request size limits configured in the service.


Reply to a log entry. This uses the same end point as when creating a log entry, but client must
send the unique id of the log entry to which the new one is a reply.

**PUT** https://localhost:8181/Olog/logs?inReplyTo=<id>

If <id> does not identify an existing log entry, a HTTP 400 status is returned.

Adding an attachment 

**POST** https://localhost:8181/Olog/logs/attachments/{logId}

.. code-block:: HTML
 
 Content-Type: multipart/form-data; boundary=----formBoundary
 ------formBoundary
 Content-Disposition: form-data; name="filename"
 Content-Type: application/json
 {"image1.png"}
 ------formBoundary
 Content-Disposition: form-data; name="fileMetadataDescription"
 Content-Type: application/json
 {"image/png"}
 ------formBoundary
 Content-Disposition: form-data; name="file "; filename="image1.png"
 Content-Type: application/octet-steam
 {…file content…}
 ------formBoundary--



Searching for Log Entries
**************************

**GET** https://localhost:8181/Olog/logs

Search Parameters

+---------------+------------------------------------------------------------------+
|Keyword        | Descriptions                                                     |
+===============+==================================================================+
| **Text search**                                                                  |
+---------------+------------------------------------------------------------------+
|*text*         | A list of keywords which are present in the log entry description|
+---------------+------------------------------------------------------------------+
|*fuzzy*        | Allow fuzzy searches                                             |
+---------------+------------------------------------------------------------------+
|*phrase*       | Finds log entries with the exact same word/s                     |
+---------------+------------------------------------------------------------------+
|*owner*        | Finds log entries with the given owner                           |
+---------------+------------------------------------------------------------------+
| **Time based searches**                                                          |
+---------------+------------------------------------------------------------------+
|*start*        | Search for log entries created after given time instant          |
+---------------+------------------------------------------------------------------+
|*end*          | Search for log entries created before the given time instant     |
+---------------+------------------------------------------------------------------+
|*includeevents*| A flag to include log event times when                           |
+---------------+------------------------------------------------------------------+
| **Meta Data searches**                                                           |
+---------------+------------------------------------------------------------------+
|*tags*         | Search for log entries with at least one of the given tags       |
+---------------+------------------------------------------------------------------+
|*logbooks*     | Search for log entries with at least one of the given logbooks   |
+---------------+------------------------------------------------------------------+
| **Attachments searches**                                                         |
+---------------+------------------------------------------------------------------+
|*attachments*  | To search for entries with at least one attachment               |
+---------------+------------------------------------------------------------------+
| **Pagination searches**                                                          |
+---------------+------------------------------------------------------------------+
|*size*         | The number of log entries to be returned within each page        |
+---------------+------------------------------------------------------------------+
|*page*         | The page number, i.e page 1 is the 1 to 1+size log               |
|               |  entries matching the search                                     |
+---------------+------------------------------------------------------------------+
|*Sorting Search Results*                                                          |
+---------------+------------------------------------------------------------------+
|*sort*         | `up|down` order the search results based on create time          |
+---------------+------------------------------------------------------------------+

For time based search requests the client may specify a **tz** parameter indicating the client's time zone.
The format must be recognized as a valid zone identifier, see for instance https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html.
If the client does not specify the time zone, the time zone of the service is used to compute start end end timestamps.
An invalid time zone specifier will result in a HTTP 400 (bad request) response.

Example:

**GET** https://localhost:8181/Olog/logs/search?desc=dump&logbooks=Operations

The above search request will return all log entires with the term "dump" in their 
descriptions and which are part of the Operations logbook.

Retrieving an attachment of a log entry
 
**GET** https://localhost:8181/Olog/logs/attachments/{logId}/{filename}

Find entries with at least one attachment of type 'image'

**GET** https://localhost:8181/Olog/logs/search?attachments=image

Updating a Log Entry
********************

**POST** https://localhost:8181/Olog/logs/{logId}

Update a log entry, the orginal log entry is archived in a seperate elastic index before any of the changes are applied.

Note: the create date, attachments, and events cannot be modified.

.. code-block:: json

 {
      "owner":"log",
      "description":"Beam Dump due to Major power dip Current Alarms Booster transmitter switched back to lower state.
                     New important info appended",
      "level":"Info",
      "title":"A new title",
      "logbooks":[
         {
            "name":"Operations"
         }
      ]
 }

Managing Logbooks, Tags and Levels
**********************************

Retrieve the list of existing tags
 
**GET** https://localhost:8181/Olog/tags

Retrieve the list of existing logbooks

**GET** https://localhost:8181/Olog/logbooks

Retrieve the list of existing levels

**GET** https://localhost:8181/Olog/levels


Create a new tag

**PUT** https://localhost:8181/Olog/tags/{tagName}

.. code-block:: json

 https://localhost:8181/Olog/tags/Fault

 {
      "name":"Fault",
      "state":"Active"
 }
 
Create multiple tags

**PUT** https://localhost:8181/Olog/tags
  
.. code-block:: json

 https://localhost:8181/Olog/tags

 [
   {"name":"Fault", "state":"Active" },
   {"name":"Alarm", "state":"Active" }
 ]
 
Create a new logbook

**PUT** https://localhost:8181/Olog/logbooks/{logbookName}

.. code-block:: json

 https://localhost:8181/Olog/logbooks/Operations

 {
      "name":"Operations",
      "owner":"olog-logs",
      "state":"Active"
 }

Create multiple logbooks

**PUT** https://localhost:8181/Olog/logbooks

.. code-block:: json

 https://localhost:8181/Olog/logbooks

 [
   {"name":"Operations", "owner":"olog-logs", "state":"Active"},
   {"name":"DAMA",       "owner":"olog-logs", "state":"Active"}
 ]

 Create a new level

 **PUT** https://localhost:8181/Olog/level/{levelName}

 .. code-block:: json

  https://localhost:8181/Olog/level/Info

  {
       "name":"Info",
       "defaultLevel":[true|false]
  }

**NOTE**: only one single level may be defined as default level.

Managing Properties
*******************

Retrieve the list of existing properties
 
**GET** https://localhost:8181/Olog/properties

Create a new property

**PUT** https://localhost:8181/Olog/properties/{propertyName}

.. code-block:: json

 {
      "name":"Ticket",
      "owner":"olog-logs",
      "state":"Active",
      "attributes":[
         {
            "name":"id",
            "state":"Active"
         },
         {
            "name":"url",
            "state":"Active"
         }
      ]
 }

Create multiple properties

**PUT** https://localhost:8181/Olog/properties

.. code-block:: json

 [
   {
      "name":"Ticket",
      "owner":"olog-logs",
      "state":"Active",
      "attributes":[
         {"name":"id", "state":"Active"},
         {"name":"url", "state":"Active"}
      ]
   },
      {
      "name":"Scan",
      "owner":"olog-logs",
      "state":"Active",
      "attributes":[
         {"name":"id", "state":"Active"}
      ]
   }
 ]
 
`Javadocs <apidocs/index.html>`_

Authentication
##############

In general, non-GET methods are protected, i.e. client needs to send a basic authentication header for each request.
Alternatively, client may use a session cookie returned upon successful authentication with the login endpoint:

**POST** https://localhost:8181/Olog/login

.. code-block:: json

{"username":"johndoe", "password":"undisclosed"}

Developer Documentation:
########################

.. toctree::
   :maxdepth: 1

   changelog
