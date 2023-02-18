FROM ubuntu
RUN apt install wget \
    && RUN apt install maven \
    && RUN apt install openjdk-17-jdk \
    && RUN mvn -B package --file pom.xml \
    && RUN cd target \
    && RUN java -jar JavaIM-1.0-SNAPSHOT.jar
