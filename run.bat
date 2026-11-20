@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"
if errorlevel 1 exit /b 1

set "JAVAC=javac"
set "JAVA=java"
if defined JAVA_HOME (
    set "JAVAC=%JAVA_HOME%\bin\javac.exe"
    set "JAVA=%JAVA_HOME%\bin\java.exe"
)
"%JAVAC%" -version >nul 2>&1
if errorlevel 1 (
    echo Install a JDK ^(Java 8 or later^) and check JAVA_HOME or PATH.
    exit /b 1
)
"%JAVA%" -version >nul 2>&1
if errorlevel 1 exit /b 1

if not exist lib mkdir lib
if not exist build mkdir build
if not exist "lib\mariadb-java-client-3.5.11.jar" (
    echo Downloading the JDBC driver...
    powershell.exe -NoLogo -NoProfile -NonInteractive -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.11/mariadb-java-client-3.5.11.jar' -OutFile 'lib\mariadb-java-client-3.5.11.jar.part'; Move-Item -Force -LiteralPath 'lib\mariadb-java-client-3.5.11.jar.part' -Destination 'lib\mariadb-java-client-3.5.11.jar'"
    if errorlevel 1 exit /b 1
)

"%JAVAC%" -encoding UTF-8 -d build DatabaseConnection.java EmployeeSearch.java EmployeeSearchFrame.java
if errorlevel 1 exit /b 1
"%JAVA%" -cp "build;lib/*" EmployeeSearchFrame
exit /b %errorlevel%
