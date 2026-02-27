# Log Entry Reference

A log entry is represented by a JSON structure.

## Available Properties

Alphabetical list of all properties:

:attachments: Can contain a list of attachments. Each attachment entry consists of:

  - `id`: Unique identifier within the attachment storage
  - `filename`: Filename of the attachment
  - `fileMetadataDescription`: ?

:createdDate: Unix timestamp of the date the entry was created.
:modifiedDate: Unix timestamp of when the entry was updated last.
   `null` if it has never been modified.
:description: Same as **source**, but stripped by any markdown.
   Used internally for the search function.
:events: *Deprecated*
:id: The unique ID of the log entry
:level: The type of the entry.
   Mandatory and single select based on the predefined list of levels.
:logbooks: List of logbooks this entry is assosciated with.
   Mandatory to have one entry.
   Each logbook entry consits of:

  **name**: Name of the logbook \
  **owner**: Owner of the logbook.
   Automatically populated based on the logbook definitions. \
  **state**: State of this logbook. Automatically populated based on the logbook definitions.

:owner: Author of the original entry, provided by the authenticator provider
:source: The content of the logbook entry.
:state: State of the current entry. *Active* by default.
:tags: List of tags for this entry.
   Tags are pre-defined.
   Each tag consists of:

  **name**: Name of the tag \
  **state**: Stage of the tag. Automatically populated.

:title: Title of the log entry.
:properties: List of properties for this entry.
   Properties are pre-defined.
   Each propertiy consists of:

   **name**: Name of the property \
   **owner**: Owner of this property \
   **state**: State of this property \
   **attributes**: List of key value pairs of attributes

       **name**: Key of the attribute
       **value**: User-filled content for this attribute
       **state**: State of this attribute

## Full Example

```json
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
         "owner":"operators",
         "state":"Active"
      }
   ],
   "tags":[
      {
         "name":"Fault",
         "state":"Active",

      }
   ],
   "properties":[
      {
         "name":"FaultReport",
         "owner":"testOwner1",
         "state":"Active",
         "attributes":[
            {
               "name":"id",
               "value":"1234",
                "state":"Active"
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
```
