#!/usr/bin/env bash

RUN_KINIT=$1
ANALYSIS_FILE=$2

kinit $RUN_KINIT
echo "KLIST OUTPUT: " >> /myoutput.txt
klist >> /myoutput.txt
Rscript /$ANALYSIS_FILE