package avatar.rain.core.net.atcp.request.worker;

import avatar.rain.core.api.Api;
import avatar.rain.core.api.ApiManager;
import avatar.rain.core.net.atcp.netpackage.BasePacket;
import avatar.rain.core.net.atcp.netpackage.PacketBodyType;
import avatar.rain.core.net.atcp.request.ATCPRequest;
import avatar.rain.core.serialization.ProtobufSerializationManager;
import avatar.rain.core.util.log.LogUtil;
import com.alibaba.fastjson.JSON;
import com.google.protobuf.GeneratedMessage;
import com.googlecode.protobuf.format.JsonFormat;

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

    private ApiManager apiManager;

    private ProtobufSerializationManager protobufSerializationManager;

    public RequestHandleWorker(ApiManager apiManager, ProtobufSerializationManager protobufSerializationManager, String workName) {
        super(workName);
        this.apiManager = apiManager;
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
        BasePacket packet = request.getPacket();
        int cmd = packet.getCmd();

        Api api = apiManager.getApi(cmd);
        if (api == null) {
            LogUtil.getLogger().error(
                    "客户端[{}]请求不存在cmd的api！请检查cmd是否正确。{}",
                    request.getSession().getRemoteIP(),
                    request.getPacket().toString());
            // todo 发送消息给客户端
            return;
        }

        // 执行业务api所需要传递的参数，从tcp包中的body部分获取解析而来
        Object[] apiArgs = parseApiParameters(packet, api);
        if (apiArgs == null) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            api.getMethod().invoke(api.getBeanClass(), apiArgs);
        } catch (Exception e) {
            LogUtil.getLogger().error("执行业务方法错误！{}", packet.toString());
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
    private Object[] parseApiParameters(BasePacket packet, Api api) {
        Object[] apiArgs = null;

        // 反序列化请求参数
        int type = packet.getType();
        if (type == PacketBodyType.Proto.getType()) {
            apiArgs = parseApiParametersFromProtobuf(packet, api);
        } else if (type == PacketBodyType.Json.getType()) {
            apiArgs = parseApiParametersFromJson(packet, api);
        } else {
            LogUtil.getLogger().error("不支持tcp头信息中的包类型为[{}]的值: {}", packet.toString());
            // todo 发送消息给客户端
        }
        return apiArgs;
    }

    /**
     * 将json格式的二进制数据封装成api需要的参数类型
     */
    private Object[] parseApiParametersFromJson(BasePacket packet, Api api) {
        String jsonStr;
        try {
            jsonStr = new String(packet.getBytes(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            LogUtil.getLogger().error(e.getMessage(), e);
            return null;
        }

        return parseJsonToParameters(api, jsonStr);
    }

    /**
     * 将proto格式的二进制数据封装成api需要的参数类型
     */
    private Object[] parseApiParametersFromProtobuf(BasePacket packet, Api api) {
        GeneratedMessage protobufJavaBean;
        try {
            protobufJavaBean = protobufSerializationManager.deserialize(api.getProtobuf(), packet.getBytes());
        } catch (Exception e) {
            LogUtil.getLogger().error(
                    "反序列化protobuf格式的request Body失败: {}",
                    packet.toString(),
                    e);
            // todo 发送消息给客户端
            return null;
        }

        LogUtil.getLogger().debug("proto内容：\n{}", protobufJavaBean.toString());
        String jsonStr = JsonFormat.printToString(protobufJavaBean);
        LogUtil.getLogger().debug("proto转成json后的内容：\n{}", jsonStr);

        return parseJsonToParameters(api, jsonStr);
    }

    /**
     * 把json数据转换成api方法的参数
     */
    private Object[] parseJsonToParameters(Api api, String jsonStr) {
        String[] parameterNames = api.getParameterNames();
        Class<?>[] parameterTypes = api.getParameterTypes();
        Map map = JSON.parseObject(jsonStr, Map.class);
        Object[] apiArgs = new Object[parameterNames.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            String parameterName = parameterNames[i];
            Object innerObj = map.get(parameterName);

            if (innerObj == null) {
                apiArgs[i] = null;
            } else if (parameterType == String.class) {
                apiArgs[i] = innerObj.toString();
            } else {
                Object o = JSON.parseObject(innerObj.toString(), parameterType);
                apiArgs[i] = o;
            }
        }
        return apiArgs;
    }

    /**
     * 接收一个事件请求，放入队列中
     */
    public final void acceptRequest(ATCPRequest event) {
        boolean ok = this.blockingQueue.offer(event);
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
