package com.example.bank.customer.mq;

/** Status values stored in t_sms_message. */
public final class SmsMessageStatus {

    public static final String INIT = "INIT";
    public static final String SENDING = "SENDING";
    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";

    private SmsMessageStatus() {
    }
}
