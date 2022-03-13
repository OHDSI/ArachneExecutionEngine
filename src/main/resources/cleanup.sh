#!/usr/bin/env bash

jail=$1
sudo umount $jail/proc
dirs=( bin bigquery boot dev etc home hive impala lib lib32 libx32 lib64 libs.r media mnt mssql netezza opt oracle postgresql proc redshift root run sbin srv sys tmp usr var .Rhistory )

for d in "${dirs[@]}"
do
    sudo rm -fr $jail/$d
done