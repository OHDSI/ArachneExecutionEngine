#!/usr/bin/env bash

#JAIL=/tmp/jail-dir
JAIL=$1
#rm -fr $JAIL/*
ANALYSIS_FILE=$2 #time.R

DIST_ARCHIVE=$3

sudo tar xzf $DIST_ARCHIVE -C $JAIL

export R_HOME=/usr/lib/R
sudo cp /etc/resolv.conf $JAIL/etc/resolv.conf

if [ -n "$KRB_CONF" ]
then
  sudo cp $KRB_CONF $JAIL/etc/krb5.conf
fi
if [ -n "$KRB_KEYTAB" ]
then
  sudo cp $KRB_KEYTAB $JAIL/etc/krb.keytab
fi
if [ -n "$BQ_KEYFILE" ]
then
  sudo mkdir -p $(dirname $JAIL/$BQ_KEYFILE)
  sudo cp $BQ_KEYFILE $JAIL/$BQ_KEYFILE
fi

sudo cp /etc/R-with-krb.sh $JAIL/etc/R-with-krb.sh
sudo cp -R /impala/. $JAIL/impala/
sudo chmod +x $JAIL/etc/R-with-krb.sh

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
    $(tail --lines=+2 $JAIL/etc/R-with-krb.sh)" | sudo tee $JAIL/etc/R-with-krb.sh > /dev/null

sudo unshare --fork --pid -- chroot $JAIL /bin/bash -c " \
    mount -t proc proc /proc && \
    ./etc/R-with-krb.sh \"$KINIT_PARAMS\" \"$ANALYSIS_FILE\" \"$KRB_PASSWORD\" \
"
