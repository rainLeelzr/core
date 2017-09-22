package avatar.rain.core.api;

import avatar.rain.core.net.tcp.request.Protobuf;
import avatar.rain.core.util.log.LogUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public class LocalApiManager implements ApplicationContextAware {

    private ServerApi serverApi;

    public ServerApi getServerApi() {
        return serverApi;
    }

    private ApplicationContext applicationContext;

    /**
     * 初始化api管理器的事件
     */
    private long initTime = 0;

    public long getInitTime() {
        return initTime;
    }

    /**
     * 需要在所有spring bean加载完后，才调用本方法
     */
    public void init() {
        LogUtil.getLogger().info("正在初始化ApiManager...");
        try {
            Map<String, Api> tempApis = new HashMap<>();

            // 遍历所有含有@RequestCmd注解的bean
            String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
            String[] emptyArray = new String[]{""};
            for (String beanDefinitionName : beanDefinitionNames) {
                Object bean = applicationContext.getBean(beanDefinitionName);

                Class<?> beanClass = bean.getClass();

                RequestMapping classRequestMapping = beanClass.getAnnotation(RequestMapping.class);

                String[] classPaths = classRequestMapping == null
                        ? emptyArray
                        : classRequestMapping.value().length == 0
                        ? classRequestMapping.path().length == 0 ? emptyArray : classRequestMapping.path()
                        : classRequestMapping.value();

                Method[] declaredMethods = beanClass.getDeclaredMethods();
                for (Method targetApiMethod : declaredMethods) {
                    Protobuf annotation = targetApiMethod.getAnnotation(Protobuf.class);
                    if (annotation == null) {
                        continue;
                    }

                    RequestMapping methodRequestMapping = targetApiMethod.getAnnotation(RequestMapping.class);
                    if (classRequestMapping == null) {
                        continue;
                    }

                    String[] methodPaths = methodRequestMapping.value().length == 0
                            ? methodRequestMapping.path().length == 0 ? emptyArray : methodRequestMapping.path()
                            : methodRequestMapping.value();

                    // 拼装url
                    for (String classPath : classPaths) {
                        for (String methodPath : methodPaths) {
                            String url = classPath + methodPath;
                            // 这个url理论上不会重复，因为在spring加载mapping时，已经进行了判断.如果存在重复的url，则spring不会启动成功
                            if (tempApis.containsKey(url)) {
                                throw new Error("方法[" +
                                        targetApiMethod.toString() +
                                        "]的requestMapping与[" +
                                        tempApis.get(url).getMethodName() +
                                        "]重复，请修改。url=" +
                                        url);
                            }
                            LogUtil.getLogger().debug("加载到{}", url);

                            Api api = new Api();
                            api.setMethodName(targetApiMethod.toString());
                            api.setProtobufC2S(annotation.c2s());

                            tempApis.put(url, api);
                        }
                    }
                }
            }

            apis = tempApis;
            initTime = System.currentTimeMillis();
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
