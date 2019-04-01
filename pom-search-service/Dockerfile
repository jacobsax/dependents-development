FROM openjdk:11-stretch

RUN apt-get -y update
RUN apt-get -y install maven
RUN mvn --version

RUN apt-get -y install git
RUN git --version

RUN mkdir -p /app
COPY ./ /app/

WORKDIR /app

EXPOSE 8082

RUN mvn clean compile

CMD mvn exec:java