#!/bin/bash

set -x

username="$1"

NODES=`cat slaves`

for slave_ip in `echo $NODES|sed  "s/#.*$//;/^$/d"`; do
    echo $slave_ip
    ssh $username@$slave_ip "echo 'yarn ALL=(ALL) NOPASSWD:/usr/bin/perf,/bin/chmod' | sudo tee --append /etc/sudoers.d/yarn"
done

. spark_app_cmd.sh

for slave_ip in `echo $NODES|sed  "s/#.*$//;/^$/d"`; do
    echo $slave_ip
    ssh $username@$slave_ip "sudo rm /etc/sudoers.d/yarn"
done
