install.packages("devtools")
library("devtools")

if ("assertthat" %in% installed.packages() == FALSE) {
    install_version("assertthat", version = "0.2.1", type = "source")
}
if ("AUC" %in% installed.packages() == FALSE) {
    install_version("AUC", version = "0.3.0", type = "source")
}
if ("base64enc" %in% installed.packages() == FALSE) {
    install_version("base64enc", version = "0.1-3", type = "source")
}
if ("BH" %in% installed.packages() == FALSE) {
    install_version("BH", version = "1.69.0-1", type = "source")
}
if ("bindr" %in% installed.packages() == FALSE) {
    install_version("bindr", version = "0.1.1", type = "source")
}
if ("bindrcpp" %in% installed.packages() == FALSE) {
    install_version("bindrcpp", version = "0.2.2", type = "source")
}
if ("bit" %in% installed.packages() == FALSE) {
    install_version("bit", version = "1.1-14", type = "source")
}
if ("bitops" %in% installed.packages() == FALSE) {
    install_version("bitops", version = "1.0-6", type = "source")
}
if ("cli" %in% installed.packages() == FALSE) {
    install_version("cli", version = "1.1.0", type = "source")
}
if ("colorspace" %in% installed.packages() == FALSE) {
    install_version("colorspace", version = "1.4-1", type = "source")
}
if ("crosstalk" %in% installed.packages() == FALSE) {
    install_version("crosstalk", version = "1.0.0", type = "source")
}
if ("curl" %in% installed.packages() == FALSE) {
    install_version("curl", version = "3.3", type = "source")
}
if ("data.table" %in% installed.packages() == FALSE) {
    install_version("data.table", version = "1.12.0", type = "source")
}
if ("DBI" %in% installed.packages() == FALSE) {
    install_version("DBI", version = "1.0.0", type = "source")
}
if ("diagram" %in% installed.packages() == FALSE) {
    install_version("diagram", version = "1.6.4", type = "source")
}
if ("dichromat" %in% installed.packages() == FALSE) {
    install_version("dichromat", version = "2.0-0", type = "source")
}
if ("digest" %in% installed.packages() == FALSE) {
    install_version("digest", version = "0.6.18", type = "source")
}
if ("dplyr" %in% installed.packages() == FALSE) {
    install_version("dplyr", version = "0.8.0.1", type = "source")
}
if ("DT" %in% installed.packages() == FALSE) {
    install_version("DT", version = "0.5", type = "source")
}
if ("evaluate" %in% installed.packages() == FALSE) {
    install_version("evaluate", version = "0.13", type = "source")
}
if ("fastmatch" %in% installed.packages() == FALSE) {
    install_version("fastmatch", version = "1.1-0", type = "source")
}
if ("ff" %in% installed.packages() == FALSE) {
    install_version("ff", version = "2.2-14", type = "source")
}
if ("ffbase" %in% installed.packages() == FALSE) {
    install_version("ffbase", version = "0.12.7", type = "source")
}
if ("formatR" %in% installed.packages() == FALSE) {
    install_version("formatR", version = "1.6", type = "source")
}
if ("futile.logger" %in% installed.packages() == FALSE) {
    install_version("futile.logger", version = "1.4.3", type = "source")
}
if ("futile.options" %in% installed.packages() == FALSE) {
    install_version("futile.options", version = "1.0.1", type = "source")
}
# gdtools was moved to archive and cannot be installed via install_version
if ("gdtools" %in% installed.packages() == FALSE){
    install_github("davidgohel/gdtools") # 0.1.8
}
if ("ggplot2" %in% installed.packages() == FALSE) {
    install_version("ggplot2", version = "3.1.0", type = "source")
}
if ("git2r" %in% installed.packages() == FALSE) {
    install_version("git2r", version = "0.24.0", type = "source")
}
if ("glue" %in% installed.packages() == FALSE) {
    install_version("glue", version = "1.3.0", type = "source")
}
if ("gridExtra" %in% installed.packages() == FALSE) {
    install_version("gridExtra", version = "2.3", type = "source")
}
if ("gtable" %in% installed.packages() == FALSE) {
    install_version("gtable", version = "0.3.0", type = "source")
}
if ("hexbin" %in% installed.packages() == FALSE) {
    install_version("hexbin", version = "1.27.2", type = "source")
}
if ("highr" %in% installed.packages() == FALSE) {
    install_version("highr", version = "0.8", type = "source")
}
if ("hms" %in% installed.packages() == FALSE) {
    install_version("hms", version = "0.4.2", type = "source")
}
if ("htmltools" %in% installed.packages() == FALSE) {
    install_version("htmltools", version = "0.3.6", type = "source")
}
if ("htmlwidgets" %in% installed.packages() == FALSE) {
    install_version("htmlwidgets", version = "1.3", type = "source")
}
if ("httpuv" %in% installed.packages() == FALSE) {
    install_version("httpuv", version = "1.4.5.1", type = "source")
}
if ("httr" %in% installed.packages() == FALSE) {
    install_version("httr", version = "1.4.0", type = "source")
}
if ("jsonlite" %in% installed.packages() == FALSE) {
    install_version("jsonlite", version = "1.6", type = "source")
}
if ("knitr" %in% installed.packages() == FALSE) {
    install_version("knitr", version = "1.21", type = "source")
}
if ("labeling" %in% installed.packages() == FALSE) {
    install_version("labeling", version = "0.3", type = "source")
}
if ("lambda.r" %in% installed.packages() == FALSE) {
    install_version("lambda.r", version = "1.2.3", type = "source")
}
if ("lazyeval" %in% installed.packages() == FALSE) {
    install_version("lazyeval", version = "0.2.2", type = "source")
}
if ("magrittr" %in% installed.packages() == FALSE) {
    install_version("magrittr", version = "1.5", type = "source")
}
if ("mailR" %in% installed.packages() == FALSE) {
    install_version("mailR", version = "0.4.1", type = "source")
}
if ("markdown" %in% installed.packages() == FALSE) {
    install_version("markdown", version = "0.9", type = "source")
}
if ("mime" %in% installed.packages() == FALSE) {
    install_version("mime", version = "0.6", type = "source")
}
if ("munsell" %in% installed.packages() == FALSE) {
    install_version("munsell", version = "0.5.0", type = "source")
}
if ("officer" %in% installed.packages() == FALSE) {
    install_version("officer", version = "0.3.3", type = "source")
}
if ("openssl" %in% installed.packages() == FALSE) {
    install_version("openssl", version = "1.2.1", type = "source")
}
if ("openxlsx" %in% installed.packages() == FALSE) {
    install_version("openxlsx", version = "4.1.0", type = "source")
}
if ("pack" %in% installed.packages() == FALSE) {
    install_version("pack", version = "0.1-1", type = "source")
}
if ("packrat" %in% installed.packages() == FALSE) {
    install_version("packrat", version = "0.5.0", type = "source")
}
if ("pkgconfig" %in% installed.packages() == FALSE) {
    install_version("pkgconfig", version = "2.0.2", type = "source")
}
if ("plogr" %in% installed.packages() == FALSE) {
    install_version("plogr", version = "0.2.0", type = "source")
}
if ("plotly" %in% installed.packages() == FALSE) {
    install_version("plotly", version = "4.8.0", type = "source")
}
if ("plyr" %in% installed.packages() == FALSE) {
    install_version("plyr", version = "1.8.4", type = "source")
}
if ("png" %in% installed.packages() == FALSE) {
    install_version("png", version = "0.1-7", type = "source")
}
if ("purrr" %in% installed.packages() == FALSE) {
    install_version("purrr", version = "0.3.2", type = "source")
}
if ("PythonInR" %in% installed.packages() == FALSE) {
    install_version("PythonInR", version = "0.1-6", type = "source")
}
if ("R.methodsS3" %in% installed.packages() == FALSE) {
    install_version("R.methodsS3", version = "1.7.1", type = "source")
}
if ("R.oo" %in% installed.packages() == FALSE) {
    install_version("R.oo", version = "1.21.0", type = "source")
}
if ("R.utils" %in% installed.packages() == FALSE) {
    install_version("R.utils", version = "2.5.0", type = "source")
}
if ("R6" %in% installed.packages() == FALSE) {
    install_version("R6", version = "2.2.2", type = "source")
}
if ("RColorBrewer" %in% installed.packages() == FALSE) {
    install_version("RColorBrewer", version = "1.1-2", type = "source")
}
if ("Rcpp" %in% installed.packages() == FALSE) {
    install_version("Rcpp", version = "1.0.1", type = "source")
}
if ("RcppEigen" %in% installed.packages() == FALSE) {
    install_version("RcppEigen", version = "0.3.3.5.0", type = "source")
}
if ("RcppParallel" %in% installed.packages() == FALSE) {
    install_version("RcppParallel", version = "4.4.2", type = "source")
}
if ("RCurl" %in% installed.packages() == FALSE) {
    install_version("RCurl", version = "1.95-4.12", type = "source")
}
if ("readr" %in% installed.packages() == FALSE) {
    install_version("readr", version = "1.3.1", type = "source")
}
# ReporteRsjars was moved to archive and cannot be installed via install_version.
# This library MUST be installed before "ReporteRs" library
if ("ReporteRsjars" %in% installed.packages() == FALSE){
    install_github("davidgohel/ReporteRsjars") # 0.0.4
}
if ("ReporteRs" %in% installed.packages() == FALSE) {
    install_version("ReporteRs", version = "0.8.10", type = "source")
}
if ("reshape2" %in% installed.packages() == FALSE) {
    install_version("reshape2", version = "1.4.3", type = "source")
}
if ("rJava" %in% installed.packages() == FALSE) {
    install_version("rJava", version = "0.9-11", type = "source")
}
if ("RJDBC" %in% installed.packages() == FALSE) {
    install_version("RJDBC", version = "0.2-7.1", type = "source")
}
if ("RJSONIO" %in% installed.packages() == FALSE) {
    install_version("RJSONIO", version = "1.3-1.1", type = "source")
}
if ("rlang" %in% installed.packages() == FALSE) {
    install_version("rlang", version = "0.3.3", type = "source")
}
if ("rstudioapi" %in% installed.packages() == FALSE) {
    install_version("rstudioapi", version = "0.9.0", type = "source")
}
if ("rvg" %in% installed.packages() == FALSE) {
    install_version("rvg", version = "0.1.9", type = "source")
}
if ("scales" %in% installed.packages() == FALSE) {
    install_version("scales", version = "1.0.0", type = "source")
}
if ("shape" %in% installed.packages() == FALSE) {
    install_version("shape", version = "1.4.4", type = "source")
}
if ("shiny" %in% installed.packages() == FALSE) {
    install_version("shiny", version = "1.2.0", type = "source")
}
if ("shinycssloaders" %in% installed.packages() == FALSE) {
    install_version("shinycssloaders", version = "0.2.0", type = "source")
}
if ("shinydashboard" %in% installed.packages() == FALSE) {
    install_version("shinydashboard", version = "0.7.1", type = "source")
}
if ("snow" %in% installed.packages() == FALSE) {
    install_version("snow", version = "0.4-3", type = "source")
}
if ("sourcetools" %in% installed.packages() == FALSE) {
    install_version("sourcetools", version = "0.1.7", type = "source")
}
if ("stringi" %in% installed.packages() == FALSE) {
    install_version("stringi", version = "1.3.1", type = "source")
}
if ("stringr" %in% installed.packages() == FALSE) {
    install_version("stringr", version = "1.4.0", type = "source")
}
if ("survAUC" %in% installed.packages() == FALSE) {
    install_version("survAUC", version = "1.0-5", type = "source")
}
if ("tibble" %in% installed.packages() == FALSE) {
    install_version("tibble", version = "2.1.1", type = "source")
}
if ("tidyr" %in% installed.packages() == FALSE) {
    install_version("tidyr", version = "0.8.2", type = "source")
}
if ("tidyselect" %in% installed.packages() == FALSE) {
    install_version("tidyselect", version = "0.2.5", type = "source")
}
if ("uuid" %in% installed.packages() == FALSE) {
    install_version("uuid", version = "0.1-2", type = "source")
}
if ("viridisLite" %in% installed.packages() == FALSE) {
    install_version("viridisLite", version = "0.3.0", type = "source")
}
if ("whisker" %in% installed.packages() == FALSE) {
    install_version("whisker", version = "0.4", type = "source")
}
if ("withr" %in% installed.packages() == FALSE) {
    install_version("withr", version = "2.1.2", type = "source")
}
if ("xgboost" %in% installed.packages() == FALSE) {
    install_version("xgboost", version = "0.81.0.1", type = "source")
}
if ("XML" %in% installed.packages() == FALSE) {
    install_version("XML", version = "3.98-1.17", type = "source")
}
if ("xml2" %in% installed.packages() == FALSE) {
    install_version("xml2", version = "1.2.0", type = "source")
}
if ("xtable" %in% installed.packages() == FALSE) {
    install_version("xtable", version = "1.8-3", type = "source")
}
if ("yaml" %in% installed.packages() == FALSE) {
    install_version("yaml", version = "2.2.0", type = "source")
}
if ("zip" %in% installed.packages() == FALSE) {
    install_version("zip", version = "2.0.0", type = "source")
}

