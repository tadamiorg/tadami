#!/usr/bin/env python3
"""
Computes the next release version from the commits since the last vX.Y.Z tag, and writes the release notes.

    next_version.py [--bump auto|patch|minor|major] [--repo owner/name] [--notes notes.md]
                    [--github-output $GITHUB_OUTPUT]

auto reads the commit subjects (conventional commits, merge commits skipped): a breaking change
("feat!: ...", "fix(scope)!: ..." or a "BREAKING CHANGE:" line in the body) bumps the major version, else a
"feat" bumps the minor one, else the patch one. The notes list the breaking changes, features and fixes.

Prints the new version, and with --github-output writes version, tag, previous and bump for later steps.
"""
import argparse
import re
import subprocess
import sys

TAG = re.compile(r"^v(\d+)\.(\d+)\.(\d+)$")
SUBJECT = re.compile(r"^(?P<type>[A-Za-z]+)(?:\((?P<scope>[^)]*)\))?(?P<breaking>!)?:\s*(?P<description>\S.*)$")
BREAKING_FOOTER = re.compile(r"^BREAKING[ -]CHANGE:", re.MULTILINE)
SECTIONS = (("breaking", "Breaking changes"), ("feat", "New"), ("fix", "Fixes"))


def git(*args):
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True, encoding="utf-8").stdout


def fail(message):
    print(f"::error::{message}")
    sys.exit(1)


def last_release():
    versions = []
    for tag in git("tag", "--list", "v*").splitlines():
        match = TAG.match(tag.strip())
        if match:
            versions.append(tuple(int(part) for part in match.groups()))
    if not versions:
        fail("No vX.Y.Z tag found. The workflow needs the whole history and the tags (fetch-depth: 0).")
    return max(versions)


def commits_since(tag):
    """(subject, body) of each non-merge commit after the tag, newest first."""
    log = git("log", "--no-merges", "--format=%s%x1f%b%x1e", f"{tag}..HEAD")
    commits = []
    for record in log.split("\x1e"):
        record = record.strip("\n")
        if record:
            subject, _, body = record.partition("\x1f")
            commits.append((subject.strip(), body))
    return commits


def kind_of(subject, body):
    """breaking, feat, fix, another conventional type, or None for a free-form subject."""
    match = SUBJECT.match(subject)
    if match is None:
        return None
    if match["breaking"] or BREAKING_FOOTER.search(body):
        return "breaking"
    kind = match["type"].lower()
    return "fix" if kind == "perf" else kind


def bumped(version, bump):
    major, minor, patch = version
    if bump == "major":
        return major + 1, 0, 0
    if bump == "minor":
        return major, minor + 1, 0
    return major, minor, patch + 1


def release_notes(commits, previous_tag, tag, repo):
    entries = {section: [] for section, _ in SECTIONS}
    seen = set()
    for subject, body in commits:
        kind = kind_of(subject, body)
        # A squash of commits already listed (a merged pull request) repeats their subject
        if kind not in entries or subject in seen:
            continue
        seen.add(subject)
        description = SUBJECT.match(subject)["description"].strip()
        entries[kind].append(description[:1].upper() + description[1:])

    lines = []
    for section, title in SECTIONS:
        if entries[section]:
            lines += [f"### {title}", *(f"- {entry}" for entry in entries[section]), ""]
    if not lines:
        lines = ["Maintenance release.", ""]
    lines.append(f"**Full changelog**: https://github.com/{repo}/compare/{previous_tag}...{tag}")
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--bump", choices=("auto", "patch", "minor", "major"), default="auto")
    parser.add_argument("--repo", default="tadamiorg/tadami", help="owner/name, for the changelog link")
    parser.add_argument("--notes", help="file to write the release notes to (Markdown)")
    parser.add_argument("--github-output", help="file to append the step outputs to ($GITHUB_OUTPUT)")
    args = parser.parse_args()

    previous = last_release()
    previous_tag = "v%d.%d.%d" % previous
    commits = commits_since(previous_tag)
    if not commits:
        fail(f"No commits since {previous_tag}: nothing to release.")

    bump = args.bump
    if bump == "auto":
        kinds = {kind_of(subject, body) for subject, body in commits}
        bump = "major" if "breaking" in kinds else "minor" if "feat" in kinds else "patch"
    version = "%d.%d.%d" % bumped(previous, bump)
    tag = f"v{version}"
    if git("tag", "--list", tag).strip():
        fail(f"The tag {tag} already exists.")

    print(f"{previous_tag} -> {tag} ({bump}, {len(commits)} commits)")
    notes = release_notes(commits, previous_tag, tag, args.repo)
    print(notes)
    if args.notes:
        with open(args.notes, "w", encoding="utf-8") as file:
            file.write(notes)
    if args.github_output:
        with open(args.github_output, "a", encoding="utf-8") as file:
            file.write(f"version={version}\ntag={tag}\nprevious={previous_tag}\nbump={bump}\n")


if __name__ == "__main__":
    main()
