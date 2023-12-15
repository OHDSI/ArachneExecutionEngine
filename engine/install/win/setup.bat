@echo off
setlocal

set WGET_CMD=bin\wget.exe
set UNZIP_CMD=bin\unzip.exe

set TEMP_DIR=temp
set ANACONDA_URL=https://repo.continuum.io/archive/.winzip/Anaconda2-4.4.0-Windows-x86_64.zip
set ANACONDA_ARCHIVE=Anaconda2-4.4.0-Windows-x86_64.zip
set ANACONDA_INSTALLER=Anaconda2-4.4.0-Windows-x86_64.exe
set RCRAN_URL=https://cran.r-project.org/bin/windows/base/R-3.3.2-win.exe
set RCRAN_INSTALLER=R-3.3.2-win.exe
set RTOOLS_URL=https://cran.r-project.org/bin/windows/Rtools/Rtools33.exe
set RTOOLS_INSTALLER=Rtools33.exe
set JDK_INSTALLER=jdk\jdk-8u141-windows-x64.exe
set JDK_HOMEDIR=c:\java\jdk_1.8u141

rem Prepare working dir, etc.
cd /d %~dp0

mkdir %TEMP_DIR%
mkdir %JDK_HOMEDIR%

rem Downloads section

echo "Downloading Anaconda"
%WGET_CMD% -nc --no-check-certificate -P %TEMP_DIR% %ANACONDA_URL%
%UNZIP_CMD% -o %TEMP_DIR%\%ANACONDA_ARCHIVE% -d %TEMP_DIR%

echo "Downloading R"
%WGET_CMD% -nc --no-check-certificate -P %TEMP_DIR% %RCRAN_URL%

echo "Downloading RTools"
%WGET_CMD% -nc --no-check-certificate -P %TEMP_DIR% %RTOOLS_URL%

rem installation section

echo "Installing Java"
%JDK_INSTALLER% /s ADDLOCAL="ToolsFeature,PublicjreFeature" INSTALLDIR=%JDK_HOMEDIR% /INSTALLDIRPUBJRE=%JDK_HOMEDIR%\jre

setx -m JAVA_HOME %JDK_HOMEDIR%
setx -m R_HOME "c:\Program Files\R\R-3.3.2"
setx PATH %PATH%;%JDK_HOMEDIR%\jre\bin\server;%R_HOME%\bin
set R_HOME=c:\Program Files\R\R-3.3.2
path %PATH%;%JDK_HOMEDIR%\jre\bin\server;%R_HOME%\bin

echo "Running Anaconda installation"
%TEMP_DIR%\%ANACONDA_INSTALLER%

echo "Running R installation"
%TEMP_DIR%\%RCRAN_INSTALLER%

%TEMP_DIR%\%RTOOLS_INSTALLER%

echo "Installing R packages"
for /R "libs" %%S IN (*.r) DO RScript %%S

rem Cleanup
for /R "%TEMP_DIR%" %%I IN (*) DO del %%I /s/q