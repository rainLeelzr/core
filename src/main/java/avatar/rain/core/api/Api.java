package avatar.rain.core.api;

import org.springframework.web.bind.annotation.RequestMethod;

public class Api {

    /**
     * api对应的方法
     */
    private String methodName;

    /**
     * 此方法接受的请求method
     */
    private RequestMethod[] requestMethods;

    /**
     * 按“/"分割的url
     */
    private String[] urlDivisions;

    /**
     * api参数对应的protobuf限定类名
     */
    private String protobufC2S;

    public String getProtobufC2S() {
        return protobufC2S;
    }

    public void setProtobufC2S(String protobufC2S) {
        this.protobufC2S = protobufC2S;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String[] getUrlDivisions() {
        return urlDivisions;
    }

    public void setUrlDivisions(String[] urlDivisions) {
        this.urlDivisions = urlDivisions;
    }

    public RequestMethod[] getRequestMethods() {
        return requestMethods;
    }

    public void setRequestMethods(RequestMethod[] requestMethods) {
        this.requestMethods = requestMethods;
    }
}
