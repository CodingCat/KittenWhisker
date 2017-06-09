#!/bin/bash

set -x

username="$1"

shared_directory = "$2"

local_directory = "$3"

NODES=`cat slaves`

# add permission for yarn to run perf
for slave_ip in `echo $NODES|sed  "s/#.*$//;/^$/d"`; do
    echo $slave_ip
    ssh $username@$slave_ip "echo 'yarn ALL=(ALL) NOPASSWD:/usr/bin/perf,/bin/chmod' | sudo tee --append /etc/sudoers.d/yarn"
done

. spark_app_cmd.sh

# revoke permission of yarn
for slave_ip in `echo $NODES|sed  "s/#.*$//;/^$/d"`; do
    echo $slave_ip
    ssh $username@$slave_ip "sudo rm /etc/sudoers.d/yarn"
done

# collect files to local
hdfs dfs -get $shared_directory $local_directory