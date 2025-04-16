#!/bin/sh
# This script is intended to be called from Docker to perform any needed
# initialization before launching the jetty server.
 
./fetch_data_repo.sh

# Run the same command as the original Jetty image to start the server, see:
# https://github.com/jetty/jetty.docker/blob/master/baseDockerfile
/docker-entrypoint.sh java -jar "$JETTY_HOME/start.jar"