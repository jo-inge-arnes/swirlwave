package com.swirlwave.android.proxies.serverside;

public enum ServerProtocolStateCode {
    UNKNOWN,
    WRITE_RANDOM_NUMBER_TO_CLIENT,
    READ_CONNECTION_MESSAGE_LENGTH,
    READ_CONNECTION_MESSAGE,
    WRITE_CONNECTION_MESSAGE_RESPONSE,
    PROXYING
}
