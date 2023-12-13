mkdir -p $WORKSPACE/src/main/extras/impala
cp /var/local/drivers/impala/*.jar $WORKSPACE/src/main/extras/impala/
mkdir -p $WORKSPACE/src/main/extras/netezza
cp /var/local/drivers/netezza/*.jar $WORKSPACE/src/main/extras/netezza/
mkdir -p $WORKSPACE/src/main/extras/bigquery
cp /var/local/drivers/bigquery/*.jar $WORKSPACE/src/main/extras/bigquery/
mkdir -p $WORKSPACE/src/main/extras/postgresql
cp /var/local/drivers/postgresql/*.jar $WORKSPACE/src/main/extras/postgresql/
mkdir -p $WORKSPACE/src/main/extras/redshift
cp /var/local/drivers/redshift/*.jar $WORKSPACE/src/main/extras/redshift/
mkdir -p $WORKSPACE/src/main/extras/mssql
cp /var/local/drivers/mssql/*.jar $WORKSPACE/src/main/extras/mssql/
mkdir -p $WORKSPACE/src/main/extras/oracle
cp /var/local/drivers/oracle/*.jar $WORKSPACE/src/main/extras/oracle/
cd $WORKSPACE/src/main/dist
sudo /bin/bash run_build.sh -d xenial