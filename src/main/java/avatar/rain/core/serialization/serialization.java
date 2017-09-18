package avatar.rain.core.serialization;

public interface serialization {

    /**
     * 反序列化
     */
    <T> T deserialize(String protobufClass, Object data) throws Exception;
}
