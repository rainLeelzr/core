package avatar.rain.core.api;

import avatar.rain.core.net.tcp.request.RequestCmd;
import avatar.rain.core.util.log.LogUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public class ApiManager implements ApplicationContextAware {

    /**
     * key: url
     * value: Api
     */
    private Map<String, Api> apis;

    public Api getApi(String url) {
        return apis.get(url);
    }

    public Map<String, Api> getApis() {
        return apis;
    }

    private ApplicationContext applicationContext;

    /**
     * 需要在所有spring bean加载完后，才调用本方法
     */
    public void init() {
        LogUtil.getLogger().info("正在初始化ApiManager...");
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
                    if (annotation.url() == null || annotation.url().length() == 0) {
                        throw new Error("方法[" + targetApiMethod.toString() + "]的RequestCmd注解参数错误，url必须赋值，且不能为空字符串");
                    }

                    if (apis.containsKey(annotation.url())) {
                        throw new Error("方法[" +
                                targetApiMethod.toString() +
                                "]的RequestCmd注解的url值与[" +
                                apis.get(annotation.url()).getMethod().toString() +
                                "]重复，请修改。url=" +
                                annotation.url());
                    }

                    Class<?>[] parameterTypes = targetApiMethod.getParameterTypes();

                    Api api = new Api();
                    api.setBeanClass(bean);
                    api.setMethod(targetApiMethod);
                    api.setProtobuf(annotation.protobuf());
                    String[] parameterNames = discoverer.getParameterNames(targetApiMethod);
                    api.setParameterNames(parameterNames);
                    api.setParameterTypes(parameterTypes);

                    apis.put(annotation.url(), api);
                }
            }
        } catch (Throwable e) {
            LogUtil.getLogger().error(e.getMessage(), e);
            System.exit(0);
        }

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
