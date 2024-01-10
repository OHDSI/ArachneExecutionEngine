# Ubuntu 22.04
FROM --platform=linux/amd64 rocker/r-ver:4.2
MAINTAINER support@odysseusinc.com

# Install system dependencies
RUN apt-get -y update && apt-get install -y \
   default-jdk \
   r-cran-rjava \
   sudo \
   libssl-dev \
   libsodium-dev \
   libsecret-1-dev \
   libxml2-dev \
   libcurl4-openssl-dev \
   cmake \
   curl \
   git \
   libffi-dev \
   python3-dev \
   python3.venv \
   && apt-get clean \
   && rm -rf /var/lib/apt/lists/ \
   && sudo R CMD javareconf

RUN install2.r --error rJava \
&& rm -rf /tmp/download_packages/ /tmp/*.rds

RUN install2.r --error DatabaseConnector \
&& rm -rf /tmp/download_packages/ /tmp/*.rds

ENV DATABASECONNECTOR_JAR_FOLDER="/opt/hades/jdbc_drivers"
RUN R -e "DatabaseConnector::downloadJdbcDrivers('all');"

RUN install2.r --error Andromeda \
&& rm -rf /tmp/download_packages/ /tmp/*.rds

RUN install2.r --error openssl httr xml2 remotes renv \
    && rm -rf /tmp/download_packages/ /tmp/*.rds

RUN echo 'options(repos = c(CRAN = "https://packagemanager.posit.co/cran/2023-11-01"))' >>"${R_HOME}/etc/Rprofile.site"
RUN echo "DATABASECONNECTOR_JAR_FOLDER=/opt/hades/jdbc_drivers" >> /usr/local/lib/R/etc/Renviron
RUN echo "RENV_PATHS_CELLAR=/opt/renv_cellar" >> /usr/local/lib/R/etc/Renviron

WORKDIR /code

COPY renv.lock /code/

# Install Hades packages
RUN install2.r --error renv remotes keyring && rm -rf /tmp/download_packages/ /tmp/*.rds
RUN R -e "remotes::install_github('ohdsi/Strategus', 'develop')"
RUN R -e "renv::restore()"

# Install Python
RUN R -e "reticulate::install_miniconda()"
RUN R -e "reticulate::install_python(version = '3.10')"

# Create Python virtual environment used by the OHDSI PatientLevelPrediction R package
ENV WORKON_HOME="/opt/.virtualenvs"
RUN R -e "PatientLevelPrediction::configurePython(envname='r-reticulate', envtype='python')"

# Install Strategus modules
RUN R -e "install.packages('keyring')"
RUN R -e "keyring::keyring_create('system', 'strategus')"
RUN mkdir /strategus
RUN echo "INSTANTIATED_MODULES_FOLDER=/strategus" >>/usr/local/lib/R/etc/Renviron
RUN echo "STRATEGUS_KEYRING_PASSWORD=strategus" >> /usr/local/lib/R/etc/Renviron
RUN R -e 'Sys.getenv("INSTANTIATED_MODULES_FOLDER")'
RUN R <<EOF
library(Strategus)
# Download Modules Specification files
analysisSpecificationsUrl <- "https://raw.githubusercontent.com/OHDSI/Strategus/develop/inst/testdata/analysisSpecification.json"
destinationFile <- file.path(Sys.getenv("INSTANTIATED_MODULES_FOLDER"), "modulesSpecification.json")
download.file(analysisSpecificationsUrl, destinationFile)
# Read Modules Specification file
analysisSpecifications <- ParallelLogger::loadSettingsFromJson(destinationFile)
# Apply Modules Specification file to download all Modules
ensureAllModulesInstantiated(analysisSpecifications)
EOF

RUN mkdir /results

CMD bash


