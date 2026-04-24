#!/usr/bin/env python3
"""
List recent OpenClaw sessions (up to 3 most recent).
Usage: python3 scripts/list_sessions.py
"""

import sys
import json
from datetime import datetime
from pathlib import Path

# Configuration
WORKSPACE_ROOT = Path("/root/.openclaw/workspace")
SESSIONS_DIR = WORKSPACE_ROOT / "sessions"
SESSION_INDEX_FILE = SESSIONS_DIR / "index.json"
MAX_SESSIONS = 3


def load_session_index():
    """Load session index from file."""
    if not SESSION_INDEX_FILE.exists():
        return {"sessions": []}

    with open(SESSION_INDEX_FILE, 'r', encoding='utf-8') as f:
        return json.load(f)


def format_timestamp(iso_timestamp):
    """Format ISO timestamp to readable string."""
    try:
        dt = datetime.fromisoformat(iso_timestamp.replace('Z', '+00:00'))
        now = datetime.now()
        diff = now - dt

        if diff.total_seconds() < 60:
            return "刚刚"
        elif diff.total_seconds() < 3600:
            return f"{int(diff.total_seconds() / 60)}分钟前"
        elif diff.total_seconds() < 86400:
            return f"{int(diff.total_seconds() / 3600)}小时前"
        else:
            return f"{diff.days}天前"
    except:
        return iso_timestamp


def list_sessions():
    """List up to MAX_SESSIONS most recent sessions."""
    print(f"\n📋 最近会话（最多{MAX_SESSIONS}个）：\n")

    # Load session index
    index_data = load_session_index()
    sessions = index_data.get("sessions", [])

    if not sessions:
        print("📭 暂无会话")
        print(f"\n💡 提示：使用\"新建会话名为<name>\"创建新会话")
        return 0

    # Sort by last activity (most recent first)
    sessions_sorted = sorted(
        sessions,
        key=lambda x: x.get("lastActivity", ""),
        reverse=True
    )

    # Get top N sessions
    recent_sessions = sessions_sorted[:MAX_SESSIONS]

    # Display sessions
    for idx, session in enumerate(recent_sessions, 1):
        session_name = session.get("name", "Unknown")
        session_id = session.get("sessionId", "N/A")
        created_at = session.get("createdAt", "N/A")
        last_activity = session.get("lastActivity", "N/A")
        is_active = session.get("active", False)

        # Format timestamps
        created_formatted = format_timestamp(created_at)
        last_activity_formatted = format_timestamp(last_activity)

        # Status indicator
        if is_active:
            status = "活跃中 ⭐"
        else:
            status = "非活跃"

        print(f"{idx}. {session_name} ({status})")
        print(f"   会话ID: {session_id}")
        print(f"   创建时间: {created_formatted} ({created_at})")
        print(f"   最后活动: {last_activity_formatted}")
        print(f"   状态: {status}")
        print()

    # Print hint
    if len(sessions) > MAX_SESSIONS:
        print(f"💡 还有 {len(sessions) - MAX_SESSIONS} 个历史会话未显示")

    print(f"💡 提示：使用\"切换会话到<name>\"可以切换到指定会话")
    print(f"       使用\"删除会话名<name>\"可以删除会话")

    return len(recent_sessions)


def main():
    """Main entry point."""
    count = list_sessions()
    sys.exit(0)


if __name__ == "__main__":
    main()
