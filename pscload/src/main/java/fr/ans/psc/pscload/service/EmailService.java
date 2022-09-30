/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.service;

import java.io.File;
import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import fr.ans.psc.pscload.model.EmailTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class EmailService.
 */
@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${pscload.mail.receiver}")
    private String receiver;

    @Value("${enable.emailing}")
    private boolean enableEmailing;

    @Value("${secpsc.environment}")
    private String platform;


    public void setEmailSender(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * Send mail.
     *
     * @param template        the email template
     * @param customBody      the custom body of mail if any
     * @param attachmentFile an optional attachment file
     */
    public void sendMail(EmailTemplate template, String customBody, File attachmentFile) {
        if (enableEmailing) {
            try {
                MimeMessage message = emailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setFrom(sender);
                String[] allReceivers = receiver.split(",");
                helper.setTo(allReceivers);
                helper.setSubject(platform + " " + template.subject);

                Multipart emailContent = new MimeMultipart();
                MimeBodyPart textBody = new MimeBodyPart();
                String body = customBody != null ? customBody : template.message;
                textBody.setText(body);
                emailContent.addBodyPart(textBody);

                if (attachmentFile != null) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.attachFile(attachmentFile);
                    emailContent.addBodyPart(attachmentPart);
                }

                message.setContent(emailContent);
                emailSender.send(message);
            } catch (MailException | MessagingException | IOException mse) {
                log.error("Mail sending error", mse);
            }
        }
    }

    /**
     * Send mail.
     *
     * @param template the template
     */
    public void sendMail(EmailTemplate template) {
        this.sendMail(template, null, null);
    }

}
