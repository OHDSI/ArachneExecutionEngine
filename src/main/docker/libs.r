library(devtools)

install_version("drat", version="0.1.3", type="source")
drat::addRepo(c("OHDSI","cloudyr"))

install_github("odysseusinc/FeatureExtraction", ref="v2.1.2")
installed.packages('DBI')
install_github("OHDSI/DatabaseConnector") # master
install_github("OHDSI/SqlRender", ref="v1.5.3")
