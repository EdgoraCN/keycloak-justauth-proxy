#!/bin/sh

echo "The application will start now"

exec java ${JAVA_OPTS} -noverify -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom -cp /app/resources/:/app/classes/:/app/libs/* "com.edgora.idp.ProxyIdpApplication"  "$@"
