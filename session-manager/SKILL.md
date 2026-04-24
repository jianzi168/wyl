---
name: session-manager
description: Session management for OpenClaw. Use when creating new sessions, deleting sessions, or listing active sessions. Supports creating named sessions (like "表单类excel功能设计"), deleting sessions by name, and displaying recent sessions (up to 3 most recent).
---

# Session Manager

## Overview

Session management skill that creates, deletes, and lists OpenClaw sessions. Enables users to organize conversations into named sessions for better context management and tracking.

### Key Features

- **Create Session**: Create new named sessions (like "表单类excel功能设计")
- **Delete Session**: Remove sessions by name
- **List Sessions**: Display up to 3 most recent sessions
- **Session Context**: Each session maintains its own conversation history and memory

---

## Quick Start

### Creating a New Session

```
User: 新建会话名为表单类excel功能设计

Agent: ✅ 已创建新会话：表单类excel功能设计
     会话ID: session-123456
     创建时间: 2026-04-23 02:30 UTC
```

### Listing Recent Sessions

```
User: 当前会话列表

Agent: 📋 最近会话（最多3个）：

     1. 表单类excel功能设计
        - 会话ID: session-123456
        - 创建时间: 2026-04-23 02:30 UTC
        - 最后活动: 2026-04-23 02:35 UTC

     2. 产品规划讨论
        - 会话ID: session-789012
        - 创建时间: 2026-04-22 18:00 UTC
        - 最后活动: 2026-04-22 23:45 UTC

     3. 技术方案评审
        - 会话ID: session-345678
        - 创建时间: 2026-04-22 10:00 UTC
        - 最后活动: 2026-04-22 16:30 UTC
```

### Deleting a Session

```
User: 删除会话名产品规划讨论

Agent: ✅ 已删除会话：产品规划讨论
     会话ID: session-789012
     删除时间: 2026-04-23 02:40 UTC
```

---

## Capabilities

### 1. Create Named Session

Create a new session with a specified name. The session maintains its own conversation history and workspace.

**Trigger Phrases:**
- "新建会话名为{name}"
- "创建会话{name}"
- "新会话{name}"

**Process:**
1. Parse session name from user input
2. Create new session with unique ID
3. Initialize session workspace
4. Create memory file for the session
5. Return confirmation with session details

**Example:**
```
User: 新建会话名为数据库设计讨论

Agent: ✅ 已创建新会话：数据库设计讨论
     会话ID: session-999888
     工作空间: /root/.openclaw/workspace/session-999888/
     创建时间: 2026-04-23 02:45 UTC
```

### 2. List Recent Sessions

Display up to 3 most recent sessions with their metadata.

**Trigger Phrases:**
- "当前会话列表"
- "会话列表"
- "显示会话"
- "列出会话"

**Process:**
1. Query all available sessions
2. Sort by last activity (most recent first)
3. Return top 3 sessions with details

**Example:**
```
User: 会话列表

Agent: 📋 最近会话（3个）：

     1. 表单类excel功能设计 (活跃中)
        会话ID: session-123456
        创建时间: 2026-04-23 02:30 UTC
        最后活动: 刚刚

     2. 数据库设计讨论
        会话ID: session-999888
        创建时间: 2026-04-23 02:45 UTC
        最后活动: 2分钟前

     3. 技术方案评审
        会话ID: session-345678
        创建时间: 2026-04-22 10:00 UTC
        最后活动: 10小时前
```

### 3. Delete Session

Remove a session by name. This deletes the session's workspace and conversation history.

**Trigger Phrases:**
- "删除会话名{name}"
- "删除会话{name}"
- "移除会话{name}"

**Process:**
1. Parse session name from user input
2. Find session by name
3. Delete session workspace
4. Delete session memory file
5. Return confirmation

**Example:**
```
User: 删除会话名技术方案评审

Agent: ✅ 已删除会话：技术方案评审
     会话ID: session-345678
     删除时间: 2026-04-23 02:50 UTC
     工作空间: 已删除 /root/.openclaw/workspace/session-345678/
```

---

## Session Data Structure

### Session Storage

Each session maintains:
- **Session ID**: Unique identifier (UUID)
- **Session Name**: User-provided name
- **Workspace Path**: Directory for session-specific files
- **Memory File**: Session-specific memory (MEMORY.md)
- **Created At**: Session creation timestamp
- **Last Activity**: Last message timestamp
- **Active Status**: Whether session is currently active

### Session Index File

`/root/.openclaw/workspace/sessions/index.json`

```json
{
  "sessions": [
    {
      "sessionId": "session-123456",
      "name": "表单类excel功能设计",
      "workspacePath": "/root/.openclaw/workspace/session-123456/",
      "memoryPath": "/root/.openclaw/workspace/session-123456/MEMORY.md",
      "createdAt": "2026-04-23T02:30:00Z",
      "lastActivity": "2026-04-23T02:35:00Z",
      "active": true
    }
  ]
}
```

---

## Resources

### scripts/

#### create_session.py

Creates a new session with the specified name.

```bash
python3 scripts/create_session.py "<session-name>"
```

**Features:**
- Generates unique session ID (UUID)
- Creates session workspace directory
- Initializes session memory file
- Updates session index
- Returns session details

