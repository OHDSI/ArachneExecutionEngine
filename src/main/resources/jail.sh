#!/usr/bin/env bash

echo ` df -h`

#JAIL=/tmp/jail-dir
JAIL=$1
#rm -fr $JAIL/*
ANALYSIS_FILE=$2 #time.R

DIST_ARCHIVE=$3

echo "jail $JAIL"
echo "jail $ANALYSIS_FILE "
echo "jail $DIST_ARCHIVE "

sudo tar xzf $DIST_ARCHIVE -C $JAIL

export R_HOME=/usr/lib/R
#sudo mkdir $JAIL/etc/

sudo cp /etc/resolv.conf $JAIL/etc/resolv.conf

echo "jail coped  $JAIL/etc/resolv.conf"

if [ -n "$KRB_CONF" ]
then
  echo "jail $KRB_CONF"
  sudo cp $KRB_CONF $JAIL/etc/krb5.conf
  echo "jail $KRB_CONF $JAIL/etc/krb5.conf"
fi
if [ -n "$KRB_KEYTAB" ]
then
  echo "jail $KRB_KEYTAB"
  sudo cp $KRB_KEYTAB $JAIL/etc/krb.keytab
  echo "jail $KRB_KEYTAB $JAIL/etc/krb.keytab"
fi
if [ -n "$BQ_KEYFILE" ]
then
  echo "jail $BQ_KEYFILE"
  sudo mkdir -p $(dirname $JAIL/$BQ_KEYFILE)
  sudo cp $BQ_KEYFILE $JAIL/$BQ_KEYFILE
  echo "jail $BQ_KEYFILE $JAIL/$BQ_KEYFILE"
fi

sudo cp /etc/R-with-krb.sh $JAIL/etc/R-with-krb.sh
echo "jail /impala/. $JAIL/impala/"
sudo cp -R /impala/. $JAIL/impala/
echo "jail $BQ_KEYFILE"
sudo chmod +x $JAIL/etc/R-with-krb.sh
echo "jail $JAIL/etc/R-with-krb.sh"

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

echo "jail echo "

echo "---------------------------------------"
echo "DEBUG_0"
lsblk

#dmesg
echo "---------------------------------------"
sudo unshare -r --fork --pid -- chroot $JAIL /bin/bash -c " \
    mount -t proc proc /proc && \
    ./etc/R-with-krb.sh \"$KINIT_PARAMS\" \"$ANALYSIS_FILE\" \"$KRB_PASSWORD\" \
"

if [ $? -ne 0 ]
  echo exit code $?
fi

echo $?
echo "---------------------------------------"
echo "DEBUG_1"
lsblk
#dmesg
echo "---------------------------------------"

echo "jail ushare "

