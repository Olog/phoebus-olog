# Rest HTTP API


## Creating a Log Entry

Create a log entry

**PUT** <https://localhost:8181/Olog/logs/multipart>

```json
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
```

**NOTE** Attachment ids must be unique, e.g. UUID. When creating a log entry - optionally with attachments - client **must**:

1. Use a multipart request and set the Content-Type to "multipart/form-data", even if no attachments are present.

#. If attachments are present: add one request part per attachment file, in the order they appear in the log entry. Each
file must be added using "files" as the name for the part.
#. Add the log entry as a request part with content type "application/json". The name of the part must be "logEntry".

Client must also be prepared to handle a HTTP 413 (payload too large) response in case the attached files exceed
file and request size limits configured in the service.

## Reply to a log entry

This uses the same end point as when creating a log entry, but client must
send the unique id of the log entry to which the new one is a reply.

**PUT** <https://localhost:8181/Olog/logs>?inReplyTo=\<id>

If \<id> does not identify an existing log entry, a HTTP 400 status is returned.

## Adding a single attachment

**POST** <https://localhost:8181/Olog/logs/attachments>/\{logId}

```HTML
Content-Type: multipart/form-data; boundary=----formBoundary
------formBoundary
Content-Disposition: form-data; name="filename"
Content-Type: application/json
{"image1.png"}
------formBoundary
```

## Searching for Log Entries

**GET** <https://localhost:8181/Olog/logs>

Search Parameters

```{eval-rst}
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
```

For time based search requests the client may specify a **tz** parameter indicating the client's time zone.
The format must be recognized as a valid zone identifier, see for instance <https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html>.
If the client does not specify the time zone, the time zone of the service is used to compute start end end timestamps.
An invalid time zone specifier will result in a HTTP 400 (bad request) response.

Example:

**GET** <https://localhost:8181/Olog/logs/search?desc=dump&logbooks=Operations>

The above search request will return all log entires with the term "dump" in their
descriptions and which are part of the Operations logbook.

Retrieving an attachment of a log entry

**GET** <https://localhost:8181/Olog/logs/attachments>/\{logId}/\{filename}

Find entries with at least one attachment of type 'image'

**GET** <https://localhost:8181/Olog/logs/search?attachments=image>

## Updating a Log Entry

**POST** <https://localhost:8181/Olog/logs>/\{logId}

Update a log entry, the orginal log entry is archived in a seperate elastic index before any of the changes are applied.

Note: the create date, attachments, and events cannot be modified.

```json
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
```

## Managing Logbooks, Tags and Levels

Retrieve the list of existing tags

**GET** <https://localhost:8181/Olog/tags>

Retrieve the list of existing logbooks

**GET** <https://localhost:8181/Olog/logbooks>

Retrieve the list of existing levels

**GET** <https://localhost:8181/Olog/levels>

Create a new tag

**PUT** <https://localhost:8181/Olog/tags>/\{tagName}

```json
{
     "name":"Fault",
     "state":"Active"
}
```

Create multiple tags

**PUT** <https://localhost:8181/Olog/tags>

```json
[
  {"name":"Fault", "state":"Active" },
  {"name":"Alarm", "state":"Active" }
]
```

Create a new logbook

**PUT** <https://localhost:8181/Olog/logbooks>/\{logbookName}

```json
{
     "name":"Operations",
     "owner":"olog-logs",
     "state":"Active"
}
```

Create multiple logbooks

**PUT** <https://localhost:8181/Olog/logbooks>

```json
[
  {"name":"Operations", "owner":"olog-logs", "state":"Active"},
  {"name":"DAMA",       "owner":"olog-logs", "state":"Active"}
]
```

Create a new level

**PUT** <https://localhost:8181/Olog/level/>{levelName}

```json
{
     "name":"Info",
     "defaultLevel":[true|false]
}
```

**NOTE**: only one single level may be defined as default level.

## Managing Properties

Retrieve the list of existing properties

**GET** <https://localhost:8181/Olog/properties>

Create a new property

**PUT** <https://localhost:8181/Olog/properties>/\{propertyName}

```json
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
```

Create multiple properties

**PUT** <https://localhost:8181/Olog/properties>

```json
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
```

## Templates

Log entry templates can be added to the storage to support use cases when the same type of log entries need to be
created on a regular basis. Templates have the same structure a regular log entries, except for attachments.

To add a new template, use:

**PUT** <https://localhost:8181/Olog/templates>

```json
{
   "description":"Template text",
   "level":"Info",
   "title":"Some title",
   "logbooks":[
      {
         "name":"Operations"
      }
   ]
}
```

In the client UI (currently only CS Studio/Phoebus) users may select from a list of templates, if available. Upon
selection of a template, the client will populate the editor's input controls based on the template content.
