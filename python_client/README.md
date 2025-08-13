# Phoebus Olog Python Client

A comprehensive Python client library for interacting with the Phoebus Olog REST API.

## Features

- ✅ **Complete API Coverage**: Supports all Olog REST endpoints
- ✅ **Log Management**: Create, read, update, search log entries
- ✅ **File Attachments**: Upload and download files with log entries
- ✅ **Metadata Management**: Manage logbooks, tags, properties, and levels
- ✅ **Templates**: Create and use log templates
- ✅ **Search**: Advanced search capabilities with multiple parameters
- ✅ **Error Handling**: Comprehensive error handling and validation
- ✅ **Type Hints**: Full type annotations for better IDE support

## Installation

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Import and use the client:
```python
from olog_client import OlogClient

# Initialize client
client = OlogClient(base_url="http://localhost:8080")

# Get service info
info = client.get_service_info()
print(info)
```

## Quick Start

### Basic Log Operations

```python
from olog_client import OlogClient

with OlogClient() as client:
    # Create a simple log entry
    log = client.create_log(
        title="My First Log Entry",
        logbooks=["operations"],
        description="This is a test log entry.",
        tags=["test", "python"]
    )
    
    # Search for logs
    results = client.search_logs(text="test", size=10)
    
    # Get specific log
    log_details = client.get_log(str(log['id']))
```

### Working with Attachments

```python
# Create log with files
log = client.create_log_with_files(
    title="Log with Attachments",
    logbooks=["operations"],
    file_paths=["data.txt", "plot.png"],
    description="Log entry with attached files"
)

# Add attachment to existing log
client.upload_attachment(
    log_id="123",
    file_path="additional_file.pdf",
    description="Additional analysis"
)

# Download attachment
content = client.download_attachment(
    log_id="123",
    attachment_name="data.txt",
    save_path="./downloaded_data.txt"
)
```

### Managing Logbooks, Tags, and Properties

```python
# Create logbook
logbook = client.create_logbook(
    name="my-logbook",
    owner="username",
    state="Active"
)

# Create tag
tag = client.create_tag(name="urgent", state="Active")

# Create property with attributes
property = client.create_property(
    name="experiment-params",
    owner="scientist",
    attributes=[
        {"name": "temperature", "value": "25.5", "state": "Active"},
        {"name": "pressure", "value": "1.013", "state": "Active"}
    ]
)
```

### Advanced Search

```python
from datetime import datetime, timedelta

# Search with multiple parameters
end_date = datetime.now()
start_date = end_date - timedelta(days=7)

results = client.search_logs(
    text="experiment",
    logbook="operations",
    tag="important",
    owner="scientist",
    from_date=start_date.strftime("%Y-%m-%d"),
    to_date=end_date.strftime("%Y-%m-%d"),
    size=20
)

print(f"Found {results['hitCount']} matching logs")
for log in results['logs']:
    print(f"- {log['title']} (ID: {log['id']})")
```

## API Reference

### OlogClient Class

#### Initialization
```python
client = OlogClient(
    base_url="http://localhost:8080",     # Olog service URL
    client_info="My Python Client",       # Client identification
    verify_ssl=True,                       # SSL verification
    timeout=30                             # Request timeout
)
```

#### Service Information
- `get_service_info()` - Get service status and information
- `get_service_configuration()` - Get service configuration

#### Logbook Management
- `get_logbooks()` - List all logbooks
- `get_logbook(name)` - Get specific logbook
- `create_logbook(name, owner, state)` - Create new logbook
- `update_logbooks(logbooks)` - Update multiple logbooks
- `delete_logbook(name)` - Delete logbook

#### Tag Management
- `get_tags()` - List all tags
- `get_tag(name)` - Get specific tag
- `create_tag(name, state)` - Create new tag
- `update_tags(tags)` - Update multiple tags
- `delete_tag(name)` - Delete tag

#### Property Management
- `get_properties(inactive=False)` - List all properties
- `get_property(name)` - Get specific property
- `create_property(name, owner, attributes, state)` - Create new property
- `update_properties(properties)` - Update multiple properties
- `delete_property(name)` - Delete property

#### Level Management
- `get_levels()` - List all levels
- `get_level(name)` - Get specific level
- `create_level(name, default_level)` - Create new level
- `create_levels(levels)` - Create multiple levels
- `delete_level(name)` - Delete level

#### Log Entry Management
- `search_logs(**params)` - Search logs with parameters
- `get_log(log_id)` - Get specific log entry
- `get_archived_log(log_id)` - Get archived log entry
- `create_log(title, logbooks, ...)` - Create new log entry
- `create_log_with_files(title, logbooks, file_paths, ...)` - Create log with attachments
- `update_log(log_id, ...)` - Update existing log entry
- `group_logs(log_ids)` - Group multiple log entries

#### Attachment Management
- `upload_attachment(log_id, file_path, description)` - Upload single file
- `upload_multiple_attachments(log_id, file_paths)` - Upload multiple files
- `download_attachment(log_id, attachment_name, save_path)` - Download attachment
- `download_attachment_by_id(attachment_id, save_path)` - Download by ID

#### Template Management
- `get_templates()` - List all templates
- `get_template(template_id)` - Get specific template
- `create_template(name, title, logbooks, ...)` - Create new template
- `delete_template(template_id)` - Delete template

### Search Parameters

The `search_logs()` method supports these parameters:

- `start` - Start index for pagination
- `size` - Number of results to return
- `from_date` - Start date (YYYY-MM-DD format)
- `to_date` - End date (YYYY-MM-DD format)
- `text` - Text search in title and description
- `logbook` - Filter by logbook name
- `tag` - Filter by tag name
- `owner` - Filter by owner
- `level` - Filter by level
- `property` - Filter by property name

## Examples

See the example files:

- `examples.py` - Basic usage examples
- `advanced_examples.py` - Advanced features including file attachments and templates

Run examples:
```bash
python examples.py
python advanced_examples.py
```

## Error Handling

The client includes comprehensive error handling:

```python
try:
    log = client.create_log(title="Test", logbooks=["nonexistent"])
except Exception as e:
    print(f"Error creating log: {e}")
```

## Context Manager

Use the client as a context manager for automatic cleanup:

```python
with OlogClient() as client:
    # Client operations here
    logs = client.search_logs(size=10)
# Session automatically closed
```

## Requirements

- Python 3.7+
- requests >= 2.28.0
- Running Phoebus Olog service

## License

This client follows the same license as the Phoebus project.
