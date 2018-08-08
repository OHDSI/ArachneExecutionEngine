#!/usr/bin/env bash

RUN_KINIT="$1"
ANALYSIS_FILE=$2

kinit "$RUN_KINIT"
#kinit "$RUN_KINIT"

#kinit -k -t /etc/krb.keytab ohdsi@ODYSSEUSINC.COM
#kinit "-k -t /etc/krb.keytab ohdsi@ODYSSEUSINC.COM" >> /myoutput2.txt

echo $RUN_KINIT >> /myoutput2.txt
cat /etc/krb5.conf >> /myoutput2.txt
cat /etc/krb.keytab >> /myoutput2.txt
echo " KLIST OUTPUT :" >> /myoutput2.txt
klist >> /myoutput2.txt
Rscript /$ANALYSIS_FILE