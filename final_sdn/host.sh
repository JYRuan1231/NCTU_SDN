sudo docker exec -it hx bash

apt-get update
apt-get install -y iproute2 net-tools
ip route del default
ip route add default via 172.xx.0.2
route
