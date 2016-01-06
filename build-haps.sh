#!/bin/sh

export MAVEN_OPTS="-Xmx7500M"
mvn clean compile dependency:copy-dependencies package
