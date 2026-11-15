package com.kuter.notification;

/**
 * Service for transactional and notification emails.
 * Uses system sender wetechethiopia@gmail.com.
 */
public interface EmailService {

    /**
     * Sends acknowledgment email when a shop registration request is submitted.
     */
    void sendRegistrationReceivedEmail(String toEmail, String shopName, String referenceCode);

    /**
     * Sends approval and activation email when a platform admin approves the shop application.
     */
    void sendShopApprovalEmail(String toEmail, String ownerName, String shopName, String shopCode, String loginUrl);
}
