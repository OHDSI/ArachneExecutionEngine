#!/usr/bin/env bash

#JAIL=$1
#DIST_ARCHIVE=$2
#sudo tar xzf $DIST_ARCHIVE -C $JAIL
#sudo cp $KRB_CONF $JAIL/etc/krb5.conf
#sudo cp $KRB_KEYTAB $JAIL/etc/krb.keytab
#sudo cp /etc/krb-with-R.sh $JAIL/etc/krb-with-R.sh
#sudo chroot $JAIL /usr/bin/env -i bash etc/krb-with-R.sh "kinit -k -t krb.keytab ohdsi@ODYSSEUSINC.COM"
sudo tar xzf /home/aklochkova/docker/r_base_xenial_amd64.tar.gz -C /home/aklochkova/docker/tmp/
sudo cp /home/aklochkova/docker/krb5.conf /home/aklochkova/docker/tmp/etc/krb5.conf
sudo cp /home/aklochkova/docker/ohdsi.keytab /home/aklochkova/docker/tmp/etc/krb.keytab
sudo cp /home/aklochkova/docker/krb-with-R.sh /home/aklochkova/docker/tmp/etc/krb-with-R.sh
sudo chroot /home/aklochkova/docker/tmp /usr/bin/env -i bash etc/krb-with-R.sh "kinit -k -t krb.keytab ohdsi@ODYSSEUSINC.COM"