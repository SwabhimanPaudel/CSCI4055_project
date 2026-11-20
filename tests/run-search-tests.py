#!/usr/bin/env python3
"""Compile and check Search against a disposable, separate local MySQL server."""
import os
from pathlib import Path
import socket
import subprocess
import sys
import tempfile
import time


def main():
    if os.geteuid() == 0:
        raise RuntimeError("Run these tests without sudo.")
    if len(sys.argv) != 2:
        raise RuntimeError("Usage: python3 tests/run-search-tests.py PATH_TO_CONNECTOR_JAR")
    root = Path(__file__).resolve().parent.parent
    jar = Path(sys.argv[1]).resolve()
    if not jar.is_file():
            raise RuntimeError("Pass the path to the MariaDB Connector/J JAR described in README.md.")
    java_home = Path(os.environ.get("JAVA_HOME",
                    "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"))
    mysql_home = Path(os.environ.get("MYSQL_HOME", "/usr/local/mysql"))
    javac = java_home / "bin/javac"
    java = java_home / "bin/java"
    mysqld = mysql_home / "bin/mysqld"
    for executable in (javac, java, mysqld):
        if not executable.is_file():
            raise RuntimeError("Missing " + str(executable) + "; set JAVA_HOME or MYSQL_HOME.")

    parent = "/private/tmp" if Path("/private/tmp").is_dir() else None
    with tempfile.TemporaryDirectory(prefix="employee-search-test-", dir=parent) as directory:
        work = Path(directory)
        classes = work / "classes"
        classes.mkdir()
        subprocess.run([str(javac), "--release", "8", "-Xlint:all", "-d", str(classes),
                        str(root / "DatabaseConnection.java"),
                        str(root / "EmployeeSearch.java"), str(root / "EmployeeSearchFrame.java"),
                        str(root / "tests/EmployeeSearchTest.java"),
                        str(root / "tests/IntegratedFrameTest.java")], check=True)
        data = work / "data"
        data.mkdir()
        # Initialization is restricted to this newly created, empty test directory.
        assert data.parent == work and not any(data.iterdir())
        common = [str(mysqld), "--no-defaults", "--basedir=" + str(mysql_home),
                  "--datadir=" + str(data), "--mysqlx=OFF"]
        log = work / "server.log"
        print("Initializing a separate temporary MySQL test database...", flush=True)
        initialized = subprocess.run(common + ["--initialize-insecure", "--log-error=" + str(log)],
                                     stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                     text=True, timeout=60)
        if initialized.returncode:
            raise RuntimeError("Test initialization failed:\n" + initialized.stdout
                               + (log.read_text() if log.exists() else ""))
        with socket.socket() as available:
            available.bind(("127.0.0.1", 0))
            port = available.getsockname()[1]
        server = subprocess.Popen(common + ["--bind-address=127.0.0.1", "--port=" + str(port),
                                  "--socket=" + str(work / "mysql.sock"),
                                  "--pid-file=" + str(work / "mysql.pid"),
                                  "--log-error=" + str(log), "--skip-log-bin"],
                                  stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        try:
            deadline = time.monotonic() + 40
            while True:
                if server.poll() is not None or time.monotonic() >= deadline:
                    raise RuntimeError("Temporary server startup failed:\n" + log.read_text())
                try:
                    with socket.create_connection(("127.0.0.1", port), timeout=0.5):
                        break
                except OSError:
                    time.sleep(0.2)
            # These are synthetic credentials for this disposable server only.
            password = "Fixture-only-database-password-49!"
            subprocess.run([str(mysql_home / "bin/mysql"), "--no-defaults", "--user=root",
                            "--protocol=SOCKET", "--socket=" + str(work / "mysql.sock"),
                            "--execute=ALTER USER 'root'@'localhost' IDENTIFIED BY '" + password + "';"],
                           check=True, timeout=10, stdout=subprocess.DEVNULL)
            environment = dict(os.environ, DB_HOST="127.0.0.1", DB_PORT=str(port),
                               DB_USER="root", DB_PASSWORD=password, DB_SSL_MODE="trust")
            print("Running Search and integrated GUI checks on the temporary server...", flush=True)
            subprocess.run([str(java), "-cp", str(classes) + os.pathsep + str(jar),
                            "EmployeeSearchTest", "jdbc:mariadb://127.0.0.1:" + str(port)
                            + "/?connectTimeout=3000&sslMode=trust"],
                           env=environment, check=True, timeout=90)
        finally:
            if server.poll() is None:
                server.terminate()
            try:
                server.wait(timeout=45)
            except subprocess.TimeoutExpired:
                # This exact child owns only disposable test data, never COMPANY.
                server.kill()
                server.wait(timeout=10)
        print("Temporary MySQL stopped; test files removed on exit.", flush=True)


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, subprocess.SubprocessError) as error:
        print("Search tests failed: " + str(error), file=sys.stderr)
        sys.exit(1)
