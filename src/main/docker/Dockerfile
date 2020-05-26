FROM odysseusinc/r-env:1.0.13
MAINTAINER alexandr.ryabokon@odysseusinc.com
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

#Copy drivers
RUN mkdir /impala
ADD extras/impala /impala/
RUN mkdir /bigquery
ADD extras/bigquery /bigquery/
RUN mkdir /netezza
ADD extras/netezza /netezza/

EXPOSE 8888
COPY run.sh /run.sh
RUN dos2unix /run.sh
RUN chmod +x /run.sh
CMD ["/run.sh"]