package avatar.rain.core.api;

import java.util.Map;

public class ServerApi {

    /**
     * 获取api的时间
     */
    private long time = 0;

    private Map<String, Api> requestMappingApis;

    public ServerApi(long time, Map<String, Api> requestMappingApis) {
        this.time = time;
        this.requestMappingApis = requestMappingApis;
    }

    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return new StringBuilder("{")
                .append("\"time\":")
                .append(time)
                .append(",\"requestMappingApis\":")
                .append(requestMappingApis)
                .append('}')
                .toString();
    }

}
