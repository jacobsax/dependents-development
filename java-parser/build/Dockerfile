FROM openjdk:11-stretch

WORKDIR /java-parser

RUN apt-get update
RUN apt-get install -y maven git
RUN mvn --version
RUN git --version

COPY ./tmp ./

RUN mvn clean package

