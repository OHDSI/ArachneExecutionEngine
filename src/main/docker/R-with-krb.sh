#!/usr/bin/env bash

KINIT_PARAMS=$1
ANALYSIS_FILE=$2
KRB_PASSWORD=$3


if [ -n "$KINIT_PARAMS" ]
then
  if [[ "$KINIT_PARAMS" == *"keytab"* ]]; then
    echo "kinit $KINIT_PARAMS"
    kinit $KINIT_PARAMS
  else
    echo $KRB_PASSWORD | $KINIT_PARAMS
  fi
fi

echo "before rscript"
Rscript /$ANALYSIS_FILE