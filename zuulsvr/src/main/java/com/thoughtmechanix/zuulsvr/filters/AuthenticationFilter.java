package com.thoughtmechanix.zuulsvr.filters;


import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.thoughtmechanix.zuulsvr.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class AuthenticationFilter extends ZuulFilter {
    private static final int FILTER_ORDER =  2;
    private static final boolean  SHOULD_FILTER=true;

    @Autowired
    FilterUtils filterUtils;

    @Autowired
    RestTemplate restTemplate;

    @Override
    public String filterType() {
        return filterUtils.PRE_FILTER_TYPE;
    }

    @Override
    public int filterOrder() {
        return FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter() {
        return SHOULD_FILTER;
    }

    private boolean isAuthTokenPresent() {
        if (filterUtils.getAuthToken() !=null){
            return true;
        }

        return false;
    }

    private UserInfo isAuthTokenValid(){
        ResponseEntity<UserInfo> restExchange = null;
        try {
            restExchange =
                    restTemplate.exchange(
                            "http://authenticationservice/v1/validate/{token}",
                            HttpMethod.GET,
                            null, UserInfo.class, filterUtils.getAuthToken());
        }
        catch(HttpClientErrorException ex){
            if (ex.getStatusCode()==HttpStatus.UNAUTHORIZED) {
                return null;
            }

            throw ex;
        }


        return restExchange.getBody();
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();

        if (isAuthTokenPresent()){
           filterUtils.flog("Authentication token is present.");
        }else{
            filterUtils.flog("Authentication token is not present.");

            ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            ctx.setSendZuulResponse(false);
        }

        UserInfo userInfo = isAuthTokenValid();
        if (userInfo!=null){
            filterUtils.setUserId(userInfo.getUserId());
            filterUtils.setOrgId(userInfo.getOrganizationId());

            filterUtils.flog("Authentication token is valid.");
            return null;
        }

        filterUtils.flog("Authentication token is not valid.");
        ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
        ctx.setSendZuulResponse(false);

        return null;

    }
}
