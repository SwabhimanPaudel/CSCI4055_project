#!/bin/sh
set -eu
cd "$(dirname "$0")"

javac=javac
java=java
if [ -n "${JAVA_HOME:-}" ]; then
    javac="$JAVA_HOME/bin/javac"
    java="$JAVA_HOME/bin/java"
fi
if ! command -v "$javac" >/dev/null 2>&1 || ! command -v "$java" >/dev/null 2>&1; then
    echo "Install a JDK (Java 8 or later) and check JAVA_HOME or PATH." >&2
    exit 1
fi

driver="lib/mariadb-java-client-3.5.11.jar"
mkdir -p lib build
if [ ! -s "$driver" ]; then
    echo "Downloading the JDBC driver..."
    curl --fail --location --retry 2 --connect-timeout 15 \
        https://repo.maven.apache.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.11/mariadb-java-client-3.5.11.jar \
        --output "$driver.part"
    mv "$driver.part" "$driver"
fi

"$javac" -encoding UTF-8 -d build DatabaseConnection.java EmployeeSearch.java EmployeeSearchFrame.java
exec "$java" -cp 'build:lib/*' EmployeeSearchFrame
