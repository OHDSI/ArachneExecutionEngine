FROM hub.arachnenetwork.com/r-env:1.0.3
MAINTAINER alexandr.ryabokon@odysseusinc.com
VOLUME /tmp
ADD execution-engine.jar /execution-engine.jar
RUN mkdir /impala
ADD Cloudera_ImpalaJDBC4_2.5.41.zip /impala/impala-jdbc.zip
RUN cd /impala && unzip impala-jdbc.zip && rm -f impala-jdbc.zip
EXPOSE 8888
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/execution-engine.jar"]