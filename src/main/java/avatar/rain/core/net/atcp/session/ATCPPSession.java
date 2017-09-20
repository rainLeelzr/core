package avatar.rain.core.net.atcp.session;

import avatar.rain.core.net.atcp.netpackage.TcpPacket;
import avatar.rain.core.util.log.LogUtil;
import io.netty.channel.Channel;

/**
 * atcp
 */
public class ATCPPSession extends Session<Channel> {

    /**
     * 发送给客户端时，默认使用的body数据表格
     */
    private String useBodyType;

    public ATCPPSession(Channel c, String useBodyType) {
        super(c);
        this.useBodyType = useBodyType;
    }

    public void sendProtoToClient(String url, byte[] bodyBytes) {
        TcpPacket packet = TcpPacket.buildProtoPackage(url, bodyBytes);
        getChannel().writeAndFlush(packet.getByteBuf());
    }

    public void sendJsonToClient(String url, byte[] bodyBytes) {
        TcpPacket packet = TcpPacket.buildJsonPackage(url, bodyBytes);
        getChannel().writeAndFlush(packet.getByteBuf());
    }

    public void sendJsonToClient(String url, String bodyJson) {
        TcpPacket packet = TcpPacket.buildJsonPackage(url, bodyJson);
        getChannel().writeAndFlush(packet.getByteBuf());
    }

    public String getClientIP() {
        return getChannel().localAddress().toString();
    }

    @Override
    public void sendClient(String url, byte[] bodyBytes) {
        if (TcpPacket.BodyType.PROTOBUF.toString().equalsIgnoreCase(useBodyType)) {
            sendProtoToClient(url, bodyBytes);
        } else if (TcpPacket.BodyType.JSON.toString().equalsIgnoreCase(useBodyType)) {
            sendJsonToClient(url, bodyBytes);
        } else {
            TcpPacket.BodyType[] values = TcpPacket.BodyType.values();
            StringBuilder definedType = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                TcpPacket.BodyType value = values[i];
                definedType.append(value.toString());
                if (i < values.length - 1) {
                    definedType.append("、");
                }
            }
            LogUtil.getLogger().warn(
                    "useBodyType未初始化，或不是以下内容的其中一个：[{}]，将自动采用{}来编码",
                    definedType.toString(),
                    TcpPacket.BodyType.PROTOBUF);
            sendProtoToClient(url, bodyBytes);
        }
    }

    @Override
    public String getRemoteIP() {
        return getChannel().remoteAddress().toString();
    }

    public void close() {
        getChannel().close();
    }
}
