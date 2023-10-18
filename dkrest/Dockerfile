ARG inferenceImg
# based on inference image for the nvidia packages
FROM ${inferenceImg}

RUN apt update && apt upgrade -y && apt install -y gstreamer1.0-tools gstreamer1.0-rtsp graphviz openjdk-17-jdk --autoremove && apt clean

ARG jar
COPY ${jar} /tmp/app.jar
ENTRYPOINT ["java","-jar","/tmp/app.jar"]

COPY src/main/resources /app/defkoi
RUN echo yes |keytool -import -cacerts -file /app/defkoi/ca.pem -alias DKRootCA -storepass changeit

COPY .djl.ai /root/.djl.ai
