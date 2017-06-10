package com.swirlwave.android.proxies.clientside;

public class ChannelAttachment {
    private ChannelDirection mChannelDirection;
    private ProtocolState mProtocolState;

    public ChannelDirection getChannelDirection() {
        return mChannelDirection;
    }

    public ProtocolState getProtocolState() {
        return mProtocolState;
    }

    public ChannelAttachment(ChannelDirection channelDirection, ProtocolState protocolState) {
        mChannelDirection = channelDirection;
        mProtocolState = protocolState;
    }
}
