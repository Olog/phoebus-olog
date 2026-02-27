# Log Entry

An minimum Olog logbook entry consists of:

:Author: The author of the entry.
   Typically provided automatically by the authentication provider
:Title: Title of the entry
:Content: Text of the entry. Supports {doc}`../guides/commonmark-cheatsheet`
:Logbook: At least one logbook.
   Multiple logbooks can be seleced.
   See below for more details
:Level: A level for the entry has to be seleced.

In addition, attachments, tags and properties can be added.

## Logbooks

Logbooks can be considered as the **group** level and are often related to organisational groups.

Olog entries support *multiple* logbooks,
which is useful if an entry affects multiple groups.

## Level

A level has to be selected for each entry.
The list of available levels are pre-defined for the Olog instance.

The level can be considered as the **severity** of an entry.

## Tags

You can add multiple tags to each entry.
The list of available tags is pre-defined for the Olog instance.

Tags can be considered as the **system**  or **component** level of an entry.

## Properties

Properties represnt additional parameters which can be provided to each entry.
The list of available properties is pre-defined for the Olog instance.
Each property consists of a **list** of key-value pairs.
If a property is added, the values can be filled out by the user.

Properties represent common information which are regulary added to entries.
This data can be used to integrate with other services or capture context information.

Example: The property `ticket` could be used to link a log entry to an issue ticket:

- Property name: `ticket`
  - Attribute 1: `id` - *Ticket ID provided by the author*
  - Attribute 2: `url` - *Direkt link to the issue provided by the author*

## Attachments

Each log entry can have a list attachments, these can be any type of files.
By default, the attachment size is limited to `15MB` per attachment and `50MB` per log entry, but this can be configured.

## Events

**Events have been deprecated**

There are instances when the log entry being created is actually associated with an event that happened some time ago.
The users had higher priority tasks to address at that moment and is able to log the event after those tasks.
The using **events** allows users to associate log entries with different instances in time,
time based searches will ensure that these log entries are also found even if the create time might not fall in the search range.
