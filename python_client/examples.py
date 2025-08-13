"""
Example usage of the Phoebus Olog Python Client

This script demonstrates various operations with the Olog service.
"""

from olog_client import OlogClient
import json
from datetime import datetime, timedelta


def main():
    # Initialize the client
    with OlogClient(base_url="http://localhost:8080", 
                   client_info="Python Example Client") as client:
        client.set_auth('admin', 'adminPass')
        
        print("=== Phoebus Olog Python Client Examples ===\n")
        
        # 1. Get service information
        print("1. Service Information:")
        service_info = client.get_service_info()
        print(json.dumps(service_info, indent=2))
        print()
        
        # 2. List existing logbooks
        print("2. Existing Logbooks:")
        logbooks = client.get_logbooks()
        for logbook in logbooks:
            print(f"  - {logbook['name']} (owner: {logbook.get('owner', 'N/A')})")
        print()
        
        # 3. List existing tags
        print("3. Existing Tags:")
        tags = client.get_tags()
        for tag in tags:
            print(f"  - {tag['name']}")
        print()
        
        # 4. Create a new logbook
        print("4. Creating new logbook 'test-logbook':")
        try:
            new_logbook = client.create_logbook(
                name="test-logbook",
                owner="python-client",
                state="Active"
            )
            print(f"  Created: {new_logbook['name']}")
        except Exception as e:
            print(f"  Error (might already exist): {e}")
        print()
        
        # 5. Create a new tag
        print("5. Creating new tag 'python-test':")
        try:
            new_tag = client.create_tag(name="python-test", state="Active")
            print(f"  Created: {new_tag['name']}")
        except Exception as e:
            print(f"  Error (might already exist): {e}")
        print()
        
        # 6. Create a new property
        print("6. Creating new property 'test-property':")
        try:
            new_property = client.create_property(
                name="test-property",
                owner="python-client",
                attributes=[
                    {"name": "key1", "value": "value1", "state": "Active"},
                    {"name": "key2", "value": "value2", "state": "Active"}
                ]
            )
            print(f"  Created: {new_property['name']}")
        except Exception as e:
            print(f"  Error (might already exist): {e}")
        print()
        
        # 7. Create a log entry
        print("7. Creating a simple log entry:")
        try:
            log_entry = client.create_log(
                title="Test Log Entry from Python Client",
                logbooks=["test-logbook", "operations"],  # Use existing + new logbook
                description="This is a test log entry created using the Python client.",
                level=None,  # Use default level
                tags=["python-test"],
                properties=[{
                    "name": "test-property",
                    "attributes": [
                        {"name": "source", "value": "python-client", "state": "Active"}
                    ]
                }]
            )
            log_id = log_entry['id']
            print(f"  Created log entry with ID: {log_id}")
        except Exception as e:
            print(f"  Error creating log: {e}")
            log_id = None
        print()
        
        # 8. Search for logs
        print("8. Searching for recent logs:")
        try:
            # Search for logs from the last 7 days
            end_date = datetime.now()
            start_date = end_date - timedelta(days=7)
            
            search_results = client.search_logs(
                size=5,  # Limit to 5 results
                from_date=start_date.strftime("%Y-%m-%d"),
                to_date=end_date.strftime("%Y-%m-%d")
            )
            
            print(f"  Found {search_results.get('hitCount', 0)} logs")
            for log in search_results.get('logs', [])[:3]:  # Show first 3
                print(f"    - ID {log['id']}: {log['title']}")
        except Exception as e:
            print(f"  Error searching logs: {e}")
        print()
        
        # 9. Update a log entry (if we created one)
        if log_id:
            print(f"9. Updating log entry {log_id}:")
            try:
                updated_log = client.update_log(
                    log_id=str(log_id),
                    description="This log entry has been updated with additional information.",
                    tags=["python-test", "updated"]
                )
                print(f"  Updated log entry: {updated_log['title']}")
            except Exception as e:
                print(f"  Error updating log: {e}")
            print()
        
        # 10. Get service configuration
        print("10. Service Configuration:")
        try:
            config = client.get_service_configuration()
            print(f"  Available levels: {config.get('levels', [])}")
        except Exception as e:
            print(f"  Error getting configuration: {e}")
        print()
        
        print("=== Examples completed ===")


if __name__ == "__main__":
    main()
