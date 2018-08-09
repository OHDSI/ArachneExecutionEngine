#!/usr/bin/env bash

RUN_KINIT=$1
ANALYSIS_FILE=$2

kinit $RUN_KINIT
Rscript /$ANALYSIS_FILE