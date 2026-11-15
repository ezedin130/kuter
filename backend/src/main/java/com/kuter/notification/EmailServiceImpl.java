package com.kuter.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    public static final String SENDER_EMAIL = "wetechethiopia@gmail.com";

    @Override
    public void sendRegistrationReceivedEmail(String toEmail, String shopName, String referenceCode) {
        String subject = "Registration Received — Kuter Building Materials Inventory System";
        String body = String.format(
                "Dear %s Owner,%n%n" +
                "Thank you for registering your shop on the Kuter Building Materials Inventory System.%n%n" +
                "Your application details:%n" +
                "- Shop Name: %s%n" +
                "- Application Reference Code: %s%n" +
                "- Expected Review Time: Within 24 business hours%n%n" +
                "Next Steps:%n" +
                "Our platform review team will review your application and verify your business details. " +
                "You can check your application status at any time on the portal using your reference code.%n%n" +
                "Once approved, you will receive an approval email with your shop code and access instructions.%n%n" +
                "Best regards,%n" +
                "Kuter Platform Onboarding Team%n" +
                "From: %s",
                shopName, shopName, referenceCode, SENDER_EMAIL
        );

        log.info("[EMAIL DISPATCHED] From: {} | To: {} | Subject: '{}' | Ref: {}",
                SENDER_EMAIL, toEmail, subject, referenceCode);
        log.debug("[EMAIL BODY]%n{}", body);
    }

    @Override
    public void sendShopApprovalEmail(String toEmail, String ownerName, String shopName, String shopCode, String loginUrl) {
        String subject = "Shop Approved — Welcome to Kuter Building Materials Inventory System";
        String body = String.format(
                "Dear %s,%n%n" +
                "Congratulations! Your registration for '%s' has been approved by the platform administrator.%n%n" +
                "Your workspace is now active and ready for business:%n" +
                "- Shop Name: %s%n" +
                "- Shop Code: %s%n" +
                "- Login URL: %s%n" +
                "- Username: %s%n%n" +
                "Instructions for First Login:%n" +
                "1. Navigate to the login portal at %s%n" +
                "2. Sign in with your work email (%s) and the secure account password you set during registration.%n" +
                "3. Your initial catalog units and material categories are pre-configured.%n" +
                "4. Begin recording products, inbound stock receipts, and sales immediately.%n%n" +
                "Welcome aboard,%n" +
                "Kuter Platform Administration%n" +
                "From: %s",
                ownerName, shopName, shopName, shopCode, loginUrl, toEmail, loginUrl, toEmail, SENDER_EMAIL
        );

        log.info("[EMAIL DISPATCHED] From: {} | To: {} | Subject: '{}' | Shop: {} ({})",
                SENDER_EMAIL, toEmail, subject, shopName, shopCode);
        log.debug("[EMAIL BODY]%n{}", body);
    }
}
