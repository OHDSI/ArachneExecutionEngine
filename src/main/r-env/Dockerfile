FROM odysseusinc/r-java:3.5.0

RUN apt-get update && apt-get install -y r-cran-rodbc
RUN apt-get install -y libpq-dev 
RUN apt-get install -y build-essential 
RUN apt-get install -y git-all 
RUN apt-get install -y gcc 
RUN apt-get install -y make 
RUN apt-get install -y libcurl4-openssl-dev 
RUN apt-get install -y libssl-dev 
RUN apt-get install -y libxml2-dev curl libssh-dev libxml2-dev r-cran-xml libdigest-hmac-perl libcairo2-dev


COPY Python-2.7.3.tgz Python-2.7.3.tgz
RUN tar -xzf Python-2.7.3.tgz
RUN cd Python-2.7.3 && ./configure --prefix=/usr --enable-shared && make && make install 

RUN update-alternatives --install /usr/bin/python python /usr/bin/python2.7 10
RUN update-alternatives --set python /usr/bin/python2.7


COPY libs.r libs.r
RUN Rscript libs.r

WORKDIR "/workdir"
