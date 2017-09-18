package avatar.rain.core.net.atcp.netpackage;

import io.netty.buffer.ByteBuf;

/**
 * 一个完整的 avatar atcp 数据包
 */
public class ATCPPacket extends BasePacket {

    public ATCPPacket() {

    }

    public ATCPPacket(int len, int cmd, int type, int userId, int code, byte[] bytes) {
        super(len, cmd, type, userId, code, bytes);
    }

    public void parseFromFullATCPData(ByteBuf byteBuf) {
        super.parseFromFullATCPData(byteBuf);
    }

    @Override
    public void buildPacket(int cmd, byte[] bytes) {
        int len = bytes.length;
        this.setLen(len);
        this.setCmd(cmd);
        this.setType(PacketBodyType.Proto.getType());
        this.setUserId(getUserId());
        this.setCode(getCode());
        this.setBytes(bytes);
    }
}
