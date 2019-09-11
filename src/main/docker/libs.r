library(devtools)
drat::addRepo(c("OHDSI","cloudyr"))

install_github("OHDSI/CohortMethod", ref="v3.0.2")
install.packages('DBI')
install_github("OHDSI/DatabaseConnector") # ref="v2.3.0"
install_github("OHDSI/EmpiricalCalibration", ref="v1.4.0")
install_github("odysseusinc/FeatureExtraction", ref="v2.2.2")
if ("OhdsiRTools" %in% installed.packages() == FALSE){
  install_github("OHDSI/OhdsiRTools", ref="v1.5.5")
}
if ("SqlRender" %in% installed.packages() == FALSE){
  install_github("OHDSI/SqlRender", ref="v1.6.0")
}
if ("Cyclops" %in% installed.packages() == FALSE){
  install_github("OHDSI/Cyclops", ref="v2.0.2")
}
if ("OhdsiSharing" %in% installed.packages() == FALSE){
  install_github("OHDSI/OhdsiSharing", ref="v0.1.3")
}
if ("MethodEvaluation" %in% installed.packages() == FALSE){
  install_github("OHDSI/MethodEvaluation", ref="v1.0.1")
}
if ("PatientLevelPrediction" %in% installed.packages() == FALSE){
  install_github("OHDSI/PatientLevelPrediction", ref="v3.0.1")
}
