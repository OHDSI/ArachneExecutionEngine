FROM eclipse-temurin:8-jre
LABEL maintainer="Odysseus Data Services, Inc: support@odysseusinc.com"
VOLUME /tmp
VOLUME /libs-r
VOLUME /dist
ADD krb5.conf /etc/
ADD R-with-krb.sh /etc/
RUN apt-get update && apt-get upgrade -y && DEBIAN_FRONTEND=noninteractive apt-get install -yq sudo unzip krb5-user dos2unix cron rsyslog logrotate systemctl
RUN rm -f /usr/bin/java && java -version
RUN ln -fs /usr/share/zoneinfo/Etc/GMT /etc/localtime && dpkg-reconfigure -f noninteractive tzdata
RUN update-rc.d cron defaults && systemctl enable rsyslog
ENV R_INSTALL_SCRIPT=libs.r
ADD execution-engine-exec.jar /execution-engine.jar
RUN mkdir /impala
ADD Cloudera_ImpalaJDBC4_2.5.41.zip /impala/impala-jdbc.zip
RUN cd /impala && unzip impala-jdbc.zip && rm -f impala-jdbc.zip
EXPOSE 8888
COPY run.sh /run.sh
RUN dos2unix /run.sh && chmod +x /run.sh
RUN apt-get remove -y unzip \
    && rm -rf /tmp/* \
    && apt-get autoremove -y \
    && apt-get autoclean -y \
    && rm -rf /var/lib/apt/lists/* \
ENTRYPOINT ["/run.sh"]
