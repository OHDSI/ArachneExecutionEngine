#!/usr/bin/env bash

WORKDIR=$1

sudo umount $WORKDIR/proc
sudo umount "$WORKDIR"_merged
# TODO
# sudo rm -rf "$WORKDIR"_overlay_work
# sudo rm -rf "$WORKDIR"_overlay_merged

dirs=( bin boot dev etc home impala lib lib64 libs.r media mnt opt proc root run sbin srv sys tmp usr var .Rhistory )

for d in "${dirs[@]}"
do
    sudo rm -fr $WORKDIR/$d
done