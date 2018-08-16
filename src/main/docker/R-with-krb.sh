#!/usr/bin/env bash

KINIT_PARAMS=$1
ANALYSIS_FILE=$2

if [ -n "$KINIT_PARAMS" ]
then
  kinit $KINIT_PARAMS
fi

Rscript /$ANALYSIS_FILE