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
            "owner":"testOwner1"
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

Quick Start
############


REST API 
#########

Creating a Log Entry
***********************

Searching for Log Entries
**************************

Managing Logbooks & Tags
************************

Managing Properties
*******************





Create a log entry 

Installation Instructions:
`Install Olog-es <http://https://github.com/shroffk/olog-es/>`_.

`Javadocs <apidocs/index.html>`_