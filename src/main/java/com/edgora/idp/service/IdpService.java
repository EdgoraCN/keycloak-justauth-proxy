package com.edgora.idp.service;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSONObject;
import com.edgora.idp.cache.AuthStateRedisCache;
import com.edgora.idp.custom.AuthMyGitlabRequest;
import com.edgora.idp.dto.ProxyIdpConfig;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.xkcoding.http.config.HttpConfig;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthConfig.AuthConfigBuilder;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.request.AuthAlipayRequest;
import me.zhyd.oauth.request.AuthAliyunRequest;
import me.zhyd.oauth.request.AuthBaiduRequest;
import me.zhyd.oauth.request.AuthCodingRequest;
import me.zhyd.oauth.request.AuthCsdnRequest;
import me.zhyd.oauth.request.AuthDingTalkRequest;
import me.zhyd.oauth.request.AuthDouyinRequest;
import me.zhyd.oauth.request.AuthElemeRequest;
import me.zhyd.oauth.request.AuthFacebookRequest;
import me.zhyd.oauth.request.AuthGiteeRequest;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.request.AuthGitlabRequest;
import me.zhyd.oauth.request.AuthGoogleRequest;
import me.zhyd.oauth.request.AuthHuaweiRequest;
import me.zhyd.oauth.request.AuthKujialeRequest;
import me.zhyd.oauth.request.AuthLinkedinRequest;
import me.zhyd.oauth.request.AuthMeituanRequest;
import me.zhyd.oauth.request.AuthMiRequest;
import me.zhyd.oauth.request.AuthMicrosoftRequest;
import me.zhyd.oauth.request.AuthOschinaRequest;
import me.zhyd.oauth.request.AuthPinterestRequest;
import me.zhyd.oauth.request.AuthQqRequest;
import me.zhyd.oauth.request.AuthRenrenRequest;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthStackOverflowRequest;
import me.zhyd.oauth.request.AuthTaobaoRequest;
import me.zhyd.oauth.request.AuthTeambitionRequest;
import me.zhyd.oauth.request.AuthToutiaoRequest;
import me.zhyd.oauth.request.AuthTwitterRequest;
import me.zhyd.oauth.request.AuthWeChatEnterpriseRequest;
import me.zhyd.oauth.request.AuthWeChatMpRequest;
import me.zhyd.oauth.request.AuthWeChatOpenRequest;
import me.zhyd.oauth.request.AuthWeiboRequest;

@Service
public class IdpService {
    private static final Logger LOG = LoggerFactory.getLogger(IdpService.class);
    @Autowired
    private RedisTemplate redisTemplate;

    private BoundHashOperations<String, String, ProxyIdpConfig> valueOperations;

    @PostConstruct
    public void init() {
        valueOperations = redisTemplate.boundHashOps("IDP::CONFIGS");
    }

    public ProxyIdpConfig save(ProxyIdpConfig config) {
        valueOperations.put(config.getAlias(), config);
        return config;
    }
    /**
     * 
     * @param idpType
     * @return
     */
    @Deprecated
    public String generateId(String idpType){
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        Long id = ops.increment("idp:id", 1);
        return idpType+"-"+id;
    }

    public ProxyIdpConfig getByAlias(String alias) {
        Object config = valueOperations.get(alias);
        if(null == config) {
            return null;
        }
        return JSONObject.parseObject(JSONObject.toJSONString(config), ProxyIdpConfig.class);
    }

    public List<ProxyIdpConfig> listAll() {
        return new LinkedList<>(Objects.requireNonNull(valueOperations.values()));
    }

    public void remove(String alias) {
        valueOperations.delete(alias);
    }
    @Autowired
    private AuthStateRedisCache stateRedisCache;
    @Autowired
    private Environment environment;

