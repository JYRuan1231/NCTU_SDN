yum install -y net-tools

#OVS1
ifconfig eth2 172.30.0.5 netmask 255.255.0.0
ifconfig eth3 172.30.0.6 netmask 255.255.0.0
ovs-vsctl add-br ovs1br
ovs-vsctl set-controller ovs1br tcp:172.17.0.2:6653
ovs-vsctl show

#OVS2
ifconfig eth2 172.30.0.7 netmask 255.255.0.0
ifconfig eth3 172.30.0.8 netmask 255.255.0.0
ovs-vsctl add-br ovs2br
ovs-vsctl set-controller ovs2br tcp:172.17.0.2:6653
ovs-vsctl show

#OVS3
ifconfig eth2 172.30.0.9 netmask 255.255.0.0
ifconfig eth3 172.30.0.10 netmask 255.255.0.0
ovs-vsctl add-br ovs3br
ovs-vsctl set-controller ovs3br tcp:172.17.0.2:6653
ovs-vsctl show

#OVS4
ifconfig eth2 172.30.0.11 netmask 255.255.0.0
ifconfig eth3 172.30.0.12 netmask 255.255.0.0
ovs-vsctl add-br ovs4br
ovs-vsctl set-controller ovs4br tcp:172.17.0.2:6653
ovs-vsctl show


ovs-vsctl add-port ovs1br eth1
ovs-vsctl add-port ovs1br eth2
ovs-vsctl add-port ovs1br eth3
ovs-vsctl add-port ovs1br eth4
ovs-vsctl show

ovs-vsctl add-port ovs2br eth1
ovs-vsctl add-port ovs2br eth2
ovs-vsctl add-port ovs2br eth3
ovs-vsctl show

ovs-vsctl add-port ovs3br eth1
ovs-vsctl add-port ovs3br eth2
ovs-vsctl add-port ovs3br eth3
ovs-vsctl show

ovs-vsctl add-port ovs4br eth1
ovs-vsctl add-port ovs4br eth2
ovs-vsctl add-port ovs4br eth3
ovs-vsctl show
