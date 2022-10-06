install.packages("devtools", repos='http://cran.us.r-project.org', type='source')
library("devtools")
message('rJava')
install.packages("rJava", repos='http://cran.us.r-project.org')
library("rJava")
install.packages("drat", repos='http://cran.us.r-project.org')
drat::addRepo("OHDSI")
install.packages("SqlRender", repos='http://cran.us.r-project.org')
install_github("ohdsi/DatabaseConnector")
install.packages("RPostgreSQL", repos='http://cran.us.r-project.org')
install.packages("stringi", repos='http://cran.us.r-project.org')

message("Cyclops")
install_github("ohdsi/Cyclops")
message("DBI")
install.packages("DBI", repos='http://cran.us.r-project.org')
install.packages("fastmatch", repos='http://cran.us.r-project.org')
#install.packages("aws.s3")
install.packages("aws.s3", repos = "http://cloudyr.github.io/drat")
install_github("ohdsi/FeatureExtraction")
install_github("ohdsi/OhdsiRTools")
install_github("ohdsi/CohortMethod")
install_github("ohdsi/OhdsiSharing")
drat::addRepo(c("OHDSI","cloudyr"))
install_github("OHDSI/Achilles")
install_github("OHDSI/OhdsiRTools")
install_github("OHDSI/FeatureExtraction")
install_github("OHDSI/PatientLevelPrediction")
install_github("OHDSI/CohortMethod")
install_github("OHDSI/PublicOracle")
