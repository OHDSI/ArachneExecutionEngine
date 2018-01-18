library(devtools)

install_version("drat", version="0.1.3", type="source")
drat::addRepo(c("OHDSI","cloudyr"))

install_github("odysseusinc/FeatureExtraction", ref="v1.2.3-develop") # v1.2.3-develop
install_github("OHDSI/SqlRender", ref="#f1a029c") # 1.4.8-SNAPSHOT
