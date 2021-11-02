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
 