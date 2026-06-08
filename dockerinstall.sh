apt update
apt -y install openjdk-17-jdk
cd JavaIM && sh build.sh
wget https://download.bell-sw.com/java/17.0.6+10/bellsoft-jre17.0.6+10-linux-amd64.deb && dpkg -i bellsoft-jre17.0.6+10-linux-amd64.deb
apt -y remove openjdk-17-jdk
