#!/bin/bash

set -x

username="$1"

NODES=`cat slaves`

for slave_ip in `echo $NODES|sed  "s/#.*$//;/^$/d"`; do
    echo $slave_ip
    ssh $username@$slave_ip "sudo adduser yarn sudo"
done

. spark_app_cmd.sh

for slave_ip in `echo $NODES|sed  "s/#.*$//;/^$/d"`; do
    echo $slave_ip
    ssh $username@$slave_ip "sudo deluser yarn sudo"
done
