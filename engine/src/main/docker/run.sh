#!/usr/bin/env bash

systemctl start rsyslog
systemctl start logrotate

java -Djava.security.egd=file:/dev/./urandom -jar /execution-engine.jar