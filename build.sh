mvn clean package jib:dockerBuild
docker push  edgora/keycloak-justauth-proxy
docker tag  edgora/keycloak-justauth-proxy  registry.cn-beijing.aliyuncs.com/edgora-oss/keycloak-justauth-proxy
docker push registry.cn-beijing.aliyuncs.com/edgora-oss/keycloak-justauth-proxy