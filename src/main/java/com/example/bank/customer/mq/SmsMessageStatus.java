package com.example.bank.customer.mq;

/** Status values stored in t_sms_message. */
public final class SmsMessageStatus {

    public static final String INIT = "INIT";
    public static final String MQ_SENT = "MQ_SENT";
    public static final String SENDING = "SENDING";
    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";
    public static final String FAILED_FINAL = "FAILED_FINAL";

    private SmsMessageStatus() {
    }
}
