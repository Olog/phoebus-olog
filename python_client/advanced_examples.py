"""
Advanced examples for the Phoebus Olog Python Client

Demonstrates file attachments, templates, and advanced search capabilities.
"""

from olog_client import OlogClient
import json
import tempfile
import os
from datetime import datetime


def create_sample_files():
    """Create some sample files for attachment testing."""
    files = []
    
    # Create a simple text file
    with tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False) as f:
        f.write("This is a sample text file for testing attachments.\n")
        f.write(f"Created at: {datetime.now()}\n")
        files.append(f.name)
    
    # Create a JSON file
    with tempfile.NamedTemporaryFile(mode='w', suffix='.json', delete=False) as f:
        sample_data = {
            "test_data": [1, 2, 3, 4, 5],
            "metadata": {
                "created_by": "python_client",
                "purpose": "attachment_test"
            }
        }
        json.dump(sample_data, f, indent=2)
        files.append(f.name)
    
    return files


def advanced_examples():
    with OlogClient(base_url="http://localhost:8080",
                   client_info="Python Advanced Examples") as client:
        client.set_auth('admin', 'adminPass')
        
        print("=== Advanced Olog Client Examples ===\n")
        
        # 1. Create a log template
        print("1. Creating a log template:")
        try:
            template = client.create_template(
                name="daily-report-template",
                title="Daily Operations Report",
                logbooks=["operations"],
                level=None,
                tags=["daily", "report"],
                properties=[{
                    "name": "report-type", 
                    "attributes": [
                        {"name": "period", "value": "daily", "state": "Active"}
                    ]
                }]
            )
            print(f"  Created template: {template.get('name')}")
            template_id = template.get('id')
        except Exception as e:
            print(f"  Error creating template: {e}")
            template_id = None
        print()
        
        # 2. Create sample files for attachments
        print("2. Creating sample files for attachments:")
        sample_files = create_sample_files()
        for i, file_path in enumerate(sample_files, 1):
            print(f"  File {i}: {os.path.basename(file_path)}")
        print()
        
        # 3. Create a log entry with file attachments
        print("3. Creating log entry with file attachments:")
        try:
            log_with_files = client.create_log_with_files(
                title="Log Entry with Attachments",
                logbooks=["operations"],
                file_paths=sample_files,
                description="This log entry includes file attachments uploaded via Python client.",
                tags=["python-test", "attachments"],
                properties=[{
                    "name": "test-property",
                    "attributes": [
                        {"name": "attachment_count", "value": str(len(sample_files)), "state": "Active"}
                    ]
                }]
            )
            log_with_files_id = log_with_files['id']
            print(f"  Created log with attachments, ID: {log_with_files_id}")
            print(f"  Attachments: {len(log_with_files.get('attachments', []))}")
        except Exception as e:
            print(f"  Error creating log with files: {e}")
            log_with_files_id = None
        print()
        
        # 4. Create a simple log and add attachment later
        print("4. Creating log and adding attachment separately:")
        try:
            simple_log = client.create_log(
                title="Simple Log for Later Attachment",
                logbooks=["operations"],
                description="We'll add an attachment to this log after creation."
            )
            simple_log_id = simple_log['id']
            print(f"  Created simple log, ID: {simple_log_id}")
            
            # Add attachment to existing log
            if sample_files:
                updated_log = client.upload_attachment(
                    log_id=str(simple_log_id),
                    file_path=sample_files[0],
                    description="Added after log creation"
                )
                print(f"  Added attachment to existing log")
        except Exception as e:
            print(f"  Error: {e}")
        print()
        
        # 5. Advanced search examples
        print("5. Advanced search examples:")
        
        # Search by text
        try:
            text_search = client.search_logs(text="Python", size=3)
            print(f"  Text search for 'Python': {text_search.get('hitCount', 0)} results")
        except Exception as e:
            print(f"  Text search error: {e}")
        
        # Search by logbook
        try:
            logbook_search = client.search_logs(logbook="operations", size=5)
            print(f"  Logbook search for 'operations': {logbook_search.get('hitCount', 0)} results")
        except Exception as e:
            print(f"  Logbook search error: {e}")
        
        # Search by tag
        try:
            tag_search = client.search_logs(tag="python-test", size=5)
            print(f"  Tag search for 'python-test': {tag_search.get('hitCount', 0)} results")
        except Exception as e:
            print(f"  Tag search error: {e}")
        print()
        
        # 6. Download attachments
        if log_with_files_id:
            print("6. Downloading attachments:")
            try:
                log_details = client.get_log(str(log_with_files_id))
                attachments = log_details.get('attachments', [])
                
                for attachment in attachments[:2]:  # Download first 2 attachments
                    filename = attachment['filename']
                    download_path = f"/tmp/downloaded_{filename}"
                    
                    content = client.download_attachment(
                        log_id=str(log_with_files_id),
                        attachment_name=filename,
                        save_path=download_path
                    )
                    print(f"  Downloaded {filename} ({len(content)} bytes) to {download_path}")
            except Exception as e:
                print(f"  Download error: {e}")
            print()
        
        # 7. Group logs
        if log_with_files_id and simple_log_id:
            print("7. Grouping log entries:")
            try:
                success = client.group_logs([log_with_files_id, simple_log_id])
                print(f"  Grouping logs: {'Success' if success else 'Failed'}")
            except Exception as e:
                print(f"  Grouping error: {e}")
            print()
        
        # 8. Get templates
        print("8. Listing templates:")
        try:
            templates = client.get_templates()
            print(f"  Found {len(templates)} templates:")
            for template in templates:
                print(f"    - {template.get('name', 'Unnamed')}: {template.get('title', 'No title')}")
        except Exception as e:
            print(f"  Error listing templates: {e}")
        print()
        
        # 9. List levels
        print("9. Available levels:")
        try:
            levels = client.get_levels()
            for level in levels:
                default_marker = " (default)" if level.get('defaultLevel') else ""
                print(f"  - {level['name']}{default_marker}")
        except Exception as e:
            print(f"  Error listing levels: {e}")
        print()
        
        # Cleanup sample files
        print("Cleaning up sample files:")
        for file_path in sample_files:
            try:
                os.unlink(file_path)
                print(f"  Deleted: {os.path.basename(file_path)}")
            except Exception as e:
                print(f"  Error deleting {file_path}: {e}")
        
        print("\n=== Advanced examples completed ===")


if __name__ == "__main__":
    advanced_examples()
