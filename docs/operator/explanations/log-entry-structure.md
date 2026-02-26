# Log entry structure

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
```

## LogEntry

The most important filed of any log entry are the description and owner.

:description: Is the text body of a log entry.
:owner: The creator of the log entry.
level: Info, Warning, etc.

Other fields include:

:source: When markup support is enabled the description text + the markup annotations are stored in this field.
:id: The unique identifier for a log entry, this is automatically generated at the time of creation.
:createTime: Another automatically generated field which stores the unix epoch millisecond at which the log entry was created.

## Attachment

Each log entry can have a list attachments, these can be any type of files.

## Logbooks & Tags

Logbooks and Tags are useful organization tools. Each log entry can be associated with one or more logbooks and have zero or more tags.

## Properties

A property is a list of key value pairs. The provide means of attaching meta data to a log entry,
this data can be used to integrate with other services or capture context information.

Some examples include

A property to link log entries to Tickets 

```json
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
```

## events

There are instances when the log entry being created is actually associated with an event that happened some time ago. 
The users had higher priority tasks to address at that moment and is able to log the event after those tasks.
The using **events** allows users to associate log entries with different instances in time,
time based searches will ensure that these log entries are also found even if the create time might not fall in the search range.

