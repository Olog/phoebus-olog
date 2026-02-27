# Olog Markup Guide

Markup support in Olog is based on the [Commonmark
specification](https://commonmark.org/). Olog supports additional markup
features: image size and tables.

In contrast to other markup implementations Olog does not support HTML
in the source. HTML tags entered by user will be rendered as plain text.

The below list is a quick reference for the supported markup features.
However, there are a few things to keep in mind when editing a log entry
text:

1.  To begin a new line you need two blank characters at the end of the
    preceding line. **Or** an empty blank line, which will be rendered
    as a blank line.
2.  Markup for images can be created in either of the following manners:
    - Using a known and valid absolute URL:
      `![alt-text](http://foo.com/image.jpg)` \
      File URLs are not supported.
    - By letting the editor create the markup using the dedicated tool
      (Embed Image). The resulting markup must not be changed.

---

:Type: `*Italic*` or `_Italic_`
:To get: *Italic*

---

:Type: `**Bold**` or `__Bold__`
:To get: **Bold**

---

:Type:
  ```markdown
  # Heading 1
  ```
:Or:
  ```markdown
  Heading 1
  =========
  ```
:To get:
  :::{rubric} Heading 1
  :heading-level: 1
  :::

---

:Type:
  ```markdown
  ## Heading 2
  ```
:Or:
  ```markdown
  Heading 2
  ---------
  ```
:To get:
  :::{rubric} Heading 2
  :heading-level: 2
  :::

---

:Type:
  ```markdown
  [Link](http://example.org)
  ```
:Or:
  ```markdown
  [Link][1]
  ⋮
  [1]: http://example.org
  ```
:To get:
  [Link](http://example.org)

---

:Type:
  ```markdown
  ![alt-text](http://example.org/image.png){width=100 height=150}
  ```

  :::{note}
  The `{width=100 height=150}` size specification is optional, \
  as is the alt-text.
  The square brackets are mandatory.
  :::

  :::{note}
  No blank character between closing parenthesis \
  and opening curly bracket.
  :::
:Or:
  ```markdown
  ![alt-text]
  ⋮
  [1]: http://example.org/image.png
  ```

  :::{note}
  This variant does not support \
  size specification.
  :::
:To get:
  ![alt-text](image.jpg)

---

:Type:
  ```markdown
  > Block quote
  ```
:To get:
  > Block quote

---

:Type:
  ```markdown
  * List
  * List
  * List
  ```
:Or:
  ```markdown
  - List
  - List
  - List
  ```
:To get:
  - List
  - List
  - List

---

:Type:
  ```markdown
  Horizontal rule

  ---

  Note the blank line
  before and after the line
  ```
:To get:
  Horizontal rule

  ---

  Note the blank line
  before and after the line

---

:Type:
  ```markdown
  `Inline code` with backticks
  ```
:To get:
  `Inline code` with backticks

---

:Type:
  ``````markdown
  ```python
  # code block
  print('3 backticks or')
  print('indent 4 spaces')
  ```
  ``````
:Or:
  ```markdown
  ····# code block
  ····print('3 backticks or')
  ····print('indent 4 spaces')
  ```
:To get:
  ```python
  # code block
  print('3 backticks or')
  print('indent 4 spaces')
  ```

---

:Type:
  ```markdown
  | Table Header 1| Table Header 2|
  |---------------|---------------|
  | Table Cell 11 | Table Cell 12 |
  | Table Cell 21 | Table Cell 22 |
  ```
:To get:
  | Table Header 1| Table Header 2|
  |---------------|---------------|
  | Table Cell 11 | Table Cell 12 |
  | Table Cell 21 | Table Cell 22 |
