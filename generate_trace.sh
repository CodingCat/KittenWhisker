#!/bin/bash

set -x

local_directory="$1"

java -cp target/happysparking-0.1-SNAPSHOT-jar-with-dependencies.jar me.codingcat.happysparking.StackTraceGenerator $local_directory

# generate graph

for file in $local_directory
do
    if [[ $file =~ .*\.stack ]]; then
        echo $file
        # $FLAMEGRAPH_DIR/stackcollapse-perf.pl $PERF_COLLAPSE_OPTS $file | tee $COLLAPSED | $FLAMEGRAPH_DIR/flamegraph.pl $PERF_FLAME_OPTS > $PERF_FLAME_OUTPUT
    fi
done