    private AuthConfigBuilder createAuthConfig(ProxyIdpConfig idpConfig) {
        AuthConfigBuilder builder = AuthConfig.builder().clientId(idpConfig.getClientId())
                .clientSecret(idpConfig.getClientSecret()).redirectUri(idpConfig.getRedirectUri());
        if (Strings.isNotBlank(idpConfig.getProxyHost()) && Strings.isNotBlank(idpConfig.getProxyType())) {
            builder = builder
                    .httpConfig(HttpConfig.builder().timeout(idpConfig.getProxyTimeout())
                            .proxy(new Proxy(Proxy.Type.valueOf(idpConfig.getProxyType().toUpperCase()),
                                    new InetSocketAddress(idpConfig.getProxyHost(), idpConfig.getProxyPort())))
                            .build());
        }
        return builder;
    }
    /**
     * caculate idp config
     * @param idpConfig
     * @return
     */
    public ProxyIdpConfig caculate(ProxyIdpConfig idpConfig){
        if(Strings.isBlank(idpConfig.getAlias())){
            throw new AuthException("please set alias for idpConfig");
        }else if("proxy".equalsIgnoreCase(idpConfig.getIdpType())){
            // for full proxy, load proxy from properteis or redis
            ProxyIdpConfig localCfg = this.getByAlias(idpConfig.getAlias());
            if(localCfg==null){
                localCfg= this.readIdpFromProperties(idpConfig.getAlias());
            }
            if(Strings.isBlank(localCfg.getIdpType())||Strings.isBlank(localCfg.getClientId())||Strings.isBlank(localCfg.getClientSecret())){
                throw new AuthException("idpType,clientId,clientSecret is required in full proxy mode");
            }
            localCfg.setRedirectUri(idpConfig.getRedirectUri());
            idpConfig = localCfg;
        } 
        this.save(idpConfig);
        return idpConfig;
    }

    
    /**
     * 根据具体的授权来源，获取授权请求工具类
     *
     * @param source
     * @return
     */
    public AuthRequest getAuthRequest(ProxyIdpConfig idpConfig) {
        AuthRequest authRequest = null;
        String source = idpConfig.getIdpType();
        AuthConfigBuilder builder = createAuthConfig(idpConfig);
        switch (source.toLowerCase()) {
            case "dingtalk":
                authRequest = new AuthDingTalkRequest(builder.build());
                break;
            case "baidu":
                authRequest = new AuthBaiduRequest(builder.build());
                break;
            case "github":
                authRequest = new AuthGithubRequest(builder.build(), stateRedisCache);
                break;
            case "gitee":
                authRequest = new AuthGiteeRequest(builder.build(), stateRedisCache);
                break;
            case "weibo":
                authRequest = new AuthWeiboRequest(builder.build());
                break;
            case "coding":
                builder = builder.codingGroupName(idpConfig.getCodingGroupName());
                authRequest = new AuthCodingRequest(builder.build());
                break;
            case "oschina":
                authRequest = new AuthOschinaRequest(builder.build());
                break;
            case "alipay":
                // 支付宝在创建回调地址时，不允许使用localhost或者127.0.0.1，所以这儿的回调地址使用的局域网内的ip
                builder = builder.alipayPublicKey(idpConfig.getAlipayPublicKey());
                authRequest = new AuthAlipayRequest(builder.build());
                break;
            case "qq":
                authRequest = new AuthQqRequest(builder.build());
                break;
            case "wechat_open":
                authRequest = new AuthWeChatOpenRequest(builder.build());
                break;
            case "csdn":
                authRequest = new AuthCsdnRequest(builder.build());
                break;
            case "taobao":
                authRequest = new AuthTaobaoRequest(builder.build());
                break;
            case "google":
                authRequest = new AuthGoogleRequest(builder.build());
                break;
            case "facebook":
                authRequest = new AuthFacebookRequest(builder.build());
                break;
            case "douyin":
                authRequest = new AuthDouyinRequest(builder.build());
                break;
            case "linkedin":
                authRequest = new AuthLinkedinRequest(builder.build());
                break;
            case "microsoft":
                authRequest = new AuthMicrosoftRequest(builder.build());
                break;
            case "mi":
                authRequest = new AuthMiRequest(builder.build());
                break;
            case "toutiao":
                authRequest = new AuthToutiaoRequest(builder.build());
                break;
            case "teambition":
                authRequest = new AuthTeambitionRequest(builder.build());
                break;
            case "pinterest":
                authRequest = new AuthPinterestRequest(builder.build());
                break;
            case "renren":
                authRequest = new AuthRenrenRequest(builder.build());
                break;
            case "stack_overflow":
                builder = builder.stackOverflowKey(idpConfig.getStackOverflowKey());
                authRequest = new AuthStackOverflowRequest(builder.build());
                break;
            case "huawei":
                authRequest = new AuthHuaweiRequest(builder.build());
                break;
            case "wechat_enterprise":
                builder = builder.agentId(idpConfig.getAgentId());
                authRequest = new AuthWeChatEnterpriseRequest(builder.build());
                break;
            case "kujiale":
                authRequest = new AuthKujialeRequest(builder.build());
                break;
            case "gitlab":
                authRequest = new AuthGitlabRequest(builder.build());
                break;
            case "meituan":
                authRequest = new AuthMeituanRequest(builder.build());
                break;
            case "eleme":
                authRequest = new AuthElemeRequest(builder.build());
                break;
            case "mygitlab":
                authRequest = new AuthMyGitlabRequest(builder.build());
                break;
            case "twitter":
                authRequest = new AuthTwitterRequest(builder.build());
                break;
            case "wechat_mp":
                authRequest = new AuthWeChatMpRequest(builder.build());
                break;
            case "aliyun":
                authRequest = new AuthAliyunRequest(builder.build());
                break;
            default:
                break;
        }
        if (null == authRequest) {
            throw new AuthException("未获取到有效的Auth配置");
        }
        return authRequest;
    }

    public ProxyIdpConfig readIdpFromProperties(String source) {
        Field[] fields = ProxyIdpConfig.class.getDeclaredFields();
        ProxyIdpConfig idpConfig = new ProxyIdpConfig();
        for (int i = 0; i < fields.length; i++) {
            String element = fields[i].getName();
            String propertyType = fields[i].getType().getName();
            fields[i].setAccessible(true);
            String value = environment.getProperty("idp." + source + "." + element);
            if (Strings.isNotBlank(value)) {
                try {
                    if (propertyType.equalsIgnoreCase("java.lang.Integer")) {
                        fields[i].set(idpConfig, Integer.parseInt(value));
                    } else {
                        fields[i].set(idpConfig, value);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return idpConfig;
    }

    public AuthRequest getAuthRequestBySource(String source) {
        ProxyIdpConfig proxyIdpConfig = readIdpFromProperties(source.toLowerCase());
        if(Strings.isBlank(proxyIdpConfig.getIdpType())){
            proxyIdpConfig.setIdpType(source);
        }
        return getAuthRequest(proxyIdpConfig);
    }
}
