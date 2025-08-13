# Phoebus Olog Python Client

A comprehensive Python client for interacting with the Phoebus Olog REST API. This client provides full access to all Olog service endpoints with proper authentication and error handling.

## Features

- **Complete API Coverage**: Supports all major Olog endpoints
- **Authentication**: Basic Auth support
- **Error Handling**: Proper exception handling with meaningful error messages
- **Type Hints**: Full type annotations for better IDE support
- **Context Manager**: Automatic session cleanup
- **File Operations**: Upload and download attachments (when server supports it)

## Installation

```bash
# Install required dependencies
pip install requests

# Or use the provided requirements.txt
pip install -r requirements.txt
```

## Quick Start

```python
from olog_client import OlogClient

# Initialize the client
with OlogClient(base_url="http://localhost:8080") as client:
    # Set authentication
    client.set_auth('admin', 'adminPass')
    
    # Get service information
    info = client.get_service_info()
    print(f"Olog Service: {info['name']} v{info['version']}")
    
    # Create a log entry
    log = client.create_log(
        title="Test Log Entry",
        logbooks=["operations"],
        description="Created from Python client"
    )
    print(f"Created log entry: {log['id']}")
```

## API Coverage

### ✅ Fully Working Endpoints

#### Service Information
- `get_service_info()` - Get service status and version
- `get_service_configuration()` - Get service configuration

#### Logbooks Management
- `get_logbooks()` - List all logbooks
- `get_logbook(name)` - Get specific logbook
- `create_logbook(name, owner, state)` - Create new logbook
- `update_logbooks(logbooks)` - Update multiple logbooks
- `delete_logbook(name)` - Delete logbook

#### Tags Management
- `get_tags()` - List all tags
- `get_tag(name)` - Get specific tag
- `create_tag(name, state)` - Create new tag
- `update_tags(tags)` - Update multiple tags
- `delete_tag(name)` - Delete tag

#### Properties Management
- `get_properties(inactive)` - List all properties
- `get_property(name)` - Get specific property
- `create_property(name, owner, attributes, state)` - Create new property
- `update_properties(properties)` - Update multiple properties
- `delete_property(name)` - Delete property

#### Levels Management
- `get_levels()` - List all levels
- `get_level(name)` - Get specific level
- `create_level(name, default_level)` - Create new level
- `create_levels(levels)` - Create multiple levels
- `delete_level(name)` - Delete level

#### Log Templates
- `get_templates()` - List all templates
- `get_template(id)` - Get specific template
- `create_template(name, title, logbooks, ...)` - Create new template
- `delete_template(id)` - Delete template

#### Log Entries
- `search_logs(**params)` - Search logs with various parameters
- `get_log(id)` - Get specific log entry
- `get_archived_log(id)` - Get archived log entry
- `create_log(title, logbooks, ...)` - Create new log entry
- `update_log(id, ...)` - Update existing log entry
- `group_logs(log_ids)` - Group multiple log entries

### ⚠️ Known Limitations

#### File Attachments
- `upload_attachment()` - Returns 500 Server Error (server-side issue)
- `create_log_with_files()` - Returns 500 Server Error (server-side issue)
- `download_attachment()` - Methods implemented but untested due to upload issues

#### Help System
- `get_help()` - No valid help topics found on test server

## Examples

### Basic Operations

```python
from olog_client import OlogClient

with OlogClient("http://localhost:8080") as client:
    client.set_auth('admin', 'adminPass')
    
    # Create resources
    logbook = client.create_logbook("my-logbook", owner="me")
    tag = client.create_tag("important")
    
    # Create log entry
    log = client.create_log(
        title="System Status Update",
        logbooks=["my-logbook"],
        description="All systems operational",
        tags=["important"],
        level="Info"
    )
    
    # Search logs
    results = client.search_logs(
        text="system",
        logbook="my-logbook",
        size=10
    )
    print(f"Found {results['hitCount']} logs")
```

### Advanced Search

```python
# Search by various parameters
recent_logs = client.search_logs(
    size=20,
    from_date="2024-01-01",
    to_date="2024-12-31",
    text="error",
    logbook="operations",
    level="Problem"
)

# Search by owner
my_logs = client.search_logs(owner="admin", size=50)

# Search by tags
tagged_logs = client.search_logs(tag="maintenance")
```

### Resource Management

```python
# Create property with attributes
property_data = client.create_property(
    name="device-status",
    owner="control-system",
    attributes=[
        {"name": "device_id", "value": "DEV001", "state": "Active"},
        {"name": "location", "value": "Building A", "state": "Active"}
    ]
)

# Create template for recurring log types
template = client.create_template(
    name="daily-report",
    title="Daily Operations Report",
    logbooks=["operations"],
    tags=["daily", "report"],
    level="Info"
)
```

## Authentication

The client supports Basic Authentication:

```python
client.set_auth('username', 'password')
```

## Error Handling

The client raises exceptions for HTTP errors:

```python
try:
    log = client.create_log(title="Test", logbooks=["nonexistent"])
except Exception as e:
    print(f"Error: {e}")
```

## Files

- `olog_client.py` - Main client implementation
- `examples.py` - Basic usage examples
- `advanced_examples.py` - Advanced usage examples  
- `test_all_endpoints.py` - Comprehensive endpoint testing
- `requirements.txt` - Python dependencies

## Testing

Run the comprehensive test suite:

```bash
python test_all_endpoints.py
```

This will test all endpoints and show their current status.

## Contributing

The client is designed to be easily extensible. To add new endpoints:

1. Add the method to the `OlogClient` class
2. Use the existing `_get_json`, `_post_json`, `_put_json`, or `_delete` helper methods
3. Add appropriate type hints and docstrings
4. Add tests to `test_all_endpoints.py`

## License

This project follows the same license as the main Phoebus Olog project.
