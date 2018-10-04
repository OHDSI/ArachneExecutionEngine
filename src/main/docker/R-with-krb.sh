#!/usr/bin/env bash

KINIT_PARAMS=$1
ANALYSIS_FILE=$2
DBMS_PASSWORD=$3

if [ -n "$KINIT_PARAMS" ]
then
  if [[ "$KINIT_PARAMS" == *"keytab"* ]]; then
    kinit $KINIT_PARAMS
  else
    echo $DBMS_PASSWORD | $KINIT_PARAMS
  fi
fi

Rscript /$ANALYSIS_FILE