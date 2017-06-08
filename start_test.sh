#!/bin/bash

set -x

username="$1"

NODES=`cat slaves`

SPARK_SUBMIT_COMMAND=`cat spark_app_cmd`

for slave_ip in `echo NODES|sed  "s/#.*$//;/^$/d"`; do
    echo $slave_ip
    ssh $username@$slave_ip "sudo adduser yarn sudo"
done

for slave_ip in `echo NODES|sed  "s/#.*$//;/^$/d"`; do
    echo $slave_ip
    ssh $username@$slave_ip "sudo deluser yarn sudo"
done
