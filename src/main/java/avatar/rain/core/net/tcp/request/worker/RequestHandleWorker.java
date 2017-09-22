package avatar.rain.core.net.tcp.request.worker;

import avatar.rain.core.api.Api;
import avatar.rain.core.api.MicroServerApi;
import avatar.rain.core.api.ServerApi;
import avatar.rain.core.net.tcp.netpackage.TcpPacket;
import avatar.rain.core.net.tcp.request.ATCPRequest;
import avatar.rain.core.serialization.ProtobufSerializationManager;
import avatar.rain.core.util.log.LogUtil;
import com.google.protobuf.GeneratedMessage;
import com.googlecode.protobuf.format.JsonFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 *
 */
public class RequestHandleWorker extends Thread {

    private static final int maxQueueSize = Math.min(1000, 1000);//要设置配置

    private volatile boolean running = true;

    private BlockingQueue<ATCPRequest> blockingQueue;

    private long handledTimes = 0;//处理完的个数

    private MicroServerApi microServerApi;

    private ProtobufSerializationManager protobufSerializationManager;

    private RestTemplate restTemplate;

    public RequestHandleWorker(RestTemplate restTemplate, MicroServerApi microServerApi, ProtobufSerializationManager protobufSerializationManager, String workName) {
        super(workName);
        this.restTemplate = restTemplate;
        this.microServerApi = microServerApi;
        this.protobufSerializationManager = protobufSerializationManager;
        this.blockingQueue = new ArrayBlockingQueue<>(maxQueueSize);
        this.setDaemon(true);
    }

    public void run() {
        while (running) {
            ATCPRequest request = null;
            try {
                request = this.blockingQueue.take();
                if (request != null) {
                    LogUtil.getLogger().debug("开始处理tcp请求：{}", request.getPacket().toString());
                    handle(request);
                    handledTimes++;
                }
            } catch (Exception e) {
                LogUtil.getLogger().error(
                        "tcp请求处理错误: {}",
                        request == null ? "request=null" :
                                request.getPacket() == null ? "request.getPacket()=null" : request.getPacket().toString(),
                        e);
            }
        }
    }

    private void handle(ATCPRequest request) {
        TcpPacket packet = request.getPacket();
        String url = packet.getUrlStr();

        if (url == null || url.length() == 0) {
            LogUtil.getLogger().debug("url为空，跳过请求的处理");
            // todo 发送消息给客户端
            return;
        }

        // 从url中获取微服务的名称
        int first = url.indexOf("/");
        if (first < 0) {
            LogUtil.getLogger().debug("url错误，没有包含/，跳过请求的处理: {}", url);
            // todo 发送消息给客户端
            return;
        }

        int second = url.indexOf("/", first);
        if (second < 0) {
            LogUtil.getLogger().debug("url错误，没有包含两个/，跳过请求的处理: {}", url);
            // todo 发送消息给客户端
            return;
        }

        String serverName = url.substring(first, second);

        // 获取微服务的api
        ServerApi microServer = microServerApi.getMicroServerByServerName(serverName);
        if (microServer == null) {
            LogUtil.getLogger().error(
                    "客户端[{}]请求不存在microServer的api！请检查url是否正确。{}",
                    request.getSession().getRemoteIP(),
                    request.getPacket().toString());
            // todo 发送消息给客户端
            return;
        }

        /*
            /im/user/{id}/mm/{ij}
            /im/user/xxx-xxxx/mm/uu
         */
        String requestUrlWithoutServerName = url.substring(second);
        String[] split = requestUrlWithoutServerName.split("/");

        Map<ServerApi.UrlArray, Api> apis = microServer.getApis();
        Api api = null;

        for (Map.Entry<ServerApi.UrlArray, Api> entry : apis.entrySet()) {
            ServerApi.UrlArray urlArray = entry.getKey();
            String[] urlArrays = urlArray.getUrlArray();
            if (urlArrays.length != split.length) {
                continue;
            }

            boolean isMatch = true;

            for (int i = 0; i < urlArrays.length; i++) {
                String u = urlArrays[i];

                if (!u.startsWith("{") || !u.endsWith("}")) {
                    String requestU = split[i];
                    if (!u.equals(requestU)) {
                        isMatch = false;
                        break;
                    }
                }
            }

            if (isMatch) {
                api = entry.getValue();
            }
        }

        if (api == null) {
            LogUtil.getLogger().error(
                    "客户端[{}]请求不存在url的api！请检查url是否正确。{}",
                    request.getSession().getRemoteIP(),
                    request.getPacket().toString());
            // todo 发送消息给客户端
            return;
        }

        // 将body参数转为json，从tcp包中的body部分获取解析而来
        String json = parseApiParameters(packet, api);
        if (json == null) {
            LogUtil.getLogger().error(
                    "解析body参数，转换为json失败[{}]{}",
                    request.getSession().getRemoteIP(),
                    request.getPacket().toString());
            // todo 发送消息给客户端
            return;
        }
        LogUtil.getLogger().debug("解析body得到的请求参数：{}", json);
        long start = System.currentTimeMillis();
        try {
            String restResult = "";
            if (packet.getMethod() == TcpPacket.MethodEnum.GET.geId()) {
                restResult = restTemplate.getForObject(url, String.class);
            } else if (packet.getMethod() == TcpPacket.MethodEnum.POST.geId()) {

                HttpHeaders headers = new HttpHeaders();
                MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
                headers.setContentType(type);
                headers.add("Accept", MediaType.APPLICATION_JSON.toString());
                HttpEntity<String> formEntity = new HttpEntity<>(json, headers);

                restResult = restTemplate.postForObject(url, formEntity, String.class);
            }
            LogUtil.getLogger().debug("restTemplate执行结果：{}", restResult);
        } catch (Exception e) {
            LogUtil.getLogger().error("执行业务方法错误！{}", packet.toString(), e);
            // todo 发送消息给客户端
            return;
        } finally {
            long costTime = System.currentTimeMillis() - start;
            LogUtil.getLogger().debug(
                    "完成业务逻处理, {} 耗时={}ms",
                    packet.toString(),
                    costTime);
        }
    }

