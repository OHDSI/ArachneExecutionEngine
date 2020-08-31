#!/usr/bin/env bash

# DIST - Ubuntu Dist
# CRAN_URL - CRAN Mirror (eg https://cran.asia/)

DIST=$1
CRAN_URL=$2

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

apt install -y software-properties-common
sudo add-apt-repository "deb http://archive.ubuntu.com/ubuntu $(lsb_release -sc) main universe restricted"
add-apt-repository -y ppa:openjdk-r/ppa
apt update && apt install -y openjdk-8-jdk

rm -f /usr/bin/java
update-alternatives --config java

# Doesn't work for Bionic
#sudo add-apt-repository -y ppa:deadsnakes/ppa
apt update && apt install -yf libpq-dev build-essential gcc make libcurl4-openssl-dev libssl-dev curl libssh-dev libxml2-dev libdigest-hmac-perl libcairo2-dev wget unzip apt-transport-https python-dev krb5-user virtualenv libgeos-dev libprotobuf-dev protobuf-compiler

wget http://cdn.azul.com/zcek/bin/ZuluJCEPolicies.zip \
        && echo "8021a28b8cac41b44f1421fd210a0a0822fcaf88d62d2e70a35b2ff628a8675a  ZuluJCEPolicies.zip" | sha256sum -c - \
        && unzip -oj ZuluJCEPolicies.zip ZuluJCEPolicies/local_policy.jar -d /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/ \
        && unzip -oj ZuluJCEPolicies.zip ZuluJCEPolicies/US_export_policy.jar -d /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/ \
        && rm -f ZuluJCEPolicies.zip

# Redshift Certificate Authority Bundle
wget https://s3.amazonaws.com/redshift-downloads/redshift-keytool.jar && java -jar redshift-keytool.jar -s && rm -f redshift-keytool.jar

# Add jq JSON processor
#add-apt-repository -y ppa:opencpu/jq
apt update & apt -y install libjq-dev

apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E298A3A825C0D65DFD57CBB651716619E084DAB9
add-apt-repository "deb https://cloud.r-project.org/bin/linux/ubuntu $DIST/"
#gpg -a --export E298A3A825C0D65DFD57CBB651716619E084DAB9 | sudo apt-key add -
#sudo sh -c 'echo "deb http://cran.rstudio.com/bin/linux/ubuntu $DIST/" >> /etc/apt/sources.list.d/rstudio.list'
#gpg --keyserver keyserver.ubuntu.com --recv-key E084DAB9
#gpg -a --export E084DAB9 | sudo apt-key add -

apt update && apt -y --allow-unauthenticated install r-base r-base-dev

cat >> /etc/R/Rprofile.site <<_EOF_
local({ 
  # add MASS to the default packages, set a CRAN mirror  
  old <- getOption("defaultPackages"); r <- getOption("repos") 
  r["CRAN"] <- "$CRAN_URL"
  options(defaultPackages = c(old, "MASS"), repos = r) 
})
_EOF_

# PYTHON 3.8
if [[ "$DIST" == "bionic" ]]; then
  sudo wget https://www.python.org/ftp/python/3.8.5/Python-3.8.5.tgz
  sudo tar xzf Python-3.8.5.tgz
  cd Python-3.8.5 || exit 1
  sudo ./configure --enable-optimizations
  sudo make install
  cd .. && rm -fr Python-3.8.5

  curl https://bootstrap.pypa.io/get-pip.py | sudo -H python3.8
  rm -f /usr/bin/python3
  ln -s /usr/local/bin/python3.8 /usr/bin/python3
elif [[ "$DIST" == "xenial" ]]; then
  sudo add-apt-repository -y ppa:deadsnakes/ppa
  sudo apt update && apt -y install python3.8 python3.8-dev python3.8-venv
#else
  # For ubuntu focal (20.04) python3.8 is default
  # sudo apt update && apt -y install python3 python3-dev python3-venv python3-pip
fi

# Miniconda for Python 3.8
# Using 4.5.12 since latest version failed during installation
# https://github.com/conda/conda/issues/10143
wget https://repo.anaconda.com/miniconda/Miniconda3-4.5.12-Linux-x86_64.sh

if [ "$(sha256sum Miniconda3-4.5.12-Linux-x86_64.sh)" -ne "866ae9dff53ad0874e1d1a60b1ad1ef8" ]; then
  echo "Miniconda checksum failed, try again"
  exit 1
fi
bash Miniconda3-4.5.12-Linux-x86_64.sh -b -p /root/miniconda -f || exit 1
echo 'export PATH=$PATH:/root/miniconda/bin' >> /root/.bashrc

export PATH=$PATH:/root/miniconda/bin
conda create -y -n PLP python=3.8.3
conda install -y -n PLP -c sebp scikit-survival=0.12.0
conda install -y -n PLP -c pytorch pytorch torchvision

R CMD javareconf
Rscript /root/libs/libs_1.r
Rscript /root/libs/libs_2.r
Rscript /root/libs/libs_3.r
Rscript /root/libs/libs_4.r
Rscript /root/libs/libs_5.r
Rscript /root/libs/libs_6.r