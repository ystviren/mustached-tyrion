#!/bin/bash
# ClientDriver.sh
# $1 is zookeeper location

java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar ClientDriver $1
