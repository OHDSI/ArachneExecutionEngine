FROM odysseusinc/r-env:1.0.15
MAINTAINER support@odysseusinc.com
VOLUME /tmp
VOLUME /libs-r
VOLUME /dist
ADD krb5.conf /etc/
ADD R-with-krb.sh /etc/
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -yq sudo krb5-user dos2unix cron rsyslog logrotate
RUN rm -f /usr/bin/java && update-alternatives --config java
RUN ln -fs /usr/share/zoneinfo/Etc/GMT /etc/localtime && dpkg-reconfigure -f noninteractive tzdata
RUN update-rc.d cron defaults && update-rc.d rsyslog defaults
ENV R_INSTALL_SCRIPT=libs.r
ADD execution-engine-exec.jar /execution-engine.jar
RUN mkdir /impala
ADD Cloudera_ImpalaJDBC4_2.5.41.zip /impala/impala-jdbc.zip
RUN cd /impala && unzip impala-jdbc.zip && rm -f impala-jdbc.zip
EXPOSE 8888
COPY run.sh /run.sh
RUN dos2unix /run.sh
RUN chmod +x /run.sh
CMD ["/run.sh"]
