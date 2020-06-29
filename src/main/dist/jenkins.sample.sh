mkdir -p $WORKSPACE/src/main/extras/impala
cp /var/local/drivers/impala/*.jar $WORKSPACE/src/main/extras/impala/
mkdir -p $WORKSPACE/src/main/extras/netezza
cp /var/local/drivers/netezza/*.jar $WORKSPACE/src/main/extras/netezza/
mkdir -p $WORKSPACE/src/main/extras/bigquery
cp /var/local/drivers/bigquery/*.jar $WORKSPACE/src/main/extras/bigquery/
cd $WORKSPACE/src/main/dist
sudo /bin/bash run_build.sh -d xenial