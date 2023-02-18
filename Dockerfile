FROM ubuntu
RUN apt update
RUN apt -y install maven \
    && RUN apt -y install openjdk-17-jdk \
    && RUN mvn -B package --file pom.xml \
    && RUN cd target \
    && RUN java -jar JavaIM-1.0-SNAPSHOT.jar
