#!/bin/bash

set -x

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

shared_directory="$1"

local_directory="$2"

. $SCRIPT_DIR/spark_app_cmd.sh

# collect files to local
hdfs dfs -get $shared_directory $local_directory
