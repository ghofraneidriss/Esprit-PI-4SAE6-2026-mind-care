package tn.esprit.microservice.volunteer.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.twilio.type.Twiml;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwilioService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    @PostConstruct
    public void init() {
        log.info("Initializing Twilio client");
        Twilio.init(accountSid, authToken);
    }

    /**
     * Makes a voice call to the specified phone number with a text-to-speech
     **/
    public String makeCall(String to, String messageText) {
        try {
            log.info("Triggering voice call to: {} with message: {}", to, messageText);

            // Generate TwiML for text-to-speech
            String twimlContent = "<Response><Say>" + messageText + "</Say></Response>";

            Call call = Call.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromPhoneNumber),
                    new Twiml(twimlContent)).create();

            log.info("Call successfully triggered. SID: {}", call.getSid());
            return call.getSid();
        } catch (Exception e) {
            log.error("Failed to trigger Twilio call to {}: {}", to, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Sends an SMS to the specified phone number.
     * 
     * @param to          The recipient's phone number.
     * @param messageText The message content.
     * @return Message SID if successful.
     */
    public String sendSms(String to, String messageText) {
        try {
            log.info("Sending SMS to: {} with message: {}", to, messageText);

            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromPhoneNumber),
                    messageText).create();

            log.info("SMS successfully sent. SID: {}", message.getSid());
            return message.getSid();
        } catch (Exception e) {
            log.error("Failed to send Twilio SMS to {}: {}", to, e.getMessage(), e);
            return null;
        }
    }
}
