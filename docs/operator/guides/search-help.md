# Searching Logbook Entries

This document details the search capabilities supported by the Olog
logbook service. Users are encouraged to familiarize themselves with
this content in order to be able to construct efficient queries.

The query as edited in the logbook clients consists of a list of
key/value pairs separated by an ampersand (&amp;) character. Keys
identify the various elements - i.e. text and meta-data - of a log
entry. Key names are **case-insensitive**.

## How to use search parameter keys

### start and end

These are used to search for log entries created in the date/time range
start - end. There are several ways to specify a date/time value:

- Date/time on any of the following formats:
  - `yyyy-MM-dd HH:mm:ss.SSS`
  - `yyyy-MM-dd HH:mm:ss`
  - `yyyy-MM-dd HH:mm`
  - `yyyy-MM-dd` - the time portion will be set to 00:00:00
  - `HH:mm:ss` - the date portion will be set to current date
- A relative time specifier using any of the following
  **case-insensitive** formats:
  - `X w` - i.e. X weeks
  - `X d` - i.e. X days
  - `X h` - i.e. X hours
  - `X m` - i.e. X minutes
  - `X s` - i.e. X seconds
  - `now` - i.e. the current date/time

  For the sake of clarity user may specify the full wording, e.g. weeks,
  days etc. It is also possible to combine w(eeks) and d(ays), e.g.
  `10 w 4 d`, but any additional elements will be ignored.

If a time specifier cannot be parsed, or if it is omitted, the start
date/time defaults to `1970-01-01 00:00:00`, while the end date/time
defaults to `now`.  
An invalid time range - e.g. start date/time after end date/time - will
trigger an error. In such cases no search is performed, i.e. the service
will not fall back to a default time range.  
To help user compose a valid date/time string for a time range search,
the advanced search view of the clients offers a date/time picker tool.

:::{note}
An absolute time specifier is ambiguous if within the hour
when a time zone switches from daylight saving time (DST). For instance,
if:

- The switch takes place at 03:00, setting back the time to 02:00
- User specifies start time 02:30
- Search is performed *after* switch away from DST

then the search result will contain entries created after 01:30 in the
non-DST time. If on the other hand the 02:30 timestamp is specified as
end time, the search result will *not* contain entries after 01:30 in
the non-DST time.
:::

### owner

This is the author element of the log entry. The value for this key is
**case-insensitive**, and wildcards can be used, e.g. `owner=John*`.  

### title

This is the title of the log entry. Semantics for this is same as the
`desc` key, see below.

### desc

This is the body of the log entry. A body text is not mandatory for a
log entry, i.e. it may be empty. The value for this key is
**case-insensitive**.  
Wildcards need not be used for a substring match, e.g. `desc=increased`
will match a log entry with the body text
`Temperature increased in tank X`.  
In some cases one may need to quote a string in order to get the wanted
search result. For instance, to search for a string like `foo-bar` one
must use `desc="foo-bar"`. Mixing quoted and non-quoted values is
supported, e.g. `desc="foo-bar",other`.

To enclose a term in quotes, one *must* use double the double quote
character (").

Limitations when using quoted search terms:

- Performance may be affected, i.e. the search process may take longer
  time to complete.
- Search will fail if quote characters are not balanced, i.e. search
  terms string must contain even number of quote characters.
- No leading or trailing characters other than a comma separator may
  appear together with a quoted term. For instance, `foo,"bar",other` is
  valid, but `foo,"bar"*,other` is not.
- Searching for occurrence of quote characters is not possible.

:::{note}
Log entries with an empty body will **not** match queries
containing the `desc` key. In other words, to match all log entries
irrespective of body text length, the `desc` key must not be present in
the query.
:::

### logbooks

This is the logbooks meta-data of a log entry. A log entry is contained
in one or multiple logbooks. The value for this key is
**case-insensitive**, and wildcards can be used, e.g.
`logbooks=operation*`.  

### tags

This is the tags meta-data of a log entry. A log entry may contain zero
or multiple tags. The value for this key is **case-insensitive**, and
wildcards can be used, e.g. `tags=cavity*`.  

### properties

This is the properties meta-data of a log entry. A log entry may contain
zero or multiple properties.  
A property is a named list of attributes, where each attribute is a
key/value pair. Consequently, the value for this search key must be
specified using a dot (.) separated string corresponding to the
structure of a property like so:  
`<property name>.<attribute name>.<attribute value>`  
Example: assume some log entries contain a property named `Shift Info`,
which in turn contains an attribute named `Shift Lead`. A search query
for log entries where the `Shift Lead` is `John Doe` would then use:
`properties=Shift Info.Shift Lead.John Doe`. All three, dot-separated,
elements of the value string for the properties key are
*case-insensitive*, and wildcards can be used.  
Not all elements need to be specified. A query like
`properties=Shift Info` will match all log entries containing such a
property, irrespective of the attribute list it contains.  

### attachments

Search related to attachments supports two distinct use cases:

1.  Search for log entries containing at least one attachment,
    irrespective of type. This is done by omitting a value, i.e. by
    adding only the keyword `attachments` to the query.
2.  Search for log entries where attachments match on file name. This is
    done by adding `attachments=some_file_name`. Here `some_file_name`
    is case-insensitive, but must otherwise match exactly. Wildcards can
    be used, e.g. `attachments=*_file*`. To search for attachments with
    a particular file name extension, one may use something like
    `attachments=*plt` or `attachments=*xls*`. As image file name
    extensions vary by type one may need to specify multiple extensions,
    e.g. `attachments=*png,*jpeg`.

:::{note}
Since the space character is treated in a special manner by
the underlying search engine, a search for file name cannot contain
space characters. For instance, to find entries with an attachment file
name like "proton electron neutron", one should instead use
`attachments=proton*` or `attachments=*electron*`.
:::

:::{note}
Search for occurrence of text *within* an attachment is not supported.
:::

### Multiple keys

If multiple keys are used in a search query, the service will consider
all (valid) keys and return log entries matching *all* criteria. In
other words, the search keys are and:ed.

### Multiple values

One may specify multiple values for a key using a comma (,) separator.
This will then find entries matching *any* of the values. However, for
the `desc` and `title` keys the search will instead apply an "and"
strategy, i.e. only entries containing *all* the values will match the
search query.

Examples:

- `user=John*,Jane*` will match entries created by users named "John"
  *or* "Jane".
- `desc=magnet,current` will match entries containing "magnet" *and*
  "current".
- `user=John*,Jane*&desc=magnet,current` will find entries created by
  users named "John" *or* "Jane", *and* that contain both "magnet" *and*
  "current".
