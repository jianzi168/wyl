---
name: wyl-autopush
description: Automatic file generation and git push workflow for wyl repository. Use when creating new files that need to be committed and pushed to the GitHub repository at /root/.openclaw/workspace/wyl/. Automatically handles: (1) Creating files in the wyl workspace, (2) Providing file names to user, (3) Git add/commit/push operations. Triggered when user asks to create files for the wyl project or needs files automatically committed to the repository.
---

# Wyl Autopush

## Overview

Automates the file generation and git workflow for the wyl GitHub repository. When creating new files, this skill ensures they are automatically saved to the correct workspace, reported to the user, and committed to the repository with proper git operations.

## Workflow

### When This Skill Activates

- Creating new files for the wyl project (documents, code, configs, etc.)
- Generating content that needs version control
- Any file operation related to `/root/.openclaw/workspace/wyl/`

### Standard File Generation Workflow

1. **Create file** at `/root/.openclaw/workspace/wyl/`
2. **Report filename** to user (always inform user what was created)
3. **Execute git operations**:
   ```bash
   cd /root/.openclaw/workspace/wyl
   git add <filename>
   git commit -m "Add <brief description>"
   git push origin main
   ```
4. **Confirm push** to user with commit ID

### Commit Message Guidelines

- Use clear, descriptive commit messages
- Format: `[Add|Update|Fix] <file-name> - <brief description>`
- Examples:
  - "Add README.md - project overview and quick start guide"
  - "Update design.md - add complex examples"
  - "Fix schema.sql - correct index syntax"

## Resources

### scripts/autopush.py

Automated script that handles the full git workflow:

```bash
# Usage (run within skill context)
python3 scripts/autopush.py <filename> <commit-message>
```

The script:
- Validates the file exists in wyl workspace
- Performs git add/commit/push
- Returns commit ID for user confirmation
- Handles git identity if not configured
