### Strategus Modules Setup
install.packages("devtools")
install.packages("remotes")
install.packages("renv")
install.packages("keyring")
keyring::keyring_create("system", "strategus")
remotes::install_github("miraisolutions/godmode")
remotes::install_version("rlang", version = "1.1.1")
remotes::install_version("rJava", version = "1.0-6", type = "source")
remotes::install_github("ohdsi/CirceR", ref = "v1.3.0", upgrade = "never")
remotes::install_github("ohdsi/DatabaseConnector", ref="v6.2.1")
remotes::install_github("ohdsi/Strategus", version="develop")

library(Strategus)
# Download Modules Specification files
analysisSpecificationsUrl <- "https://raw.githubusercontent.com/OHDSI/Strategus/develop/inst/testdata/analysisSpecification.json"
destinationFile <- file.path(Sys.getenv("INSTANTIATED_MODULES_FOLDER"), "modulesSpecification.json")
download.file(analysisSpecificationsUrl, destinationFile)
# Read Modules Specification file
analysisSpecifications <- ParallelLogger::loadSettingsFromJson(destinationFile)
# Apply Modules Specification file to download all Modules
ensureAllModulesInstantiated(analysisSpecifications)

