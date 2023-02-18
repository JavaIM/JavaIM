FROM ubuntu
RUN apt update && apt -y install wget && wget https://raw.githubusercontent.com/QiLechan/JavaIM/main/dockerinstall.sh && sh dockerinstall.sh