#### list_sessions.py

Lists recent sessions (up to 3).

```bash
python3 scripts/list_sessions.py
```

**Features:**
- Reads session index file
- Sorts by last activity
- Returns top 3 sessions
- Displays session metadata

#### delete_session.py

Deletes a session by name.

```bash
python3 scripts/delete_session.py "<session-name>"
```

**Features:**
- Finds session by name
- Deletes session workspace
- Removes session from index
- Handles errors gracefully

---

## Usage Examples

### Example 1: Creating a New Session

```
User: 新建会话名为wyl公式引擎设计

Agent: ✅ 已创建新会话：wyl公式引擎设计
     会话ID: session-abc123
     工作空间: /root/.openclaw/workspace/session-abc123/
     创建时间: 2026-04-23 02:55 UTC

     📝 已创建会话文件：
     - /root/.openclaw/workspace/session-abc123/MEMORY.md
     - /root/.openclaw/workspace/session-abc123/IDENTITY.md
     - /root/.openclaw/workspace/session-abc123/USER.md

     💡 提示：现在可以在此会话中讨论wyl公式引擎设计，所有讨论内容将保存在此会话中
```

### Example 2: Listing Sessions

```
User: 当前会话列表

Agent: 📋 最近会话（3个）：

     1. wyl公式引擎设计 (活跃中) ⭐
        会话ID: session-abc123
        创建时间: 2026-04-23 02:55 UTC
        最后活动: 刚刚
        状态: 活跃

     2. 表单类excel功能设计
        会话ID: session-123456
        创建时间: 2026-04-23 02:30 UTC
        最后活动: 10分钟前
        状态: 非活跃

     3. 数据库设计讨论
        会话ID: session-999888
        创建时间: 2026-04-23 02:45 UTC
        最后活动: 15分钟前
        状态: 非活跃

     💡 提示：使用"切换会话 {name}"可以切换到指定会话
```

### Example 3: Deleting a Session

```
User: 删除会话名数据库设计讨论

Agent: ✅ 已删除会话：数据库设计讨论
     会话ID: session-999888
     删除时间: 2026-04-23 03:00 UTC

     🗑️  已删除内容：
     - 工作空间: /root/.openclaw/workspace/session-999888/
     - 内存文件: /root/.openclaw/workspace/session-999888/MEMORY.md
     - 会话记录: 已从会话索引中移除

     ⚠️  注意：会话及其所有内容已被永久删除，无法恢复
```

### Example 4: Session Context Switching

```
User: 切换会话到wyl公式引擎设计

Agent: ✅ 已切换会话：wyl公式引擎设计
     会话ID: session-abc123
     最后活动: 刚刚

     📝 会话摘要：
     - 上次讨论: Excel公式链路DAG系统设计
     - 创建文件: 2026-04-22-excel-formula-dag-design.md
     - 设计要点: 伪坐标系统、跨表公式、剪支逻辑

     💡 现在此会话中，继续讨论...
```

---

## Error Handling

### Session Not Found

```
User: 删除会话名不存在的会话

Agent: ❌ 错误：找不到会话 "不存在的会话"

     📋 当前可用会话：
     - wyl公式引擎设计
     - 表单类excel功能设计

     💡 提示：请检查会话名称是否正确，或使用"当前会话列表"查看所有会话
```

### Session Name Conflict

```
User: 新建会话名为表单类excel功能设计

Agent: ⚠️  警告：会话 "表单类excel功能设计" 已存在

     📋 会话详情：
     - 会话ID: session-123456
     - 创建时间: 2026-04-23 02:30 UTC
     - 最后活动: 20分钟前

     💡 提示：如需重新创建，请先删除现有会话，或使用不同的名称
```

---

## Notes

- **Session Limit**: Currently supports unlimited sessions, but displays only 3 most recent
- **Persistence**: Session data persists in workspace directories
- **Memory**: Each session has its own MEMORY.md file for context
- **Deletion**: Deleting a session permanently removes all its data
- **Activity Tracking**: Session "last activity" is updated on each message

---

## Implementation Details

### Session ID Format

Session IDs are generated using UUID format:
```
session-<8-char-uuid>

Example: session-a1b2c3d4
```

### Workspace Structure

```
/root/.openclaw/workspace/
├── sessions/
│   └── index.json                    # Session index
└── session-<id>/                     # Individual session workspace
    ├── MEMORY.md                      # Session memory
    ├── IDENTITY.md                    # Session identity
    ├── USER.md                        # User info
    └── (other session files)
```

### Session Index JSON Schema

```json
{
  "sessions": [
    {
      "sessionId": "string (UUID)",
      "name": "string",
      "workspacePath": "string (path)",
      "memoryPath": "string (path)",
      "createdAt": "string (ISO8601)",
      "lastActivity": "string (ISO8601)",
      "active": "boolean"
    }
  ],
  "metadata": {
    "version": "1.0",
    "lastUpdated": "string (ISO8601)"
  }
}
```

---

## Future Enhancements

- [ ] Session archiving for old sessions
- [ ] Session search and filtering
- [ ] Session tags and categories
- [ ] Session export/import
- [ ] Session sharing and collaboration
- [ ] Session templates
