package avatar.rain.core.api;

import java.lang.reflect.Method;
import java.util.Arrays;

public class Api {

    /**
     * api对应的方法所在的bean对象
     */
    private Object beanClass;

    /**
     * api对应的方法
     */
    private Method method;

    /**
     * api的方法参数的类型
     */
    private Class<?>[] parameterTypes;

    /**
     * api的方法参数名称
     */
    private String[] parameterNames;

    /**
     * api参数对应的protobuf限定类名
     */
    private String protobuf;

    public String getProtobuf() {
        return protobuf;
    }

    public void setProtobuf(String protobuf) {
        this.protobuf = protobuf;
    }

    public Object getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Object beanClass) {
        this.beanClass = beanClass;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    public void setParameterNames(String[] parameterNames) {
        this.parameterNames = parameterNames;
    }

    @Override
    public String toString() {
        return new StringBuilder("{")
                .append("\"beanClass\":")
                .append(beanClass)
                .append(",\"method\":")
                .append(method)
                .append(",\"parameterNames\":")
                .append(Arrays.toString(parameterNames))
                .append(",\"protobuf\":\"")
                .append(protobuf).append('\"')
                .append('}')
                .toString();
    }
}
