#!/usr/bin/env bash

#JAIL=/tmp/jail-dir
WORKDIR=$1
#rm -fr $WORKDIR/*
ANALYSIS_FILE=$2 #time.R

R_DIST_DIR=$3

# sudo tar xzf $DIST_ARCHIVE -C $WORKDIR

export R_HOME=/usr/lib/R

sudo mkdir $WORKDIR/etc
sudo mkdir $WORKDIR/impala

sudo cp /etc/resolv.conf $WORKDIR/etc/resolv.conf

if [ -n "$KRB_CONF" ]
then
  sudo cp $KRB_CONF $WORKDIR/etc/krb5.conf
fi
if [ -n "$KRB_KEYTAB" ]
then
  sudo cp $KRB_KEYTAB $WORKDIR/etc/krb.keytab
fi
if [ -n "$BQ_KEYFILE" ]
then
  sudo mkdir -p $(dirname $WORKDIR/$BQ_KEYFILE)
  sudo cp $BQ_KEYFILE $WORKDIR/$BQ_KEYFILE
fi

sudo cp /etc/R-with-krb.sh $WORKDIR/etc/R-with-krb.sh
sudo cp -R /impala/. $WORKDIR/impala/
sudo chmod +x $WORKDIR/etc/R-with-krb.sh

sudo echo -e "#!/usr/bin/env bash \n \
    export DATA_SOURCE_NAME=\"$DATA_SOURCE_NAME\" \n \
    export DBMS_USERNAME=\"$DBMS_USERNAME\" \n \
    export DBMS_PASSWORD=\"$DBMS_PASSWORD\" \n \
    export DBMS_TYPE=\"$DBMS_TYPE\" \n \
    export CONNECTION_STRING=\"$CONNECTION_STRING\" \n \
    export DBMS_SCHEMA=\"$DBMS_SCHEMA\" \n \
    export TARGET_SCHEMA=\"$TARGET_SCHEMA\" \n \
    export RESULT_SCHEMA=\"$RESULT_SCHEMA\" \n \
    export COHORT_TARGET_TABLE=\"$COHORT_TARGET_TABLE\" \n \
    export ANALYSIS_ID=\"$ANALYSIS_ID\" \n \
    export PATH=\"$PATH\" \n \
    export HOME=\"$HOME\" \n \
    export JDBC_DRIVER_PATH=\"$JDBC_DRIVER_PATH\" \n \
    $(tail --lines=+2 $WORKDIR/etc/R-with-krb.sh)" | sudo tee $WORKDIR/etc/R-with-krb.sh > /dev/null

mkdir "$WORKDIR"_overlay_work
mkdir "$WORKDIR"_overlay_merged
sudo mount -t overlay overlay -olowerdir="$R_DIST_DIR",upperdir="$WORKDIR",workdir="$WORKDIR"_overlay_work "$WORKDIR"_overlay_merged

sudo unshare --fork --pid -- chroot "$WORKDIR"_overlay_merged /bin/bash -c " \
    mount -t proc proc /proc && \
    ./etc/R-with-krb.sh \"$KINIT_PARAMS\" \"$ANALYSIS_FILE\" \"$KRB_PASSWORD\" \
"