    /**
     * 将客户端传递过来的参数，封装成api需要的参数类型
     */
    private String parseApiParameters(TcpPacket packet, Api api) {
        String json = "";

        // 如果不是post请求，则不解析body
        if (packet.getMethod() != TcpPacket.MethodEnum.POST.geId()) {
            return json;
        }

        // 反序列化请求参数
        byte bodyType = packet.getBodyType();
        if (bodyType == TcpPacket.BodyTypeEnum.PROTOBUF.geId()) {
            json = parseApiParametersFromProtobuf(packet, api);
        } else if (bodyType == TcpPacket.BodyTypeEnum.JSON.geId()) {
            json = parseApiParametersFromJson(packet, api);
        } else {
            LogUtil.getLogger().error("不支持tcp头信息中的包类型为[{}]的值: {}", packet.toString());
            return null;
            // todo 发送消息给客户端
        }
        return json;
    }

    /**
     * 将json格式的二进制数据封装成api需要的参数类型
     */
    private String parseApiParametersFromJson(TcpPacket packet, Api api) {
        String jsonStr;
        try {
            jsonStr = new String(packet.getBody(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            LogUtil.getLogger().error(e.getMessage(), e);
            return null;
        }
        return jsonStr;
    }

    /**
     * 将proto格式的二进制数据封装成api需要的参数类型
     */
    private String parseApiParametersFromProtobuf(TcpPacket packet, Api api) {
        GeneratedMessage protobufJavaBean;
        try {
            protobufJavaBean = protobufSerializationManager.deserialize(api.getProtobufC2S(), packet.getBody());
        } catch (Exception e) {
            LogUtil.getLogger().error(
                    "反序列化protobuf格式的request Body失败: {}",
                    packet.toString(),
                    e);
            // todo 发送消息给客户端
            return null;
        }

        LogUtil.getLogger().debug("proto内容：{}", protobufJavaBean.toString());
        String jsonStr = JsonFormat.printToString(protobufJavaBean);
        LogUtil.getLogger().debug("proto转成json后的内容：{}", jsonStr);

        return jsonStr;
    }

    /**
     * 接收一个事件请求，放入队列中
     */
    public final void acceptRequest(ATCPRequest request) {
        boolean ok = this.blockingQueue.offer(request);
        if (!ok) {
            LogUtil.getLogger().error("添加请求到 请求队列 失败！");
        }
    }

    public void shutdown() {
        if (running) {
            running = false;
            //设置线程中断标志
            this.interrupt();
        }
    }

    public boolean isRunning() {
        return this.running && this.isAlive();
    }

}
