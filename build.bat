@echo off
set "JAVA_HOME=D:\java\jdk-17.0.10+7"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using Java:
java -version
echo.
echo Starting Maven Build...
call "D:\java\apache-maven-3.9.12\bin\mvn.cmd" -s "D:\workspace\ERP\project-settings.xml" clean install -DskipTests -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true
