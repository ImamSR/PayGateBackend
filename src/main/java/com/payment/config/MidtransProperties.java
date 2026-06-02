package com.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.providers.midtrans")
public class MidtransProperties {
    private String baseUrl;
    private String serverKey;
    private String clientKey;
    private String merchantId;
    private String callbackPath;
    private Long connectTimeoutMillis;
    private Long readTimeoutMillis;

    public String getBaseUrl(){
        return baseUrl;
    }

    public void setBaseUrl(final String baseUrl){
        this.baseUrl = baseUrl;
    }

    public String getServerKey(){
        return serverKey;
    }

    public void setServerKey(final String serverKey){
        this.serverKey = serverKey;
    }

    public String getClientKey(){
        return clientKey;
    }

    public void setClientKey(final String clientKey){
        this.clientKey = clientKey;
    }

    public String getMerchantId(){
        return merchantId;
    }

    public void setMerchantId(final String merchantId){
        this.merchantId = merchantId;
    }

    public String getCallbackPath(){
        return callbackPath;
    }

    public void setCallbackPath(final String callbackPath){
        this.callbackPath = callbackPath;
    }

    public Long getConnectTimeoutMillis(){
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(final Long connectTimeoutMillis){
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public Long getReadTimeoutMillis(){
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(final Long readTimeoutMillis){
        this.readTimeoutMillis = readTimeoutMillis;
    }
    
}
