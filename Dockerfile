FROM ubuntu
RUN apt install wget && wget https://raw.githubusercontent.com/QiLechan/JavaIM/main/dockerinstall.sh && sh dockerinstall.sh
