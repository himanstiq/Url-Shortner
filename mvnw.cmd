@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------
@echo off
@setlocal

set MAVEN_PROJECTBASEDIR=%~dp0

@REM Find JAVA_HOME
if not "%JAVA_HOME%"=="" goto OkJHome
echo Error: JAVA_HOME not found in your environment. >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
goto error

:OkJHome
if exist "%JAVA_HOME%\bin\java.exe" goto init
echo Error: JAVA_HOME is set to an invalid directory: %JAVA_HOME% >&2
goto error

:init
set MAVEN_WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set MAVEN_WRAPPER_DOWNLOADER="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\MavenWrapperDownloader.java"
set DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

@REM Download maven-wrapper.jar if missing
if exist %MAVEN_WRAPPER_JAR% goto runMaven
echo Downloading Maven Wrapper...
"%JAVA_HOME%\bin\java.exe" -cp "" ^
  -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperDownloader ^
  "%DOWNLOAD_URL%" %MAVEN_WRAPPER_JAR%

:runMaven
"%JAVA_HOME%\bin\java.exe" ^
  -classpath %MAVEN_WRAPPER_JAR% ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
exit /B %ERROR_CODE%
