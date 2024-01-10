# Ubuntu 22.04
FROM --platform=linux/amd64 rocker/rstudio:4.2

# Install system dependencies
RUN apt-get -y update && apt-get install -y \
   default-jdk \
   r-cran-rjava \
   sudo \
   vim \
   libxml2-dev \
   libssl-dev \
   libcurl4-openssl-dev \
   libpq-dev \
   curl \
   && apt-get clean \
   && rm -rf /var/lib/apt/lists/ \
   && sudo R CMD javareconf

RUN install2.r --error rJava \
&& rm -rf /tmp/download_packages/ /tmp/*.rds

RUN install2.r --error DatabaseConnector \
&& rm -rf /tmp/download_packages/ /tmp/*.rds

RUN mkdir -p /opt/hades/jdbc_drivers
RUN echo "DATABASECONNECTOR_JAR_FOLDER=/opt/hades/jdbc_drivers" >> /usr/local/lib/R/etc/Renviron
RUN echo "RENV_PATHS_CELLAR=/opt/renv_cellar" >> /usr/local/lib/R/etc/Renviron
ENV DATABASECONNECTOR_JAR_FOLDER="/opt/hades/jdbc_drivers"
RUN R -e "DatabaseConnector::downloadJdbcDrivers('all');"

RUN install2.r --error Andromeda \
&& rm -rf /tmp/download_packages/ /tmp/*.rds

RUN install2.r --error openssl httr xml2 remotes \
    && rm -rf /tmp/download_packages/ /tmp/*.rds

RUN install2.r --error odbc RPostgres duckdb \
   && rm -rf /tmp/downloaded_packages/ /tmp/*.rds

RUN echo 'options(repos = c(CRAN = "https://packagemanager.posit.co/cran/2023-11-01"))' >>"${R_HOME}/etc/Rprofile.site"

RUN install2.r --error here log4r testthat \
&& rm -rf /tmp/download_packages/ /tmp/*.rds

# Install OHDSI R packages
RUN R -e "remotes::install_github('ohdsi/CirceR')"
RUN R -e "remotes::install_github('ohdsi/Hades')"
RUN R -e "remotes::install_github('ohdsi/Achilles')"
RUN R -e "remotes::install_github('ohdsi/DataQualityDashboard')"

# disable rstudio login by default
ARG DISABLE_AUTH=true

RUN mkdir /results
WORKDIR /code

CMD bash