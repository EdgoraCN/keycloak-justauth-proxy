package com.edgora.idp.dto;

import lombok.Data;
import me.zhyd.oauth.model.AuthCallback;
@Data
public class IdpAuthCallback extends AuthCallback {
    private String redirectUri;
}
