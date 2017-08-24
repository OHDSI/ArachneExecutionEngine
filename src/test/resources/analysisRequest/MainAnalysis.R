###########################################################
# R script for creating SQL files (and sending the SQL    # 
# commands to the server) for the treatment pattern       #
# studies for these diseases:                             #
# - Hypertension (HTN)                                    #
# - Type 2 Diabetes (T2DM)                                #
# - Depression                                            #
#                                                         #
# Requires: R and Java 1.6 or higher                      #
###########################################################

# Install necessary packages if needed
#install.packages("devtools")
#library(devtools)
#install_github("ohdsi/SqlRender")
#install_github("ohdsi/DatabaseConnector")
#install.packages("RPostgreSQL")

# Load libraries
library(SqlRender)
library(DatabaseConnector)
library(RODBC)

###########################################################
# Parameters: Please change these to the correct values:  #
###########################################################

folder        = "." # Folder containing the R and SQL files, use forward slashes
minCellCount  = 1   # the smallest allowable cell count, 1 means all counts are allowed
cdmSchema     = "public"
resultsSchema = "public"
sourceName    = "study_test"
dbms          = Sys.getenv("DBMS_TYPE") # Should be "sql server", "oracle", "postgresql" or "redshift"

# If you want to use R to run the SQL and extract the results tables, please create a connectionDetails 
# object. See ?createConnectionDetails for details on how to configure for your DBMS.



user <- Sys.getenv("DBMS_USERNAME")
pw <- Sys.getenv("DBMS_PASSWORD")
server <- Sys.getenv("DBMS_URL")
port <- Sys.getenv("DBMS_PORT")

connectionDetails <- createConnectionDetails(dbms=dbms,
server=server,
port=port,
user=user,
password=pw,
schema=cdmSchema)


###########################################################
# End of parameters. Make no changes after this           #
###########################################################

setwd(folder)

source("HelperFunctions.R")

# Create the parameterized SQL files:
htnSqlFile <- renderStudySpecificSql("HTN",minCellCount,cdmSchema,resultsSchema,sourceName,dbms)
t2dmSqlFile <- renderStudySpecificSql("T2DM",minCellCount,cdmSchema,resultsSchema,sourceName,dbms)
depSqlFile <- renderStudySpecificSql("Depression",minCellCount,cdmSchema,resultsSchema,sourceName,dbms)

# Execute the SQL:
conn <- connect(connectionDetails)
executeSql(conn,readSql(htnSqlFile))
executeSql(conn,readSql(t2dmSqlFile))
executeSql(conn,readSql(depSqlFile))

# Extract tables to CSV files:
extractAndWriteToFile(conn, "summary", resultsSchema, sourceName, "HTN", dbms)
extractAndWriteToFile(conn, "person_cnt", resultsSchema, sourceName, "HTN", dbms)
extractAndWriteToFile(conn, "seq_cnt", resultsSchema, sourceName, "HTN", dbms)

extractAndWriteToFile(conn, "summary", resultsSchema, sourceName, "T2DM", dbms)
extractAndWriteToFile(conn, "person_cnt", resultsSchema, sourceName, "T2DM", dbms)
extractAndWriteToFile(conn, "seq_cnt", resultsSchema, sourceName, "T2DM", dbms)

extractAndWriteToFile(conn, "summary", resultsSchema, sourceName, "Depression", dbms)
extractAndWriteToFile(conn, "person_cnt", resultsSchema, sourceName, "Depression", dbms)
extractAndWriteToFile(conn, "seq_cnt", resultsSchema, sourceName, "Depression", dbms)

dbDisconnect(conn)


