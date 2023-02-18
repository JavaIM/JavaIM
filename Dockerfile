FROM ubuntu
RUN apt update && apt -y install git && apt -y install wget && git clone https://github.com/QiLechan/JavaIM.git && cd JavaIM && sh dockerinstall.sh
