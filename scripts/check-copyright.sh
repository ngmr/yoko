#!/bin/sh

# Copyright 2024 IBM Corporation and others.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an \"AS IS\" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0

# Enforce top-level subshell to avoid leaking environment changes (in case script is sourced)
(
  # Stop on first unexpected error
  set -e

  usage() {
    echo "usage:\t$0 [-q|--quiet|-t|--terse|-v|--verbose] git-base-ref"
    echo "\t-h,--help\tprints this usage info"
    echo "\t-t,--terse\tsuppresses all informational outpout"
    echo "\t-q,--quiet\tsuppresses all non-error outpout"
    echo "\t-v,--verbose\tenables verbose output"
  }

  # Copy stdout and stderr to other file descriptors for logging and error reporting
  exec 3>/dev/null 4>&1 5>&1 6>&2
  # Define semantic functions to echo or cat messages to the various file descriptors
  echocat() { { [ $# -gt 0 ] && echo "$@"; } || cat; }
  log() { echocat "$@" >&3; }
  inf() { echocat "$@" >&4; }
  wrn() { echocat "$@" >&5; }
  err() { echocat "$@" >&6; }
  # Define a Perl-style "die" function to emit an error message and exit with an error code
  die() {
    err "$@"
    exit 1
  }

  # Parse script options
  while [ $# -gt 0 ]; do
    case "$1" in
      # -t disables logging and info
      -h|--help) usage; exit 0;;
      # -t disables logging and info
      -t|--terse)   exec 3>/dev/null 4>/dev/null 5>&2;        shift;;
      # -q disables logging, info, and warning
      -q|--quiet)   exec 3>/dev/null 4>/dev/null 5>/dev/null; shift;;
      # -v enables logging
      -v|--verbose) exec 3>&1        4>&1        5>&2;        shift;;
      # -- indicates the explicit end of options, so consume it and exit the loop
      --) shift; break;;
      # any other option-like string is an error
      -*) err "$0: unknown option '$1'"; usage | die;;
      # any non-option-like string indicates the end of the options
      *) break;;
    esac
  done

  # Check that git is a command
  command -v git > /dev/null 2>&1 || die "Can not find 'git' command."

  # Check that base ref has been specified
  [ -n "$1" ] || die "Missing required first parameter: git-base-ref"
  BASE="$1"

  # Check it is a valid ref
  git rev-parse --quiet --verify "$BASE" > /dev/null || die "Specified git base ref '$BASE' is not a valid ref in this repository."

  # Git uses the following one-character change status codes
  # A - Added
  # C - Copied
  # D - Deleted
  # M - Modified
  # R - Renamed

  # Keep track of how many files failed the copyright check so we can find them all and return 0 if there were none
  FAILED=0

  # Look for unsupported changes with Broken (B), changed (T), Unmerged (U), or Unknown (X) status
  BAD_FILES="$(git diff --name-status --diff-filter=BTUX "$BASE")"

  [ -z "$BAD_FILES" ] || {
    err "‼️ This script ($0) may need fixing to deal with more types of change."
    echo "$BAD_FILES" | sed 's/^/🤯 Unsupported change type: /' | err
    FAILED=$(( FAILED + $(echo "$BAD_FILES"|wc -l) ))
  }

  # Log deleted files
  git diff --name-only --diff-filter=D "$BASE" | sed 's/^/🫥 Ignoring deleted file: /' | log

  # Function to print each file from stdin to stdout unless it has good copyright
  badCopyrightFilter() {
    while read filePath; do
      [ -f "$filePath" ] || die "Cannot check copyright in non-existent file: '$filePath'"
      grep -Eq "SPDX-License-Identifier: Apache-2.0" "$filePath" || {
        wrn "👿 License identifier not found: $filePath"
        echo "$filePath"
        continue
      }
      yearModified=`git log -1 --pretty=format:%cd --date=format:%Y -- "$filePath"`
      grep -Eq "Copyright $yearModified IBM Corporation and" "$filePath" && inf "😅 Copyright OK: $filePath" || {
        existingModifiedYear="$(grep -Eo 'Copyright [0-9]{4} IBM Corporation and' "$filePath" | cut -d ' ' -f 2 )"
        case "$existingModifiedYear" in
          "$yearModified") continue ;;
          "")              wrn "🤬 No copyright year in '$filePath': expected '$yearModified'." ;;
          *)               wrn "😡 Wrong copyright year in '$filePath': expected '$yearModified' but found '$existingModifiedYear'." ;;
        esac
        echo "$filePath"
      }
    done
  }

  inf "Checking added and modified files..."
  FAILED=$((FAILED + $(git diff --name-only --find-copies-harder --diff-filter=AM "$BASE" | badCopyrightFilter | wc -l)))

  # Renamed (R) and copied (C) files are more complicated.
  # They can report as less than 100% identical even when their contents are the same.
  # This is apparently due to metadata changes. Shrug.
  # So check whether the contents have changed significantly.

  # Define how to compare a file against its origin for significant content changes.
  # Succeed if there are differences, and print the filename.
  isReallyDifferent() { ! git diff --ignore-all-space --quiet "$1" "$2" 2>/dev/null && echo "$2"; }

  # Read status, source, and destination as separate records (lines).
  # Check the status is R... or C... (otherwise it was parsed incorrectly).
  # Then compare the source and dest for significant differences.
  # Lastly, if they were different, check them for copyright.
  badCopyrightFilter2() {
    while read status && read src && read dst
    do
      case "$status" in
        R*) isReallyDifferent "$BASE:$src" "$dst" || log "🫥 Ignoring renamed file: $src -> $dst" ;;
        C*) isReallyDifferent "$BASE:$src" "$dst" || log "🫥 Ignoring copied file: $src -> $dst" ;;
        *) die "Unexpected status while parsing git diff output: status='$status' src='$src' dst='$dst'" ;;
      esac
    done | badCopyrightFilter
  }

  inf "Checking renamed and copied files..."
  OUTPUT="$(git diff --name-status --find-copies-harder --diff-filter=CR -z "$BASE" 2>/dev/null | tr '\0' '\n')"
  FAILED=$((FAILED + $(echo "$OUTPUT"| badCopyrightFilter2 | wc -l)))
  exit $FAILED
)
