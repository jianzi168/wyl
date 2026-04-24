#!/usr/bin/env python3
"""
Delete an OpenClaw session by name.
Usage: python3 scripts/delete_session.py "<session-name>"
"""

import sys
import json
import shutil
from datetime import datetime
from pathlib import Path

# Configuration
WORKSPACE_ROOT = Path("/root/.openclaw/workspace")
SESSIONS_DIR = WORKSPACE_ROOT / "sessions"
SESSION_INDEX_FILE = SESSIONS_DIR / "index.json"


def load_session_index():
    """Load session index from file."""
    if not SESSION_INDEX_FILE.exists():
        return {"sessions": []}

    with open(SESSION_INDEX_FILE, 'r', encoding='utf-8') as f:
        return json.load(f)


def save_session_index(data):
    """Save session index to file."""
    data["metadata"]["lastUpdated"] = datetime.now().isoformat()
    with open(SESSION_INDEX_FILE, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)


def find_session_by_name(session_name, index_data):
    """Find session by name."""
    for session in index_data.get("sessions", []):
        if session.get("name") == session_name:
            return session
    return None


def list_available_sessions(index_data):
    """List all available sessions."""
    sessions = index_data.get("sessions", [])

    if not sessions:
        print("📭 暂无会话")
        return

    print("📋 当前可用会话：")
    for idx, session in enumerate(sessions, 1):
        session_name = session.get("name", "Unknown")
        print(f"  - {session_name}")


def delete_session(session_name):
    """Delete a session by name."""
    print(f"\n🗑️  Deleting session: {session_name}")

    # Load session index
    index_data = load_session_index()

    # Find session
    session = find_session_by_name(session_name, index_data)

    if not session:
        print(f"\n❌ 错误：找不到会话 \"{session_name}\"")
        print(f"\n📋 当前可用会话：")
        list_available_sessions(index_data)
        print(f"\n💡 提示：请检查会话名称是否正确，或使用\"当前会话列表\"查看所有会话")
        return False

    # Get session details
    session_id = session.get("sessionId")
    workspace_path = session.get("workspacePath")
    memory_path = session.get("memoryPath")

    print(f"✓ 找到会话")
    print(f"  - 会话ID: {session_id}")
    print(f"  - 工作空间: {workspace_path}")

    # Delete workspace directory
    if workspace_path and Path(workspace_path).exists():
        shutil.rmtree(workspace_path)
        print(f"✓ 已删除工作空间: {workspace_path}")
    else:
        print(f"⚠️  警告：工作空间不存在: {workspace_path}")

    # Remove session from index
    index_data["sessions"] = [
        s for s in index_data["sessions"]
        if s.get("sessionId") != session_id
    ]
    save_session_index(index_data)
    print(f"✓ 已从会话索引中移除")

    # Success message
    print(f"\n✅ 已删除会话：{session_name}")
    print(f"  - 会话ID: {session_id}")
    print(f"  - 删除时间: {datetime.now().isoformat()}")
    print(f"  - 工作空间: 已删除")
    print(f"  - 内存文件: 已删除")
    print(f"\n⚠️  注意：会话及其所有内容已被永久删除，无法恢复")

    return True


def main():
    """Main entry point."""
    if len(sys.argv) != 2:
        print("Usage: python3 scripts/delete_session.py \"<session-name>\"")
        print("Example: python3 scripts/delete_session.py \"产品规划讨论\"")
        sys.exit(1)

    session_name = sys.argv[1]

    if not session_name or session_name.strip() == "":
        print("❌ Error: Session name cannot be empty")
        sys.exit(1)

    # Delete session
    result = delete_session(session_name.strip())

    if result:
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
