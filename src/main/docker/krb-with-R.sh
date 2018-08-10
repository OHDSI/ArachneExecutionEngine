#!/usr/bin/env bash

RUN_KINIT=$1
ANALYSIS_FILE=$2

if [ -n "$RUN_KINIT" ]
then
  kinit $RUN_KINIT
fi

Rscript /$ANALYSIS_FILE