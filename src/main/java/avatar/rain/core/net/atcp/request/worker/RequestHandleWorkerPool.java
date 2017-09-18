package avatar.rain.core.net.atcp.request.worker;

import avatar.rain.core.api.ApiManager;
import avatar.rain.core.net.atcp.netpackage.BasePacket;
import avatar.rain.core.net.atcp.request.ATCPRequest;
import avatar.rain.core.serialization.ProtobufSerializationManager;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 管理业务逻辑工人线程线程池
 * 分配式
 */
@Component
public class RequestHandleWorkerPool {

    private int minWorkerCount = 5;//最少的工人队列

    private int maxWorkerCount = 30;//最大的工人队列

    private RequestHandleWorker[] workers;

    @Resource
    private ApiManager apiManager;

    @Resource
    private ProtobufSerializationManager protobufSerializationManager;

    /**
     * 初始化工作线程
     */
    public void initWorkers() {
        workers = new RequestHandleWorker[this.maxWorkerCount];
        for (int i = 0; i < this.minWorkerCount; i++) {
            RequestHandleWorker worker = new RequestHandleWorker(apiManager, protobufSerializationManager, "worker-" + i);
            workers[i] = worker;
            worker.start();
        }
    }

    /**
     * 将网络事件包分配到指定的
     */
    public void putRequestInQueue(ATCPRequest event) {
        BasePacket packet = event.getPacket();
        int code = packet.getCode();
        int index = code % workers.length;
        RequestHandleWorker worker = workers[index];
        worker.acceptRequest(event);
    }

}
