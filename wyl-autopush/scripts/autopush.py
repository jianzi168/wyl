#!/usr/bin/env python3
"""
Wyl Autopush Script
Automates git add/commit/push workflow for wyl repository.

Usage:
    python3 scripts/autopush.py <filename> <commit-message>

Example:
    python3 scripts/autopush.py README.md "Add README.md - project overview"
"""

import sys
import os
import subprocess
from pathlib import Path

# Configuration
WYL_REPO = Path("/root/.openclaw/workspace/wyl")
GIT_EMAIL = "jianzi168@users.noreply.github.com"
GIT_NAME = "jianzi168"

def ensure_git_config():
    """Ensure git user identity is configured."""
    try:
        subprocess.run(
            ["git", "config", "user.email", GIT_EMAIL],
            cwd=WYL_REPO,
            check=True,
            capture_output=True
        )
        subprocess.run(
            ["git", "config", "user.name", GIT_NAME],
            cwd=WYL_REPO,
            check=True,
            capture_output=True
        )
    except subprocess.CalledProcessError as e:
        print(f"Error configuring git: {e}", file=sys.stderr)
        return False
    return True

def git_add(filename):
    """Add file to git staging area."""
    try:
        result = subprocess.run(
            ["git", "add", filename],
            cwd=WYL_REPO,
            check=True,
            capture_output=True,
            text=True
        )
        return True, result.stdout
    except subprocess.CalledProcessError as e:
        return False, e.stderr

def git_commit(message):
    """Commit staged changes."""
    try:
        result = subprocess.run(
            ["git", "commit", "-m", message],
            cwd=WYL_REPO,
            check=True,
            capture_output=True,
            text=True
        )
        # Extract commit ID from output
        for line in result.stdout.split('\n'):
            if line.startswith('[main'):
                # Extract commit hash from line like "[main 7b49eba] ..."
                commit_id = line.split()[1].rstrip(']')
                return True, commit_id
        return True, "committed"
    except subprocess.CalledProcessError as e:
        return False, e.stderr

def git_push(branch="main"):
    """Push commits to remote repository."""
    try:
        result = subprocess.run(
            ["git", "push", "origin", branch],
            cwd=WYL_REPO,
            check=True,
            capture_output=True,
            text=True
        )
        return True, result.stdout
    except subprocess.CalledProcessError as e:
        return False, e.stderr

def validate_file(filename):
    """Validate that the file exists in the wyl repository."""
    filepath = WYL_REPO / filename
    if not filepath.exists():
        return False, f"File not found: {filepath}"
    if not filepath.is_file():
        return False, f"Path is not a file: {filepath}"
    return True, str(filepath)

def autopush(filename, commit_message):
    """Execute full autopush workflow."""
    # Step 1: Validate file
    success, message = validate_file(filename)
    if not success:
        print(f"❌ Validation failed: {message}", file=sys.stderr)
        return None

    print(f"✓ File validated: {filename}")

    # Step 2: Configure git
    if not ensure_git_config():
        print("❌ Git configuration failed", file=sys.stderr)
        return None
    print("✓ Git configured")

    # Step 3: Git add
    success, output = git_add(filename)
    if not success:
        print(f"❌ Git add failed: {output}", file=sys.stderr)
        return None
    print(f"✓ File staged: {filename}")

    # Step 4: Git commit
    success, output = git_commit(commit_message)
    if not success:
        print(f"❌ Git commit failed: {output}", file=sys.stderr)
        return None
    commit_id = output
    print(f"✓ Committed: {commit_id}")

    # Step 5: Git push
    success, output = git_push()
    if not success:
        print(f"❌ Git push failed: {output}", file=sys.stderr)
        return None
    print("✓ Pushed to origin/main")

    return commit_id

def main():
    """Main entry point."""
    if len(sys.argv) != 3:
        print("Usage: python3 scripts/autopush.py <filename> <commit-message>", file=sys.stderr)
        print("Example: python3 scripts/autopush.py README.md 'Add README.md - project overview'", file=sys.stderr)
        sys.exit(1)

    filename = sys.argv[1]
    commit_message = sys.argv[2]

    commit_id = autopush(filename, commit_message)

    if commit_id:
        print(f"\n✅ Success! File '{filename}' pushed to GitHub (commit: {commit_id})")
        sys.exit(0)
    else:
        print("\n❌ Failed to push file", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
