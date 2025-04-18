#!/bin/sh
# This script fetches a source data repository and replaces the current data with it.
#
# Env vars:
#  - SOURCE_DATA_REPO (required): URL of a Git repository containing a resources and clients folder.
#    The script does nothing if not set.
#
#  - SOURCE_DATA_BRANCH (optional): Branch name to fetch. Defaults to main if not set

if [ -n "$SOURCE_DATA_REPO" ]; then
  TARGET_DIR=temp-fetch-target

  if [ -z "$SOURCE_DATA_BRANCH" ]; then
    SOURCE_DATA_BRANCH=main
  fi
  echo Copying data from $SOURCE_DATA_REPO ...
  git clone --branch $SOURCE_DATA_BRANCH --depth 1 $SOURCE_DATA_REPO $TARGET_DIR

  rm -r resources/
  mv $TARGET_DIR/resources .

  rm -r clients/
  mv $TARGET_DIR/clients .

  rm -r $TARGET_DIR
fi
