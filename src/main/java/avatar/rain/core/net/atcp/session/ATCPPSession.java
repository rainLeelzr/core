package avatar.rain.core.net.atcp.session;

import avatar.rain.core.net.atcp.netpackage.ATCPPacket;
import avatar.rain.core.net.atcp.netpackage.BasePacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * atcp
 */
public class ATCPPSession extends Session<Channel> {

    private ChannelHandlerContext channelHandlerContext;

    public ATCPPSession(ChannelHandlerContext chx) {
        super(chx.channel());
        this.channelHandlerContext = chx;
    }

    public void sendClient(int cmd, byte[] bytes) {
        BasePacket packet = new ATCPPacket();
        packet.buildPacket(cmd, bytes);
        getChannel().writeAndFlush(packet.sendPacket());
    }

    public String getClientIP() {
        return getChannel().localAddress().toString();
    }

    @Override
    public String getRemoteIP() {
        return getChannel().remoteAddress().toString();
    }

    public void close() {
        getChannel().close();
    }
}
