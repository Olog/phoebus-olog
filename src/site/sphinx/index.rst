Olog Service Documentation!
===========================

Olog-es is an online logbook service which allows for the creation and retrieval of log entries.

The service was developed to address the needs of operators, engineers, and users of large scientific facilities.

Key features:
 - Integration with cs-studio, phoebus, bluesky, and other controls and data acquisition tools.
 - Tags & Logbooks provide an effective way to organize and sort log entries
 - Support for fuzzy searching
 - Markup support for creating rich log entries


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

Download and install elasticsearch (verision 6.3) from elastic.com
Download and install mongodb from mongodb

Configure the service
The configuration files for olog-es are present under olog-es/tree/master/src/main/resources/applications.properties

Build
::

   mvn clean install

Start the service
::

   mvn org.springframework.boot:spring-boot-maven-plugin:run

Detailed Installation Instructions:
`Install Olog-es <https://github.com/Olog/olog-es#olog-es--->`_.

REST API 
#########

Creating a Log Entry
***********************

Create a simple log entry 

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
|*desc*         | A list of keywords which are present in the log entry description| 
+---------------+------------------------------------------------------------------+
|*fuzzy*        | Allow fuzzy searches                                             |
+---------------+------------------------------------------------------------------+
|*phrase*       | Finds log entries with the exact same word/s                     |
+---------------+------------------------------------------------------------------+
|*owner*        | Finds log entries with the given owner                           |
+---------------+------------------------------------------------------------------+
+---------------+------------------------------------------------------------------+
| **Time based searches**                                                          |
+---------------+------------------------------------------------------------------+
|*start*        | Search for log entries created after given time instant          |
+---------------+------------------------------------------------------------------+
|*end*          | Search for log entries created before the given time instant     |
+---------------+------------------------------------------------------------------+
|*includeevents*| A flag to include log event times when                           |
+---------------+------------------------------------------------------------------+
+---------------+------------------------------------------------------------------+
| **Meta Data searches**                                                           |
+---------------+------------------------------------------------------------------+
|*tags*         | Search for log entries with at least one of the given tags       |
+---------------+------------------------------------------------------------------+
|*logbooks*     | Search for log entries with at least one of the given logbooks   |
+---------------+------------------------------------------------------------------+

Example:

**GET** https://localhost:8181/Olog/logs?desc=dump&logbooks=Operations

The above search request will return all log entires with the term "dump" in their 
descriptions and which are part of the Operations logbook.

Retrieving an attachment of a log entry
 
**GET** https://localhost:8181/Olog/logs/attachments/{logId}/{filename}


Managing Logbooks & Tags
************************

Retrieve the list of existing tags
 
**GET** https://localhost:8181/Olog/tags

Retrieve the list of existing logbooks

**GET** https://localhost:8181/Olog/logbooks

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
