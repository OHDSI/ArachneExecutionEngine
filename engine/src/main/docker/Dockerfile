FROM eclipse-temurin:8-jdk
LABEL maintainer="Odysseus Data Services, Inc: support@odysseusinc.com"
VOLUME /tmp
VOLUME /libs-r
ADD krb5.conf /etc/
ADD R-with-krb.sh /etc/
RUN apt-get update && apt-get upgrade -y && DEBIAN_FRONTEND=noninteractive apt-get install -yq sudo unzip krb5-user dos2unix cron rsyslog logrotate systemctl
RUN rm -f /usr/bin/java && java -version
RUN ln -fs /usr/share/zoneinfo/Etc/GMT /etc/localtime && dpkg-reconfigure -f noninteractive tzdata
RUN update-rc.d cron defaults && systemctl enable rsyslog
ENV R_INSTALL_SCRIPT=libs.r
ADD execution-engine-exec.jar /execution-engine.jar
RUN mkdir /runtimes
COPY descriptor_base.json /runtimes/descriptor_base.json
RUN ls -la /runtimes/*
COPY run.sh /run.sh
RUN dos2unix /run.sh && chmod +x /run.sh
RUN apt-get remove -y unzip \
    && rm -rf /tmp/* \
    && apt-get autoremove -y \
    && apt-get autoclean -y \
    && rm -rf /var/lib/apt/lists/*
EXPOSE 8888
CMD ["/run.sh"]
