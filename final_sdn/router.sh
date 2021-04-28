#Other Router
apt-get update
apt-get install -y vim quagga net-tools telnet
vim /etc/sysctl.conf
sysctl -p
vim /etc/quagga/daemons 
vim /etc/quagga/zebra.conf
'''
hostname RXzebra
password sdnip
log stdout
'''
vim /etc/quagga/bgpd.conf
'''
! BGP configuration for R1
!
hostname R1bgp
password sdnip
!
router bgp 65001
	bgp router-id 172.22.0.2
	timers bgp 3 9
	neighbor 172.22.0.100 remote-as 65000
	neighbor 172.22.0.100 ebgp-multihop
	neighbor 172.22.0.100 timers connect 5
	neighbor 172.22.0.100 advertisement-interval 5
	network 172.18.0.0/16
!
log stdout
'''

'''
! BGP configuration for R2
!
hostname R2bgp
password sdnip
!
router bgp 65002
	bgp router-id 172.23.0.2
	timers bgp 3 9
	neighbor 172.23.0.100 remote-as 65000
	neighbor 172.23.0.100 ebgp-multihop
	neighbor 172.23.0.100 timers connect 5
	neighbor 172.23.0.100 advertisement-interval 5
	network 172.19.0.0/16
!
log stdout
'''

'''
! BGP configuration for R3
!
hostname R3bgp
password sdnip
!
router bgp 65003
	bgp router-id 172.24.0.2
	timers bgp 3 9
	neighbor 172.24.0.100 remote-as 65000
	neighbor 172.24.0.100 ebgp-multihop
	neighbor 172.24.0.100 timers connect 5
	neighbor 172.24.0.100 advertisement-interval 5
	network 172.20.0.0/16
!
log stdout
'''

'''
! BGP configuration for R4
!
hostname R4bgp
password sdnip
!
router bgp 65004
	bgp router-id 172.25.0.2
	timers bgp 3 9
	neighbor 172.25.0.100 remote-as 65000
	neighbor 172.25.0.100 ebgp-multihop
	neighbor 172.25.0.100 timers connect 5
	neighbor 172.25.0.100 advertisement-interval 5
	network 172.21.0.0/16
!
log stdout
'''
/etc/init.d/quagga restart





#Speaker
apt-get update
apt-get install -y vim quagga net-tools telnet
vim /etc/sysctl.conf
sysctl -p
ip addr add 172.22.0.100/16 dev eth1
ip addr add 172.23.0.100/16 dev eth1
ip addr add 172.24.0.100/16 dev eth1
ip addr add 172.25.0.100/16 dev eth1
ip addr show
vim /etc/quagga/daemons
vim /etc/quagga/zebra.conf
'''
hostname Speakerzebra
password sdnip
log stdout
'''
vim /etc/quagga/bgpd.conf
'''
hostname Speakerbgp
password sdnip
!
router bgp 65000
	bgp router-id 172.17.0.100
	timers bgp 3 9
	!
	neighbor 172.22.0.2 remote-as 65001
	neighbor 172.22.0.2 ebgp-multihop
	neighbor 172.22.0.2 timers connect 5
	neighbor 172.22.0.2 advertisement-interval 5
	!
	neighbor 172.23.0.2 remote-as 65002
	neighbor 172.23.0.2 ebgp-multihop
	neighbor 172.23.0.2 timers connect 5
	neighbor 172.23.0.2 advertisement-interval 5
	!
	neighbor 172.24.0.2 remote-as 65003
	neighbor 172.24.0.2 ebgp-multihop
	neighbor 172.24.0.2 timers connect 5
	neighbor 172.24.0.2 advertisement-interval 5
	!
	neighbor 172.25.0.2 remote-as 65004
	neighbor 172.25.0.2 ebgp-multihop
	neighbor 172.25.0.2 timers connect 5
	neighbor 172.25.0.2 advertisement-interval 5
	!
	! ONOS
	neighbor 172.17.0.2 remote-as 65000
	neighbor 172.17.0.2 port 2000
	neighbor 172.17.0.2 timers connect 5
'''
/etc/init.d/quagga restart
