package com.edgora.idp.dto;

import lombok.Data;
@Data
public class ProxyIdpConfig {
    /**
     * idp proxy type
     */
    private String idpType;
    /**
     * idpId which load from redis or database
     */
    private String id;
    /**
     * idp setting
     */
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String alipayPublicKey;
    private String stackOverflowKey;
    private String agentId;
    private String codingGroupName;
    /**
     * proxy setting
     */
    private String proxyHost;
    private String proxyType="HTTP";
    private Integer proxyPort=8080;
    private Integer proxyTimeout=15000;

}
