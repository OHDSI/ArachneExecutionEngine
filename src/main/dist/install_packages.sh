#!/usr/bin/env bash

DIST=$1

HOME=/root

locale-gen --purge en_US.UTF-8
echo -e 'LANG="en_US.UTF-8"\nLANGUAGE="en_US:en"\n' > /etc/default/locale

apt-get install -y software-properties-common
add-apt-repository -y ppa:openjdk-r/ppa
apt-get update && apt-get install -y openjdk-8-jdk

rm -f /usr/bin/java
update-alternatives --config java

apt-get install -y libpq-dev build-essential gcc make libcurl4-openssl-dev libssl-dev curl libssh-dev libxml2-dev libdigest-hmac-perl libcairo2-dev wget unzip apt-transport-https python-dev krb5-user

wget http://cdn.azul.com/zcek/bin/ZuluJCEPolicies.zip \
        && echo "8021a28b8cac41b44f1421fd210a0a0822fcaf88d62d2e70a35b2ff628a8675a  ZuluJCEPolicies.zip" | sha256sum -c - \
        && unzip -oj ZuluJCEPolicies.zip ZuluJCEPolicies/local_policy.jar -d /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/ \
        && unzip -oj ZuluJCEPolicies.zip ZuluJCEPolicies/US_export_policy.jar -d /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/ \
        && rm -f ZuluJCEPolicies.zip

# Redshift Certificate Authority Bundle
wget https://s3.amazonaws.com/redshift-downloads/redshift-keytool.jar && java -jar redshift-keytool.jar -s && rm -f redshift-keytool.jar

add-apt-repository "deb http://cran.rstudio.com/bin/linux/ubuntu $DIST/"
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E084DAB9
gpg -a --export E084DAB9 | sudo apt-key add -
#sudo sh -c 'echo "deb http://cran.rstudio.com/bin/linux/ubuntu $DIST/" >> /etc/apt/sources.list.d/rstudio.list'
#gpg --keyserver keyserver.ubuntu.com --recv-key E084DAB9
#gpg -a --export E084DAB9 | sudo apt-key add -

apt-get update && apt-get -y install r-base

cat >> /etc/R/Rprofile.site <<_EOF_
local({ 
  # add MASS to the default packages, set a CRAN mirror  
  old <- getOption("defaultPackages"); r <- getOption("repos") 
  r["CRAN"] <- "https://cran.cnr.berkeley.edu/"
  options(defaultPackages = c(old, "MASS"), repos = r) 
})
_EOF_

R CMD javareconf
Rscript /root/libs.r