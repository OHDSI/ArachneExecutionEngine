FROM hub.arachnenetwork.com/study
MAINTAINER alexandr.ryabokon@odysseusinc.com
VOLUME /tmp
ADD execution-engine.jar /execution-engine.jar
EXPOSE 8888
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/execution-engine.jar"]