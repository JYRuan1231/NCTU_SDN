sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name ONOS onosproject/onos:2.2.0
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name OVS1 openshift/openvswitch:latest
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name OVS2 openshift/openvswitch:latest
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name OVS3 openshift/openvswitch:latest
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name OVS4 openshift/openvswitch:latest
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name h1 ubuntu:16.04
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name R1 ubuntu:16.04
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name h2 ubuntu:16.04
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name R2 ubuntu:16.04
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name h3 ubuntu:16.04
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name R3 ubuntu:16.04
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name h4 ubuntu:16.04
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name R4 ubuntu:16.04
sudo docker run --privileged --cap-add NET_ADMIN --cap-add NET_BROADCAST -d -it --name Speaker ubuntu:16.04

sudo docker start ONOS
sudo docker start OVS1
sudo docker start OVS2
sudo docker start OVS3
sudo docker start OVS4
sudo docker start h1
sudo docker start R1
sudo docker start h2
sudo docker start R2
sudo docker start h3
sudo docker start R3
sudo docker start h4
sudo docker start R4
sudo docker start Speaker

sudo docker network create R1h1br
sudo docker network connect R1h1br R1
sudo docker network connect R1h1br h1

sudo docker network create R2h2br
sudo docker network connect R2h2br R2
sudo docker network connect R2h2br h2

sudo docker network create R3h3br
sudo docker network connect R3h3br R3
sudo docker network connect R3h3br h3

sudo docker network create R4h4br
sudo docker network connect R4h4br R4
sudo docker network connect R4h4br h4


sudo docker network create R1OVS1br
sudo docker network connect R1OVS1br R1
sudo docker network connect R1OVS1br OVS1

sudo docker network create R2OVS2br
sudo docker network connect R2OVS2br R2
sudo docker network connect R2OVS2br OVS2

sudo docker network create R3OVS3br
sudo docker network connect R3OVS3br R3
sudo docker network connect R3OVS3br OVS3

sudo docker network create R4OVS4br
sudo docker network connect R4OVS4br R4
sudo docker network connect R4OVS4br OVS4


sudo docker network create OVS1OVS2br
sudo docker network connect OVS1OVS2br OVS1
sudo docker network connect OVS1OVS2br OVS2

sudo docker network create OVS2OVS3br
sudo docker network connect OVS2OVS3br OVS2
sudo docker network connect OVS2OVS3br OVS3

sudo docker network create OVS3OVS4br
sudo docker network connect OVS3OVS4br OVS3
sudo docker network connect OVS3OVS4br OVS4

sudo docker network create OVS4OVS1br
sudo docker network connect OVS4OVS1br OVS4
sudo docker network connect OVS4OVS1br OVS1


sudo docker network create ONOSbr
sudo docker network connect ONOSbr ONOS
sudo docker network connect ONOSbr Speaker
sudo docker network connect ONOSbr OVS1
