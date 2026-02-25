# Phoebus Olog documentation

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

Limitations:

 - While attachments can be of any type (images, videos, pdfs...), HEIC/HEIF images cannot be uploaded as this
   format is not supported in all web browsers, and not supported in the Phoebus Olog client. This format is the default
   image format used by the default iOS camera app.

```{toctree}
:caption: Operator
:maxdepth: 3
:glob:

operator/guides/index
operator/explanations/index
```

```{toctree}
:caption: System admin
:maxdepth: 3
:glob:

sysadmin/guides/index
```

```{toctree}
:caption: Developer
:maxdepth: 3
:glob:

developer/references/index
```

```{toctree}
:caption: Contributor
:maxdepth: 3
:glob:

contributor/guides/index
contributor/explanations/index
contributor/references/index
```

```{toctree}
:titlesonly:
:caption: Resources

changelog
Source code repository <https://github.com/Olog/phoebus-olog>
```

