package com.payment.controller.midtrans;

import com.payment.dto.midtrans.MidtransWebhookNotification;
import com.payment.service.midtrans.MidtransWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/midtrans")
public class MidtransWebhookController {
    
    private final MidtransWebhookService midtransWebhookService;

    public MidtransWebhookController(final MidtransWebhookService midtransWebhookService){
        this.midtransWebhookService = midtransWebhookService;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody final MidtransWebhookNotification notification){
        midtransWebhookService.handleNotification(notification);
        return ResponseEntity.ok().build();
    }

}
