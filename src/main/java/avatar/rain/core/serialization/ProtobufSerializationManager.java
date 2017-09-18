package avatar.rain.core.serialization;

import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@Component
public class ProtobufSerializationManager implements serialization {

    /**
     * key: protobuf的类名
     * value: protobuf的parseFrom(byte[] data)方法
     */
    private Map<String, Method> methods;

    public void init(Map<String, Method> methods) {
        this.methods = methods;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String protobufClass, Object data) throws Exception {
        Method parseMethod = getParseMethod(protobufClass);
        Object protobufJavaBean = parseMethod.invoke(null, data);
        return (T) protobufJavaBean;
    }

    /**
     * 根据proto类的名称获取pb的解析二进制数据的方法
     */
    private Method getParseMethod(String protobufClass) {
        return methods.get(protobufClass);
    }

    /**
     * 获取对象的成员变量值
     */
    public Object getMemberValue(String memberVariableName, String protobufClass, Object obj)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // 从缓存中获取class，性能较好
        Method parseMethod = getParseMethod(protobufClass);
        Class<?> protoClass = parseMethod.getDeclaringClass();
        Method method = protoClass.getMethod(memberVariableName);
        return method.invoke(obj);
    }

}
