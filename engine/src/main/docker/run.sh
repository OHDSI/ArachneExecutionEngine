#!/usr/bin/env bash

systemctl start rsyslog
systemctl start logrotate
service cron start

#
# Kerberos authentication scheduling
#
if [[ -n "${KRB5_USER}" ]]; then
    echo "Kerberos init"
    sed -i 's/\(default_realm = \).*/\1'"$KRB5_REALM"'/' /etc/krb5.conf
    sed -i '/\[realms\]/a '"$KRB5_REALM"' = {\n\t\tkdc = '"$KRB5_KDC"'\n\t\tadmin_server = '"$KRB5_ADMIN_SERVER"'\n\t}' /etc/krb5.conf
    sed -i '/PATH/a MAILTO=""' /etc/crontab

    KRB_REFRESH="/bin/krb-refresh"
    CRON_EXPR="*/5 * * * *  root  $KRB_REFRESH 2>&1 >/dev/null | /usr/bin/logger -t kerberos"

    if [[ -n "${KRB5_PASSWORD}" ]]; then
        echo "${KRB5_PASSWORD}" | kinit "${KRB5_USER}"
        echo "#!/usr/bin/env bash
        echo ${KRB5_PASSWORD} | kinit ${KRB5_USER}" > $KRB_REFRESH
    elif [[ -n "${KRB5_KEYTAB}" ]]; then
        kinit -k -t "${KRB5_KEYTAB}" "${KRB5_USER}"
        echo "#!/usr/bin/env bash
        kinit -k -t ${KRB5_KEYTAB} ${KRB5_USER}" > $KRB_REFRESH
    else
        echo "Either KRB5_PASSWORD or KRB5_KEYTAB is required"
        exit 1
    fi
    chmod a+x $KRB_REFRESH
    grep -q -F "$CRON_EXPR" /etc/crontab || echo "$CRON_EXPR" >> /etc/crontab
fi

java --add-opens=java.base/java.net=ALL-UNNAMED -Djava.security.egd=file:/dev/./urandom -jar /execution-engine.jar
