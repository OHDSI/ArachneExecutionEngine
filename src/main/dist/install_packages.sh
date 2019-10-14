#!/usr/bin/env bash

DIST=$1

HOME=/root

export LANG=en_US.UTF-8
export LANGUAGE=en_US:en
export LC_CTYPE="en_US.UTF-8"
export LC_NUMERIC="en_US.UTF-8"
export LC_TIME="en_US.UTF-8"
export LC_COLLATE="en_US.UTF-8"
export LC_MONETARY=en_US.UTF-8
export LC_MESSAGES="en_US.UTF-8"
export LC_PAPER=en_US.UTF-8
export LC_NAME=en_US.UTF-8
export LC_ADDRESS=en_US.UTF-8
export LC_TELEPHONE=en_US.UTF-8
export LC_MEASUREMENT=en_US.UTF-8
export LC_IDENTIFICATION=en_US.UTF-8
export LC_ALL=en_US.UTF-8

LANG=en_US.UTF-8 locale-gen --purge en_US.UTF-8
echo -e 'LANG="en_US.UTF-8"\nLANGUAGE="en_US:en"\nLC_ALL="en_US.UTF-8"' > /etc/default/locale
LC_ALL=en_US.UTF-8 dpkg-reconfigure -f noninteractive locales

apt-get install -y software-properties-common
sudo add-apt-repository "deb http://archive.ubuntu.com/ubuntu $(lsb_release -sc) main universe restricted"
add-apt-repository -y ppa:openjdk-r/ppa
apt-get update && apt-get install -y openjdk-8-jdk

rm -f /usr/bin/java
update-alternatives --config java

sudo add-apt-repository -y ppa:deadsnakes/ppa
apt-get update && apt-get install -y libpq-dev build-essential gcc make libcurl4-openssl-dev libssl-dev curl libssh-dev libxml2-dev libdigest-hmac-perl libcairo2-dev wget unzip apt-transport-https python-dev krb5-user python3.6 python3.6-dev virtualenv python3.6-venv libgeos-dev libprotobuf-dev protobuf-compiler

wget http://cdn.azul.com/zcek/bin/ZuluJCEPolicies.zip \
        && echo "8021a28b8cac41b44f1421fd210a0a0822fcaf88d62d2e70a35b2ff628a8675a  ZuluJCEPolicies.zip" | sha256sum -c - \
        && unzip -oj ZuluJCEPolicies.zip ZuluJCEPolicies/local_policy.jar -d /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/ \
        && unzip -oj ZuluJCEPolicies.zip ZuluJCEPolicies/US_export_policy.jar -d /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/ \
        && rm -f ZuluJCEPolicies.zip

# Redshift Certificate Authority Bundle
wget https://s3.amazonaws.com/redshift-downloads/redshift-keytool.jar && java -jar redshift-keytool.jar -s && rm -f redshift-keytool.jar

# Add jq JSON processor
add-apt-repository -y ppa:opencpu/jq
apt-get update
apt-get -y install libjq-dev

add-apt-repository "deb http://cran.rstudio.com/bin/linux/ubuntu $DIST/"
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E084DAB9
gpg -a --export E084DAB9 | sudo apt-key add -
#sudo sh -c 'echo "deb http://cran.rstudio.com/bin/linux/ubuntu $DIST/" >> /etc/apt/sources.list.d/rstudio.list'
#gpg --keyserver keyserver.ubuntu.com --recv-key E084DAB9
#gpg -a --export E084DAB9 | sudo apt-key add -

apt-get update && apt-get -y --allow-unauthenticated install r-base

cat >> /etc/R/Rprofile.site <<_EOF_
local({ 
  # add MASS to the default packages, set a CRAN mirror  
  old <- getOption("defaultPackages"); r <- getOption("repos") 
  r["CRAN"] <- "https://cran.cnr.berkeley.edu/"
  options(defaultPackages = c(old, "MASS"), repos = r) 
})
_EOF_

curl https://bootstrap.pypa.io/get-pip.py | sudo -H python3.6
rm -f /usr/bin/python3
ln -s /usr/bin/python3.6 /usr/bin/python3

python3 -m pip install --upgrade pip
python3 -m pip install -U NumPy
python3 -m pip install -U SciPy
python3 -m pip install -U scikit-learn
python3 -m pip install -U torch
python3 -m pip install --upgrade tensorflow
python3 -m pip install keras

export USESPECIALPYTHONVERSION=python3.6

R CMD javareconf
Rscript /root/libs/libs_1.r
Rscript /root/libs/libs_2.r
Rscript /root/libs/libs_3.r
Rscript /root/libs/libs_4.r
Rscript /root/libs/libs_5.r
Rscript /root/libs/libs_6.r