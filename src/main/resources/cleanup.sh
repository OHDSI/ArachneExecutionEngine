#!/usr/bin/env bash

jail=$1
sudo umount $jail/proc
dirs=( bin boot dev etc home lib lib64 libs.r media mnt opt proc root run sbin srv sys tmp usr var .Rhistory )

for d in "${dirs[@]}"
do
    sudo rm -fr $jail/$d
done