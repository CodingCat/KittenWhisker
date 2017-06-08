#!/bin/bash

set -x

username="$1"

NODES=`cat slaves`

for slave_ip in `echo NODES|sed  "s/#.*$//;/^$/d"`; do
    echo $slave_ip
    ssh $username@$slave_ip echo 'export HADOOP_YARN_USER=zhunan' > ~/.bashrc
    # scp -r conf/spark.headnode.conf $username@$slave_ip:/etc/opt/microsoft/omsagent/conf/omsagent.d
    # ssh $username@$slave_ip "sudo pkill -9 -f 'omsagent';sudo killall -9 -u omsagent"
    # ssh $username@$slave_ip sudo /opt/microsoft/omsagent/ruby/bin/ruby /opt/microsoft/omsagent/bin/omsagent -d /var/opt/microsoft/omsagent/$workspaceid/run/omsagent.pid -o /var/opt/microsoft/omsagent/$workspaceid/log/omsagent.log -c /etc/opt/microsoft/omsagent/$workspaceid/conf/omsagent.conf --no-supervisor
done


