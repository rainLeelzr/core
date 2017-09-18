package avatar.rain.core.api;

import avatar.rain.core.net.atcp.request.RequestCmd;
import avatar.rain.core.util.log.LogUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public class ApiManager {

    /**
     * key: cmd
     * value: Api
     */
    private Map<Integer, Api> apis;

    public Api getApi(Integer cmd) {
        return apis.get(cmd);
    }

    public Map<Integer, Api> getApis() {
        return apis;
    }

    public void init(ApplicationContext applicationContext) {
        try {
            apis = new HashMap<>();

            LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

            // 遍历所有含有@RequestCmd注解的bean
            String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
            for (String beanDefinitionName : beanDefinitionNames) {
                Object bean = applicationContext.getBean(beanDefinitionName);
                Method[] declaredMethods = bean.getClass().getDeclaredMethods();
                for (Method targetApiMethod : declaredMethods) {
                    RequestCmd annotation = targetApiMethod.getAnnotation(RequestCmd.class);
                    if (annotation == null) {
                        continue;
                    }

                    // 解析此方法上的RequestCmd注解，判断其参数是否正确
                    if (annotation.cmd() == 0) {
                        throw new Error("方法[" + targetApiMethod.toString() + "]的RequestCmd注解参数错误，cmd必须赋值，且不能等于0");
                    }

                    if (apis.containsKey(annotation.cmd())) {
                        throw new Error("方法[" +
                                targetApiMethod.toString() +
                                "]的RequestCmd注解的cmd值与[" +
                                apis.get(annotation.cmd()).getMethod().toString() +
                                "]重复，请修改。cmd=" +
                                annotation.cmd());
                    }

                    Class<?>[] parameterTypes = targetApiMethod.getParameterTypes();

                    Api api = new Api();
                    api.setBeanClass(bean);
                    api.setMethod(targetApiMethod);
                    api.setProtobuf(annotation.protobuf());
                    String[] parameterNames = discoverer.getParameterNames(targetApiMethod);
                    api.setParameterNames(parameterNames);
                    api.setParameterTypes(parameterTypes);

                    apis.put(annotation.cmd(), api);
                }
            }
        } catch (Throwable e) {
            LogUtil.getLogger().error(e.getMessage(), e);
            System.exit(0);
        }

    }

}
