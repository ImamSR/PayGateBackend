package com.payment.service.midtrans;

import com.payment.config.MidtransProperties;
import com.payment.dto.midtrans.*;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class MidtransClient {

    private static final String CHARGE_PATH = "/v2/charge";


    private final RestTemplate restTemplate;
    private final MidtransProperties midtransProperties;

    public MidtransClient(
        final RestTemplate restTemplate,
        final MidtransProperties midtransProperties
    ){
        this.restTemplate = restTemplate;
        this.midtransProperties = midtransProperties;
    }

    public MidtransChargeResponse charge(final MidtransChargeRequest request){
        final HttpHeaders headers =  new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader());

        final HttpEntity<MidtransChargeRequest> httpEntity = new HttpEntity<> (request, headers);
        final String url = midtransProperties.getBaseUrl() + CHARGE_PATH;

        try {
            final ResponseEntity<MidtransChargeResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, httpEntity, MidtransChargeResponse.class);
                if (response.getBody() == null ) {
                    throw new MidtransIntegrationException("Midtrans error returned empty response body");
                } 

                return response.getBody();
        } catch (RestClientException exception){
            throw new MidtransIntegrationException("Failed to call MIDTRANS API", exception);
        }
    }

    private String buildAuthorizationHeader(){
        final String credential = midtransProperties.getServerKey() +":";
        final String encoded = Base64.getEncoder()
        .encodeToString(credential.getBytes(StandardCharsets.UTF_8));

        return "Basic " + encoded;
    }
}
