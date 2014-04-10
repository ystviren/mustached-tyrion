#!/bin/bash
# Worker.sh
# $1 is zookeeper location
# $2 worker id, must be uniques
java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. Worker $1 $2
