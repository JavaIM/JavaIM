FROM ubuntu
RUN apt update
RUN apt -y install git
RUN apt -y install wget
RUN apt -y install openjdk-17-jdk
RUN apt -y install maven
RUN git clone https://github.com/QiLechan/JavaIM.git
RUN cd JavaIM && sh build.sh
RUN wget https://download.bell-sw.com/java/17.0.6+10/bellsoft-jre17.0.6+10-linux-amd64.deb && dpkg -i bellsoft-jre17.0.6+10-linux-amd64.deb
RUN apt -y remove maven
RUN apt -y remove openjdk-17-jdk
