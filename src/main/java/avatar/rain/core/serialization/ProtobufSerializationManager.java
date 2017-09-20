package avatar.rain.core.serialization;

import avatar.rain.core.api.Api;
import avatar.rain.core.api.ApiManager;
import avatar.rain.core.util.log.LogUtil;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProtobufSerializationManager implements serialization {

    /**
     * key: protobuf的类名
     * value: protobuf的parseFrom(byte[] data)方法
     */
    private Map<String, Method> methods;

    @Resource
    private ApiManager apiManager;

    /**
     * 需要在apiManager.init()之的，才执行。因为apiManager.init()未执行前，apiManager.getApis()为空
     */
    public void init() {
        LogUtil.getLogger().info("正在初始化ProtobufSerializationManager...");
        Collection<Api> values = apiManager.getApis().values();
        Map<String, Method> methods = new HashMap<>();

        values.forEach(a -> {
            if (!methods.containsKey(a.getProtobuf())) {
                try {
                    Class<?> protoClass = Class.forName(a.getProtobuf());
                    Method parseFrom = protoClass.getMethod("parseFrom", byte[].class);
                    methods.put(a.getProtobuf(), parseFrom);
                } catch (ClassNotFoundException e) {
                    LogUtil.getLogger().error("找不到api定义的protobuf类：{}", a.toString());
                    System.exit(0);
                } catch (NoSuchMethodException e) {
                    LogUtil.getLogger().error("api定义的protobuf类中找不到parseFrom方法：{}", a.toString());
                    System.exit(0);
                }
            }

        });
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
