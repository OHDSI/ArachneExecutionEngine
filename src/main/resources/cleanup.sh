#!/usr/bin/env bash

WORKDIR=$1
sudo umount $WORKDIR/proc
sudo umount "$WORKDIR"_merged