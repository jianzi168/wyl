#!/usr/bin/env python3
"""
Create a new OpenClaw session with specified name.
Usage: python3 scripts/create_session.py "<session-name>"
"""

import sys
import os
import json
import uuid
from datetime import datetime
from pathlib import Path

# Configuration
WORKSPACE_ROOT = Path("/root/.openclaw/workspace")
SESSIONS_DIR = WORKSPACE_ROOT / "sessions"
SESSION_INDEX_FILE = SESSIONS_DIR / "index.json"


def generate_session_id():
    """Generate a unique session ID."""
    return f"session-{str(uuid.uuid4())[:8]}"


def create_session_index():
    """Create session index file if it doesn't exist."""
    if not SESSION_INDEX_FILE.exists():
        SESSION_INDEX_FILE.parent.mkdir(parents=True, exist_ok=True)
        data = {
            "sessions": [],
            "metadata": {
                "version": "1.0",
                "lastUpdated": datetime.now().isoformat()
            }
        }
        with open(SESSION_INDEX_FILE, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        print(f"✓ Created session index: {SESSION_INDEX_FILE}")


def load_session_index():
    """Load session index from file."""
    if not SESSION_INDEX_FILE.exists():
        create_session_index()

    with open(SESSION_INDEX_FILE, 'r', encoding='utf-8') as f:
        return json.load(f)


def save_session_index(data):
    """Save session index to file."""
    data["metadata"]["lastUpdated"] = datetime.now().isoformat()
    with open(SESSION_INDEX_FILE, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)


def check_session_exists(session_name, index_data):
    """Check if a session with the given name already exists."""
    for session in index_data.get("sessions", []):
        if session.get("name") == session_name:
            return session
    return None


def create_session_workspace(session_id, session_name):
    """Create session workspace directory and files."""
    session_dir = SESSIONS_DIR / session_id
    session_dir.mkdir(exist_ok=True)

    # Create MEMORY.md
    memory_file = session_dir / "MEMORY.md"
    with open(memory_file, 'w', encoding='utf-8') as f:
        f.write(f"# {session_name} - Session Memory\n\n")
        f.write(f"**Session ID**: {session_id}\n")
        f.write(f"**Created**: {datetime.now().isoformat()}\n\n")
        f.write("## Recent Activity\n\n")
        f.write("- Session created\n")

    # Create IDENTITY.md
    identity_file = session_dir / "IDENTITY.md"
    with open(identity_file, 'w', encoding='utf-8') as f:
        f.write(f"# IDENTITY.md - Who Am I\n\n")
        f.write(f"**Session Name**: {session_name}\n")
        f.write(f"**Session ID**: {session_id}\n\n")
        f.write("- This is a fresh workspace.\n")

    # Create USER.md
    user_file = session_dir / "USER.md"
    with open(user_file, 'w', encoding='utf-8') as f:
        f.write(f"# USER.md - About Your Human\n\n")
        f.write(f"**Session Name**: {session_name}\n")
        f.write(f"**Session ID**: {session_id}\n\n")
        f.write("- Context will be added here.\n")

    print(f"✓ Created session workspace: {session_dir}")
    print(f"  - MEMORY.md")
    print(f"  - IDENTITY.md")
    print(f"  - USER.md")

    return session_dir


def add_session_to_index(session_id, session_name, workspace_path):
    """Add new session to index."""
    index_data = load_session_index()

    new_session = {
        "sessionId": session_id,
        "name": session_name,
        "workspacePath": str(workspace_path),
        "memoryPath": str(workspace_path / "MEMORY.md"),
        "createdAt": datetime.now().isoformat(),
        "lastActivity": datetime.now().isoformat(),
        "active": True
    }

    index_data["sessions"].append(new_session)
    save_session_index(index_data)

    print(f"✓ Added session to index")
    return new_session


def create_session(session_name):
    """Create a new session with the given name."""
    print(f"\n🚀 Creating new session: {session_name}")

    # Check if session already exists
    index_data = load_session_index()
    existing_session = check_session_exists(session_name, index_data)

    if existing_session:
        print(f"\n⚠️  Warning: Session '{session_name}' already exists")
        print(f"\n📋 Existing session details:")
        print(f"  - Session ID: {existing_session['sessionId']}")
        print(f"  - Created: {existing_session['createdAt']}")
        print(f"  - Last Activity: {existing_session['lastActivity']}")
        print(f"\n💡 Tip: To recreate, first delete the existing session or use a different name")
        return None

    # Generate session ID
    session_id = generate_session_id()
    print(f"✓ Generated session ID: {session_id}")

    # Create session workspace
    workspace_path = create_session_workspace(session_id, session_name)

    # Add to index
    session_data = add_session_to_index(session_id, session_name, workspace_path)

    # Success message
    print(f"\n✅ Successfully created session: {session_name}")
    print(f"  - Session ID: {session_id}")
    print(f"  - Workspace: {workspace_path}")
    print(f"  - Created: {session_data['createdAt']}")
    print(f"\n💡 Tip: You can now discuss topics related to '{session_name}' in this session")
    print(f"       All discussions will be saved to this session's workspace.")

    return session_data


def main():
    """Main entry point."""
    if len(sys.argv) != 2:
        print("Usage: python3 scripts/create_session.py \"<session-name>\"")
        print("Example: python3 scripts/create_session.py \"表单类excel功能设计\"")
        sys.exit(1)

    session_name = sys.argv[1]

    if not session_name or session_name.strip() == "":
        print("❌ Error: Session name cannot be empty")
        sys.exit(1)

    # Create session
    result = create_session(session_name.strip())

    if result:
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
