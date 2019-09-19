#!/usr/bin/env bash

WORKDIR=$1
sudo umount $WORKDIR/proc
sudo umount "$WORKDIR"_merged
dirs=( bin boot dev etc home impala lib lib64 libs.r media mnt opt proc root run sbin srv sys tmp usr var .Rhistory )

for d in "${dirs[@]}"
do
    sudo rm -fr $jail/$d
done