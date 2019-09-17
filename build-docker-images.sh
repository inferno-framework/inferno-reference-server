#!/bin/sh

docker build . -t artifacts.mitre.org:8200/inferno-us-core-r4-reference-server
docker build . -f Dockerfile.database -t artifacts.mitre.org:8200/inferno-us-core-r4-reference-server-db
