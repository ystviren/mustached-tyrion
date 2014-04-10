#!/bin/bash
# MD5.sh
# $1 is word to get hash

java -classpath lib/zookeeper-3.3.2.jar:lib/log4j-1.2.15.jar:. MD5Test $1
