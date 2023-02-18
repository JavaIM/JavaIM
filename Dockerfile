FROM ubuntu
RUN apt update
RUN apt -y install git
RUN apt -y install openjdk-17-jdk
RUN apt -y install maven
RUN git clone https://github.com/QiLechan/JavaIM.git
RUN cd JavaIM && sh build.sh
