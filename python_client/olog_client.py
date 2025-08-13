"""
Phoebus Olog Python Client

A comprehensive Python client for interacting with the Phoebus Olog REST API.
Supports all CRUD operations for logs, logbooks, tags, properties, levels, and templates.
"""

import requests
import json
from typing import List, Dict, Optional, Any, Union
from datetime import datetime
import mimetypes
import os
from pathlib import Path


class OlogClient:
    """
    Python client for Phoebus Olog service.
    
    Provides methods to interact with all Olog API endpoints including:
    - Log entries (create, read, update, search)
    - Logbooks management
    - Tags management  
    - Properties management
    - Levels management
    - Log templates
    - File attachments
    """
    
    def __init__(self, base_url: str = "http://localhost:8080", 
                 client_info: str = "Python Olog Client",
                 verify_ssl: bool = True,
                 timeout: int = 30):
        """
        Initialize the Olog client.
        
        Args:
            base_url: Base URL of the Olog service (default: http://localhost:8080)
            client_info: Client identification string
            verify_ssl: Whether to verify SSL certificates
            timeout: Request timeout in seconds
        """
        self.base_url = base_url.rstrip('/')
        self.client_info = client_info
        self.verify_ssl = verify_ssl
        self.timeout = timeout
        self.session = requests.Session()
        
        # Set default headers
        self.session.headers.update({
            'Content-Type': 'application/json',
            'X-Olog-Client-Info': client_info
        })
        # Optionally set Basic Auth credentials
        self.session.auth = None

    def set_auth(self, username: str, password: str):
        """Set Basic Auth credentials for the session."""
        self.session.auth = (username, password)
    
    def _make_request(self, method: str, endpoint: str, **kwargs) -> requests.Response:
        """Make HTTP request with error handling."""
        url = f"{self.base_url}{endpoint}"
        
        # Set default request parameters
        kwargs.setdefault('verify', self.verify_ssl)
        kwargs.setdefault('timeout', self.timeout)
        
        try:
            response = self.session.request(method, url, **kwargs)
            response.raise_for_status()
            return response
        except requests.exceptions.RequestException as e:
            raise Exception(f"Request failed: {e}")
    
    def _get_json(self, endpoint: str, **kwargs) -> Any:
        """GET request returning JSON data."""
        response = self._make_request('GET', endpoint, **kwargs)
        return response.json() if response.content else None
    
    def _post_json(self, endpoint: str, data: Any = None, **kwargs) -> Any:
        """POST request with JSON data."""
        if data is not None:
            kwargs['json'] = data
        response = self._make_request('POST', endpoint, **kwargs)
        return response.json() if response.content else None
    
    def _put_json(self, endpoint: str, data: Any = None, **kwargs) -> Any:
        """PUT request with JSON data."""
        if data is not None:
            kwargs['json'] = data
        response = self._make_request('PUT', endpoint, **kwargs)
        return response.json() if response.content else None
    
    def _delete(self, endpoint: str, **kwargs) -> bool:
        """DELETE request."""
        response = self._make_request('DELETE', endpoint, **kwargs)
        return response.status_code == 200

    # Service Information
    def get_service_info(self) -> Dict[str, Any]:
        """Get service information and health status."""
        return self._get_json('/Olog')
    
    def get_service_configuration(self) -> Dict[str, Any]:
        """Get service configuration."""
        return self._get_json('/Olog/configuration')

    # Logbooks Management
    def get_logbooks(self) -> List[Dict[str, Any]]:
        """Get all logbooks."""
        return self._get_json('/Olog/logbooks')
    
    def get_logbook(self, logbook_name: str) -> Dict[str, Any]:
        """Get specific logbook by name."""
        return self._get_json(f'/Olog/logbooks/{logbook_name}')
    
    def create_logbook(self, name: str, owner: str = None, state: str = "Active") -> Dict[str, Any]:
        """
        Create a new logbook.
        
        Args:
            name: Logbook name
            owner: Owner of the logbook
            state: State (Active/Inactive)
        """
        logbook_data = {
            "name": name,
            "owner": owner,
            "state": state
        }
        return self._put_json(f'/Olog/logbooks/{name}', logbook_data)
    
    def update_logbooks(self, logbooks: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Update multiple logbooks."""
        return self._put_json('/Olog/logbooks', logbooks)
    
    def delete_logbook(self, logbook_name: str) -> bool:
        """Delete a logbook."""
        return self._delete(f'/Olog/logbooks/{logbook_name}')

    # Tags Management
    def get_tags(self) -> List[Dict[str, Any]]:
        """Get all tags."""
        return self._get_json('/Olog/tags')
    
    def get_tag(self, tag_name: str) -> Dict[str, Any]:
        """Get specific tag by name."""
        return self._get_json(f'/Olog/tags/{tag_name}')
    
    def create_tag(self, name: str, state: str = "Active") -> Dict[str, Any]:
        """
        Create a new tag.
        
        Args:
            name: Tag name
            state: State (Active/Inactive)
        """
        tag_data = {
            "name": name,
            "state": state
        }
        return self._put_json(f'/Olog/tags/{name}', tag_data)
    
    def update_tags(self, tags: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Update multiple tags."""
        return self._put_json('/Olog/tags', tags)
    
    def delete_tag(self, tag_name: str) -> bool:
        """Delete a tag."""
        return self._delete(f'/Olog/tags/{tag_name}')

    # Properties Management
    def get_properties(self, inactive: bool = False) -> List[Dict[str, Any]]:
        """
        Get all properties.
        
        Args:
            inactive: Include inactive properties
        """
        params = {'inactive': inactive} if inactive else {}
        return self._get_json('/Olog/properties', params=params)
    
    def get_property(self, property_name: str) -> Dict[str, Any]:
        """Get specific property by name."""
        return self._get_json(f'/Olog/properties/{property_name}')
    
    def create_property(self, name: str, owner: str = None, 
                       attributes: List[Dict[str, str]] = None, 
                       state: str = "Active") -> Dict[str, Any]:
        """
        Create a new property.
        
        Args:
            name: Property name
            owner: Property owner
            attributes: List of attributes with name, value, state
            state: State (Active/Inactive)
        """
        property_data = {
            "name": name,
            "owner": owner,
            "state": state,
            "attributes": attributes or []
        }
        return self._put_json(f'/Olog/properties/{name}', property_data)
    
    def update_properties(self, properties: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Update multiple properties."""
        return self._put_json('/Olog/properties', properties)
    
    def delete_property(self, property_name: str) -> bool:
        """Delete a property."""
        return self._delete(f'/Olog/properties/{property_name}')

    # Levels Management
    def get_levels(self) -> List[Dict[str, Any]]:
        """Get all levels."""
        return self._get_json('/Olog/levels')
    
    def get_level(self, level_name: str) -> Dict[str, Any]:
        """Get specific level by name."""
        return self._get_json(f'/Olog/levels/{level_name}')
    
    def create_level(self, name: str, default_level: bool = False) -> Dict[str, Any]:
        """
        Create a new level.
        
        Args:
            name: Level name
            default_level: Whether this is the default level
        """
        level_data = {
            "name": name,
            "defaultLevel": default_level
        }
        return self._put_json(f'/Olog/levels/{name}', level_data)
    
    def create_levels(self, levels: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Create multiple levels."""
        return self._put_json('/Olog/levels', levels)
    
    def delete_level(self, level_name: str) -> bool:
        """Delete a level."""
        return self._delete(f'/Olog/levels/{level_name}')

    # Log Templates Management
    def get_templates(self) -> List[Dict[str, Any]]:
        """Get all log templates."""
        return self._get_json('/Olog/templates')
    
    def get_template(self, template_id: str) -> Dict[str, Any]:
        """Get specific template by ID."""
        return self._get_json(f'/Olog/templates/{template_id}')
    
    def create_template(self, name: str, title: str, logbooks: List[str],
                       source: str = None, level: str = None,
                       tags: List[str] = None, 
                       properties: List[Dict[str, Any]] = None) -> Dict[str, Any]:
        """
        Create a new log template.
        
        Args:
            name: Template name
            title: Template title
            logbooks: List of logbook names
            source: Template source
            level: Level name
            tags: List of tag names
            properties: List of properties
        """
        template_data = {
            "name": name,
            "title": title,
            "logbooks": [{"name": lb} for lb in logbooks],
            "source": source,
            "level": level,
            "tags": [{"name": tag} for tag in (tags or [])],
            "properties": properties or []
        }
        return self._put_json('/Olog/templates', template_data)
    
    def delete_template(self, template_id: str) -> bool:
        """Delete a template."""
        return self._delete(f'/Olog/templates/{template_id}')

    # Log Entries Management
    def search_logs(self, **search_params) -> Dict[str, Any]:
        """
        Search log entries with various parameters.
        
        Common search parameters:
            start: Start index for pagination
            size: Number of results to return
            from: Start date (YYYY-MM-DD)
            to: End date (YYYY-MM-DD)
            text: Text search
            logbook: Logbook name
            tag: Tag name
            owner: Owner name
            level: Level name
        """
        # Handle common parameter name variations
        if 'from_date' in search_params:
            search_params['from'] = search_params.pop('from_date')
        if 'to_date' in search_params:
            search_params['to'] = search_params.pop('to_date')
        
        return self._get_json('/Olog/logs/search', params=search_params)
    
    def get_log(self, log_id: str) -> Dict[str, Any]:
        """Get specific log entry by ID."""
        return self._get_json(f'/Olog/logs/{log_id}')
    
    def get_archived_log(self, log_id: str) -> Dict[str, Any]:
        """Get archived log entry by ID."""
        return self._get_json(f'/Olog/logs/archived/{log_id}')
    
    def create_log(self, title: str, logbooks: List[str], 
                   description: str = "", level: str = None,
                   tags: List[str] = None, properties: List[Dict[str, Any]] = None,
                   markup: str = None, in_reply_to: str = "-1") -> Dict[str, Any]:
        """
        Create a new log entry.
        
        Args:
            title: Log title (required)
            logbooks: List of logbook names (required)
            description: Log description
            level: Level name
            tags: List of tag names
            properties: List of properties
            markup: Markup type for description
            in_reply_to: ID of log this is replying to
        """
        log_data = {
            "title": title,
            "description": description,
            "logbooks": [{"name": lb} for lb in logbooks],
            "level": level,
            "tags": [{"name": tag} for tag in (tags or [])],
            "properties": properties or []
        }
        
        params = {}
        if markup:
            params['markup'] = markup
        if in_reply_to != "-1":
            params['inReplyTo'] = in_reply_to
            
        return self._put_json('/Olog/logs', log_data, params=params)
    
    def create_log_with_files(self, title: str, logbooks: List[str],
                             file_paths: List[str], description: str = "",
                             level: str = None, tags: List[str] = None,
                             properties: List[Dict[str, Any]] = None,
                             markup: str = None, in_reply_to: str = "-1") -> Dict[str, Any]:
        """
        Create a new log entry with file attachments.
        
        Args:
            title: Log title (required)
            logbooks: List of logbook names (required)  
            file_paths: List of file paths to attach
            description: Log description
            level: Level name
            tags: List of tag names
            properties: List of properties
            markup: Markup type for description
            in_reply_to: ID of log this is replying to
        """
        log_data = {
            "title": title,
            "description": description,
            "logbooks": [{"name": lb} for lb in logbooks],
            "level": level,
            "tags": [{"name": tag} for tag in (tags or [])],
            "properties": properties or []
        }
        
        # Prepare multipart data
        files = []
        for file_path in file_paths:
            if os.path.exists(file_path):
                filename = os.path.basename(file_path)
                mime_type = mimetypes.guess_type(file_path)[0] or 'application/octet-stream'
                files.append(('files', (filename, open(file_path, 'rb'), mime_type)))
        
        params = {}
        if markup:
            params['markup'] = markup
        if in_reply_to != "-1":
            params['inReplyTo'] = in_reply_to
        
        # For multipart, we need to handle the request differently
        multipart_data = {
            'logEntry': (None, json.dumps(log_data), 'application/json')
        }
        multipart_data.update(dict(files))
        
        # Remove Content-Type header for multipart
        headers = dict(self.session.headers)
        if 'Content-Type' in headers:
            del headers['Content-Type']
        
        response = self._make_request('PUT', '/Olog/logs/multipart', 
                                    files=multipart_data, params=params, 
                                    headers=headers)
        
        # Close file handles
        for _, file_tuple in files:
            file_tuple[1].close()
        
        return response.json() if response.content else None
    
    def update_log(self, log_id: str, title: str = None, description: str = None,
                   level: str = None, tags: List[str] = None,
                   properties: List[Dict[str, Any]] = None,
                   markup: str = None) -> Dict[str, Any]:
        """
        Update an existing log entry.
        
        Args:
            log_id: ID of log to update
            title: New title
            description: New description
            level: New level
            tags: New tags list
            properties: New properties list
            markup: Markup type
        """
        # Get current log to preserve existing data
        current_log = self.get_log(log_id)
        
        # Update only provided fields
        if title is not None:
            current_log['title'] = title
        if description is not None:
            current_log['description'] = description
        if level is not None:
            current_log['level'] = level
        if tags is not None:
            current_log['tags'] = [{"name": tag} for tag in tags]
        if properties is not None:
            current_log['properties'] = properties
        
        params = {}
        if markup:
            params['markup'] = markup
            
        return self._post_json(f'/Olog/logs/{log_id}', current_log, params=params)
    
    def group_logs(self, log_ids: List[int]) -> bool:
        """Group multiple log entries together."""
        response = self._make_request('POST', '/Olog/logs/group', json=log_ids)
        return response.status_code == 200

    # Attachment Management
    def upload_attachment(self, log_id: str, file_path: str, 
                         description: str = "") -> Dict[str, Any]:
        """
        Upload a single attachment to an existing log.
        
        Args:
            log_id: ID of the log entry
            file_path: Path to file to upload
            description: File description
        """
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"File not found: {file_path}")
        
        filename = os.path.basename(file_path)
        
        # Use the create_log_with_files approach for existing logs by updating
        # First try simple multipart approach
        try:
            with open(file_path, 'rb') as f:
                files = {'file': (filename, f, 'application/octet-stream')}
                data = {
                    'filename': filename,
                    'fileMetadataDescription': description
                }
                
                # Remove Content-Type header for multipart
                headers = dict(self.session.headers)
                if 'Content-Type' in headers:
                    del headers['Content-Type']
                
                response = self._make_request('POST', f'/Olog/logs/attachments/{log_id}',
                                            files=files, data=data, headers=headers)
                return response.json() if response.content else None
        except Exception as e:
            # If that fails, try alternative multipart format
            with open(file_path, 'rb') as f:
                multipart_data = [
                    ('file', (filename, f, 'application/octet-stream')),
                    ('filename', (None, filename)),
                    ('fileMetadataDescription', (None, description))
                ]
                
                # Remove Content-Type header for multipart
                headers = dict(self.session.headers)
                if 'Content-Type' in headers:
                    del headers['Content-Type']
                
                response = self._make_request('POST', f'/Olog/logs/attachments/{log_id}',
                                            files=multipart_data, headers=headers)
                return response.json() if response.content else None
    
    def upload_multiple_attachments(self, log_id: str, 
                                   file_paths: List[str]) -> Dict[str, Any]:
        """
        Upload multiple attachments to an existing log.
        
        Args:
            log_id: ID of the log entry
            file_paths: List of file paths to upload
        """
        files = []
        for file_path in file_paths:
            if os.path.exists(file_path):
                filename = os.path.basename(file_path)
                mime_type = mimetypes.guess_type(file_path)[0] or 'application/octet-stream'
                files.append(('file', (filename, open(file_path, 'rb'), mime_type)))
        
        # Remove Content-Type header for multipart
        headers = dict(self.session.headers)
        if 'Content-Type' in headers:
            del headers['Content-Type']
        
        response = self._make_request('POST', f'/Olog/logs/attachments-multi/{log_id}',
                                    files=files, headers=headers)
        
        # Close file handles
        for _, file_tuple in files:
            file_tuple[1].close()
        
        return response.json() if response.content else None
    
    def download_attachment(self, log_id: str, attachment_name: str, 
                           save_path: str = None) -> bytes:
        """
        Download an attachment from a log entry.
        
        Args:
            log_id: ID of the log entry
            attachment_name: Name of the attachment
            save_path: Optional path to save the file
        """
        response = self._make_request('GET', 
                                    f'/Olog/logs/attachments/{log_id}/{attachment_name}')
        
        if save_path:
            Path(save_path).parent.mkdir(parents=True, exist_ok=True)
            with open(save_path, 'wb') as f:
                f.write(response.content)
        
        return response.content
    
    def download_attachment_by_id(self, attachment_id: str, 
                                 save_path: str = None) -> bytes:
        """
        Download an attachment by its ID.
        
        Args:
            attachment_id: ID of the attachment
            save_path: Optional path to save the file
        """
        response = self._make_request('GET', f'/Olog/attachment/{attachment_id}')
        
        if save_path:
            Path(save_path).parent.mkdir(parents=True, exist_ok=True)
            with open(save_path, 'wb') as f:
                f.write(response.content)
        
        return response.content

    # Help and Documentation
    def get_help(self, topic: str, language: str = "en") -> str:
        """
        Get help documentation for a specific topic.
        
        Args:
            topic: Help topic
            language: Language code (default: en)
        """
        params = {'lang': language} if language != "en" else {}
        response = self._make_request('GET', f'/Olog/help/{topic}', params=params)
        return response.text

    # Utility Methods
    def close(self):
        """Close the session."""
        self.session.close()
    
    def __enter__(self):
        """Context manager entry."""
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.close()
