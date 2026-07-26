@echo off
setlocal

set "APP_HOME=%~dp0"
set "GRADLE_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar"

if exist "%GRADLE_JAR%" (
  if exist "%JAVA_HOME%\bin\java.exe" (
    "%JAVA_HOME%\bin\java.exe" -classpath "%GRADLE_JAR%" org.gradle.wrapper.GradleWrapperMain %*
    exit /b %ERRORLEVEL%
  )
)

if exist "C:\Users\tanko\.gradle\wrapper\dists\gradle-9.1.0-all\7wzd0jkjit61aq2p43wpjgij9\gradle-9.1.0\bin\gradle.bat" (
  "C:\Users\tanko\.gradle\wrapper\dists\gradle-9.1.0-all\7wzd0jkjit61aq2p43wpjgij9\gradle-9.1.0\bin\gradle.bat" %*
  exit /b %ERRORLEVEL%
)

where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle wrapper and Gradle executable were not found. >&2
  exit /b 1
)

gradle %*
exit /b %ERRORLEVEL%

