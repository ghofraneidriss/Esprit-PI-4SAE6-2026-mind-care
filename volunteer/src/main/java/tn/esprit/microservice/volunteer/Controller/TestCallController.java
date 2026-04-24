package tn.esprit.microservice.volunteer.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.microservice.volunteer.Service.TwilioService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TestCallController {

    private final TwilioService twilioService;

    @GetMapping("/test-call")
    public String triggerTestCall(@RequestParam("to") String to) {
        String message = "Hello, this is a test call from MindCare volunteering microservice. Your Twilio integration is working correctly.";
        String callSid = twilioService.makeCall(to, message);

        if (callSid != null) {
            return "Test call triggered successfully. Call SID: " + callSid;
        } else {
            return "Failed to trigger test call. Check logs for details.";
        }
    }

    @GetMapping("/test-sms")
    public String triggerTestSms(@RequestParam("to") String to) {
        String message = "Hello, this is a test SMS from MindCare volunteering microservice. Your Twilio integration is working correctly.";
        String smsSid = twilioService.sendSms(to, message);

        if (smsSid != null) {
            return "Test SMS sent successfully. SMS SID: " + smsSid;
        } else {
            return "Failed to send test SMS. Check logs for details.";
        }
    }
}
