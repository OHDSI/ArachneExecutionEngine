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

#sudo cp -R /impala/. $JAIL/impala/
CHROOT_DEF=$JAIL/etc/R-with-krb.sh
sudo touch $CHROOT_DEF
printf "#!/usr/bin/env bash\n " | sudo tee -a $CHROOT_DEF > /dev/null
printf "	export DATA_SOURCE_NAME=%q \n" "${DATA_SOURCE_NAME}" | sudo tee -a $CHROOT_DEF > /dev/null
printf "	export DBMS_USERNAME=%q \n" "${DBMS_USERNAME}" | sudo tee -a $CHROOT_DEF  > /dev/null
printf "	export DBMS_PASSWORD=%q \n" "${DBMS_PASSWORD}" | sudo tee -a $CHROOT_DEF  > /dev/null
printf "	export DBMS_TYPE=%q \n" "${DBMS_TYPE}" | sudo tee -a $CHROOT_DEF  > /dev/null
printf "	export CONNECTION_STRING=%q \n" "${CONNECTION_STRING}" | sudo tee -a $CHROOT_DEF  > /dev/null
printf "	export DBMS_SCHEMA=%q \n" "${DBMS_SCHEMA}" | sudo tee -a $CHROOT_DEF  > /dev/null
printf "	export TARGET_SCHEMA=%q \n" "${TARGET_SCHEMA}" | sudo tee -a $CHROOT_DEF  > /dev/null
printf "	export RESULT_SCHEMA=%q \n" "${RESULT_SCHEMA}" | sudo tee -a $CHROOT_DEF  > /dev/null
printf "	export COHORT_TARGET_TABLE=%q \n" "${COHORT_TARGET_TABLE}" | sudo tee -a $CHROOT_DEF  > /dev/null
printf "	export ANALYSIS_ID=%q \n" "${ANALYSIS_ID}" | sudo tee -a $CHROOT_DEF  > /dev/null
printf "	export PATH=%q \n" "${PATH}" | sudo tee -a $CHROOT_DEF  > /dev/null
printf "	export HOME=%q \n" "${HOME}" | sudo tee -a $CHROOT_DEF  > /dev/null
printf "	export JDBC_DRIVER_PATH=%q \n" "${JDBC_DRIVER_PATH}" | sudo tee -a $CHROOT_DEF  > /dev/null
sudo tail --lines=+2 /etc/R-with-krb.sh  | sudo tee -a $CHROOT_DEF  > /dev/null

sudo chmod +x $CHROOT_DEF

sudo unshare --fork --pid -- chroot $JAIL /bin/bash -c " \
    mount -t proc proc /proc && \
    ./etc/R-with-krb.sh \"$KINIT_PARAMS\" \"$ANALYSIS_FILE\" \"$KRB_PASSWORD\" \
"
