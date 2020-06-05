#!/usr/bin/env bash

jail=$1
sudo umount $jail/proc
dirs=( bin bigquery boot dev etc home hive impala lib lib64 libs.r media mnt netezza opt proc root run sbin srv sys tmp usr var .Rhistory )

for d in "${dirs[@]}"
do
    sudo rm -fr $jail/$d
done