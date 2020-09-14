# A JustAuth Proxy for keycloak

## Usage

* start a redis with docker

```bash
docker run -d  --name redis --restart=always \
    -v ~/data/redis:/bitnami \
    -e REDIS_PASSWORD=passwd \
    -p 6379:6379 \
bitnami/redis:5.0.8
```

* start keycloak justauth proxy wit docker

```docker run -d  --name justauth-proxy --restart=always \
   -e SPINRG_REDIS_HOST=redis \
    -e SPINRG_REDIS_PASSWORD=passwd \
    -e SPINRG_REDIS_PORT=6379 \
    -e SERVER_PORT=8080 \
    -p 8080:8080 \
    edgora/keycloak-justauth-proxy
```

## refer repositories

* [keycloak-proxy-idp](https://github.com/EdgoraCN/keycloak-proxy-idp)

* [JustAuth](https://gitee.com/yadong.zhang/JustAuth)
