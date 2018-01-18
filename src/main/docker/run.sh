#!/usr/bin/env bash

Rscript /libs-r/$R_INSTALL_SCRIPT
java -Djava.security.egd=file:/dev/./urandom -jar /execution-engine.jar