if ("drat" %in% installed.packages() == FALSE) {
    install_version("drat", version = "0.1.5", type = "source")
}

drat::addRepo(c("OHDSI", "cloudyr"))

if ("CohortMethod" %in% installed.packages() == FALSE) {
    install_github("OHDSI/CohortMethod", ref = "v3.0.2")
}
if ("DatabaseConnector" %in% installed.packages() == FALSE) {
    install_github("OHDSI/DatabaseConnector", ref = "v2.3.0")
}
if ("EmpiricalCalibration" %in% installed.packages() == FALSE) {
    install_github("OHDSI/EmpiricalCalibration", ref = "v1.4.0")
}
if ("FeatureExtraction" %in% installed.packages() == FALSE) {
    install_github("OHDSI/FeatureExtraction", ref = "v2.2.2")
}
if ("OhdsiRTools" %in% installed.packages() == FALSE) {
    install_github("OHDSI/OhdsiRTools", ref = "v1.5.5")
}
if ("SqlRender" %in% installed.packages() == FALSE) {
    install_github("OHDSI/SqlRender", ref = "v1.6.0")
}
if ("Cyclops" %in% installed.packages() == FALSE) {
    install_github("OHDSI/Cyclops", ref = "v2.0.2")
}
if ("OhdsiSharing" %in% installed.packages() == FALSE) {
    install_github("OHDSI/OhdsiSharing", ref = "v0.1.3")
}
if ("MethodEvaluation" %in% installed.packages() == FALSE) {
    install_github("OHDSI/MethodEvaluation", ref = "v1.0.1")
}
if ("PatientLevelPrediction" %in% installed.packages() == FALSE) {
    install_github("OHDSI/PatientLevelPrediction", ref = "v3.0.1")
}
