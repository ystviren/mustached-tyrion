#!/bin/bash
# JobTracker.sh
# $1 listening port
# $2 zookeeper location
java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar FileServer $1 $2
