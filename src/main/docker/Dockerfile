FROM hub.arachnenetwork.com/r-env:1.0.2
MAINTAINER alexandr.ryabokon@odysseusinc.com
VOLUME /tmp
ADD execution-engine.jar /execution-engine.jar
EXPOSE 8888
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/execution-engine.jar"]