FROM hub.arachnenetwork.com/r-env:1.0.3
MAINTAINER alexandr.ryabokon@odysseusinc.com
VOLUME /tmp
VOLUME /libs-r
ENV R_INSTALL_SCRIPT=libs.r
ADD execution-engine.jar /execution-engine.jar
RUN mkdir /impala
ADD Cloudera_ImpalaJDBC4_2.5.41.zip /impala/impala-jdbc.zip
RUN cd /impala && unzip impala-jdbc.zip && rm -f impala-jdbc.zip
EXPOSE 8888
COPY run.sh /run.sh
RUN chmod +x /run.sh
CMD ["/run.sh"]