#!/bin/bash

set -x

shared_directory="$1"

local_directory="$2"

. spark_app_cmd.sh

# collect files to local
hdfs dfs -get $shared_directory $local_directory
