FROM ubuntu
RUN apt update && apt install git && apt install wget && git clone https://github.com/QiLechan/JavaIM.git && cd JavaIM && sh dockerinstall.sh
