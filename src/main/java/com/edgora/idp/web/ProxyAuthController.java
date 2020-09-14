package com.edgora.idp.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.edgora.idp.cache.AuthStateRedisCache;
import com.edgora.idp.dto.IdpAuthCallback;
import com.edgora.idp.dto.ProxyIdpConfig;
import com.edgora.idp.dto.Response;
import com.edgora.idp.service.IdpService;
import com.edgora.idp.service.UserService;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthDefaultRequest;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;

/**
 * @author tiger.wang
 * @version 1.0
 */
@RestController
@RequestMapping("/auth")
public class ProxyAuthController {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyAuthController.class);
    @Autowired
    private IdpService idpService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthStateRedisCache stateRedisCache;

    @PostMapping("/register")
    @ResponseBody
    public AuthResponse<Map<String,String>> renderAuth(@RequestBody ProxyIdpConfig idpConfig, HttpServletResponse response) throws IOException {
        LOG.info("register auth config：{}" + JSON.toJSONString(idpConfig));
        idpConfig.setRedirectUri("http://redirectUri");
        idpConfig = idpService.caculate(idpConfig);
        AuthRequest authRequest = idpService.getAuthRequest(idpConfig);
        String authorizeUrl = authRequest.authorize("{state}");
        LOG.info("authorizeUrl={}",authorizeUrl);
        AuthResponse<Map<String,String>> authResponse = new AuthResponse<>();
        Map<String,String> resultMap = new HashMap<>();
        resultMap.put("authorizeUrl", authorizeUrl);
        resultMap.put("idpType", idpConfig.getIdpType());
        authResponse.setData(resultMap);
        authResponse.setCode(2000);
        return authResponse;
    }

    @PostMapping("/render/{alias}")
    @ResponseBody
    public AuthResponse<Map<String,String>> renderAuth(@PathVariable String alias,@RequestParam String redirectUri,@RequestParam String state, HttpServletResponse response) throws IOException {
        ProxyIdpConfig config = idpService.getByAlias(alias);
        config.setRedirectUri(redirectUri);
        LOG.info("render auth config：{}" + JSON.toJSONString(config));
        AuthRequest authRequest = idpService.getAuthRequest(config);
        if(Strings.isBlank(state)){
            state = AuthStateUtils.createState();
        }
        String authorizeUrl = authRequest.authorize(state);
        LOG.info("authorizeUrl={}",authorizeUrl);
        AuthResponse<Map<String,String>> authResponse = new AuthResponse<>();
        Map<String,String> resultMap = new HashMap<>();
        resultMap.put("authorizeUrl", authorizeUrl);
        resultMap.put("idpType", config.getIdpType());
        authResponse.setCode(2000);
        return authResponse;
    }

    /**
     * proxy login api, return user data
     */
    @PostMapping("/login/{alias}")
    public AuthResponse<AuthUser> login(@RequestBody IdpAuthCallback callback,@PathVariable String alias,HttpServletRequest request) {
        LOG.info("callback：{}" , JSONObject.toJSONString(callback));
        ProxyIdpConfig config = idpService.getByAlias(alias);
        LOG.debug("config={}",JSONObject.toJSONString(config));
        config.setRedirectUri(callback.getRedirectUri());
        AuthRequest authRequest = idpService.getAuthRequest(config);
        LOG.debug("request={}",JSONObject.toJSONString(authRequest));
        AuthResponse<AuthUser> response = authRequest.login(callback);
        LOG.info("response={}", JSONObject.toJSONString(response));
        if (response.ok()) {
            userService.save(response.getData());
        }
        return response;
    }

    @RequestMapping("/revoke/{alias}/{uuid}")
    @ResponseBody
    public Response revokeAuth(@PathVariable("alias") String id, @PathVariable("uuid") String uuid) throws IOException {
        ProxyIdpConfig config = idpService.getByAlias(id);
        AuthRequest authRequest = idpService.getAuthRequest(config);
        AuthUser user = userService.getByUuidAndSource(uuid,config.getIdpType());
        if (null == user) {
            return Response.error("用户不存在");
        }
        AuthResponse<AuthToken> response = null;
        try {
            response = authRequest.revoke(user.getToken());
            if (response.ok()) {
                userService.remove(user.getUuid());
                return Response.success("用户 [" + user.getUsername() + "] 的 授权状态 已收回！");
            }
            return Response.error("用户 [" + user.getUsername() + "] 的 授权状态 收回失败！" + response.getMsg());
        } catch (AuthException e) {
            return Response.error(e.getErrorMsg());
        }
    }
    

    @RequestMapping("/refresh/{alias}/{uuid}")
    @ResponseBody
    public Object refreshAuth(@PathVariable("alias") String alias, @PathVariable("uuid") String uuid) {
        ProxyIdpConfig config = idpService.getByAlias(alias);
        AuthRequest authRequest = idpService.getAuthRequest(config);
        AuthUser user = userService.getByUuidAndSource(uuid,config.getIdpType());
        if (null == user) {
            return Response.error("用户不存在");
        }
        AuthResponse<AuthToken> response = null;
        try {
            response = authRequest.refresh(user.getToken());
            if (response.ok()) {
                user.setToken(response.getData());
                userService.save(user);
                return Response.success("用户 [" + user.getUsername() + "] 的 access token 已刷新！新的 accessToken: " + response.getData().getAccessToken());
            }
            return Response.error("用户 [" + user.getUsername() + "] 的 access token 刷新失败！" + response.getMsg());
        } catch (AuthException e) {
            return Response.error(e.getErrorMsg());
        }
    }

    
}