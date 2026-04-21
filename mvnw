#!/bin/sh
# Maven Wrapper Shell Script
MAVEN_PROJECTBASEDIR=$(dirname "$(readlink -f "$0")" 2>/dev/null || dirname "$0")
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

# Download if missing
if [ ! -e "$MAVEN_WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper..."
    if command -v curl > /dev/null; then
        curl -o "$MAVEN_WRAPPER_JAR" "$DOWNLOAD_URL"
    elif command -v wget > /dev/null; then
        wget -O "$MAVEN_WRAPPER_JAR" "$DOWNLOAD_URL"
    fi
fi

exec "$JAVA_HOME/bin/java" \
    -classpath "$MAVEN_WRAPPER_JAR" \
    "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
    org.apache.maven.wrapper.MavenWrapperMain "$@"
