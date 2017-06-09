#!/bin/bash

set -x

local_directory="$1"

java -cp target/happysparking-0.1-SNAPSHOT-jar-with-dependencies.jar me.codingcat.happysparking.StackTraceGenerator $local_directory