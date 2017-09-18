package avatar.rain.core.net.atcp.netpackage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.UnsupportedEncodingException;

/**
 * 网络数据包的基础结构
 */
public abstract class BasePacket {

    private int len;//包长度

    private int type;//包类型. 0 protobuff ; 1 json

    private int cmd;//对应具体命令（反射到具体函数中）

    private int userId;

    private int code;//序号。可以根据序号来分配工人线程处理业务逻辑

    private byte[] bytes;// 业务数据

    public int getType() {
        return type;
    }

    public int getCmd() {
        return cmd;
    }

    public byte[] getBytes() {
        return bytes;
    }

    void setCmd(int cmd) {
        this.cmd = cmd;
    }

    void setType(int type) {
        this.type = type;
    }

    void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    void setLen(int len) {
        this.len = len;
    }

    public int getUserId() {
        return userId;
    }

    void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCode() {
        return code;
    }

    void setCode(int code) {
        this.code = code;
    }

    BasePacket() {
    }

    BasePacket(int len, int cmd, int type, int userId, int code, byte[] bytes) {
        this.len = len;
        this.cmd = cmd;
        this.type = type;
        this.userId = userId;
        this.code = code;
        this.bytes = bytes;
    }

    public void parseFromFullATCPData(ByteBuf byteBuf) {
        this.len = byteBuf.readInt();
        this.cmd = byteBuf.readInt();
        this.type = byteBuf.readInt();
        this.userId = byteBuf.readInt();
        this.code = byteBuf.readInt();
        this.bytes = new byte[this.len];
        byteBuf.readBytes(bytes);
    }

    public abstract void buildPacket(int cmd, byte[] bytes);

    public ByteBuf sendPack(String respon) {
        try {
            byte[] req = respon.getBytes("UTF-8");
            ByteBuf byteBuf = Unpooled.buffer();
            int len = req.length;
            byteBuf.writeByte(len);
            byteBuf.writeInt(this.cmd);
            byteBuf.writeByte(this.type);
            byteBuf.writeBytes(req);
            return byteBuf;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 服务器发送给客户端的数据包的格式
     */
    public ByteBuf sendPacket() {
        ByteBuf byteBuf = Unpooled.buffer();
        int len = this.len;
        byteBuf.writeInt(len);
        byteBuf.writeInt(this.cmd);
        byteBuf.writeInt(this.type);
        byteBuf.writeInt(this.userId);
        byteBuf.writeInt(this.code);
        byteBuf.writeBytes(this.bytes);
        return byteBuf;
    }

    @Override
    public String toString() {
        return new StringBuilder("{")
                .append("\"len\":")
                .append(len)
                .append(",\"type\":")
                .append(type)
                .append(",\"cmd\":")
                .append(cmd)
                .append(",\"userId\":")
                .append(userId)
                .append(",\"code\":")
                .append(code)
                .append('}')
                .toString();
    }
}
