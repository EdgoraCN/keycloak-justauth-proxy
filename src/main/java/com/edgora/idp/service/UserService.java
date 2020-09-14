package com.edgora.idp.service;

import me.zhyd.oauth.model.AuthUser;

import java.util.List;

/**
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @version 1.0.0
 * @date 2020/6/27 22:39
 * @since 1.0.0
 */
public interface UserService {

    AuthUser save(AuthUser user);

    AuthUser getByUuidAndSource(String uuid,String source);

    List<AuthUser> listAll();

    void remove(String uuid);
}
