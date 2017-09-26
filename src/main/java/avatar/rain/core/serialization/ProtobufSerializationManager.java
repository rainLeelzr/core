package avatar.rain.core.serialization;

import avatar.rain.core.api.Api;
import avatar.rain.core.api.MicroServerService;
import avatar.rain.core.api.MicroServerUpdatedEvent;
import avatar.rain.core.api.ServerApi;
import avatar.rain.core.net.tcp.TcpServerCondition;
import avatar.rain.core.util.log.LogUtil;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Conditional(TcpServerCondition.class)
public class ProtobufSerializationManager implements serialization, ApplicationListener<MicroServerUpdatedEvent> {

    /**
     * key: protobuf的类名
     * value: protobuf的parseFrom(byte[] data)方法
     * 在初始化时，必须重新new map，因为微服务有可以删除了一些api，如果此处的map，在更新时，只用put来覆盖原值，则不能清空已被删除的api
     */
    private Map<String, Method> methods;

    @Resource
    private MicroServerService microServerService;

    /**
     * 需要在apiManager.init()之的，才执行。因为apiManager.init()未执行前，apiManager.getApis()为空
     */
    public void init() {
        Map<String, Method> tempMethods = new HashMap<>();
        Map<String, ServerApi> microServerApis = microServerService.getMicroServerApis();
        for (Map.Entry<String, ServerApi> entry : microServerApis.entrySet()) {
            ServerApi serverApi = entry.getValue();
            Map<String, List<Api>> requestMappingApis = serverApi.getRequestMappingApis();
            for (Map.Entry<String, List<Api>> apiEntry : requestMappingApis.entrySet()) {
                List<Api> apis = apiEntry.getValue();
                for (Api api : apis) {
                    String protobufC2S = api.getProtobufC2S();
                    if (protobufC2S == null || protobufC2S.length() == 0 || tempMethods.containsKey(protobufC2S)) {
                        continue;
                    }

                    try {
                        Class<?> protoClass = Class.forName(protobufC2S);
                        Method parseFrom = protoClass.getMethod("parseFrom", byte[].class);
                        LogUtil.getLogger().info("加载到proto：{}", protobufC2S);
                        tempMethods.put(protobufC2S, parseFrom);
                    } catch (ClassNotFoundException e) {
                        LogUtil.getLogger().error("找不到api定义的protobuf类：{}", api);
                        System.exit(0);
                    } catch (NoSuchMethodException e) {
                        LogUtil.getLogger().error("api定义的protobuf类中找不到parseFrom方法：{}", api);
                        System.exit(0);
                    }
                }
            }
        }

        this.methods = tempMethods;
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

    @Override
    public void onApplicationEvent(MicroServerUpdatedEvent event) {
        init();
    }
}
