package avatar.rain.core.net.tcp.request.worker;

import avatar.rain.core.api.ApiManager;
import avatar.rain.core.net.tcp.TcpServerCondition;
import avatar.rain.core.net.tcp.netpackage.TcpPacket;
import avatar.rain.core.net.tcp.request.ATCPRequest;
import avatar.rain.core.serialization.ProtobufSerializationManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * 管理业务逻辑工人线程线程池
 * 分配式
 */
@Configuration
@Conditional(TcpServerCondition.class)
public class RequestHandleWorkerPool implements InitializingBean {

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
        workers = new RequestHandleWorker[this.minWorkerCount];
        for (int i = 0; i < workers.length; i++) {
            RequestHandleWorker worker = new RequestHandleWorker(apiManager, protobufSerializationManager, "worker-" + i);
            workers[i] = worker;
            worker.start();
        }
    }

    /**
     * 将网络事件包分配到指定的
     */
    public void putRequestInQueue(ATCPRequest event) {
        TcpPacket packet = event.getPacket();
        byte[] code = packet.getUserId();
        int index = code.length == 0 ? 0 : code[code.length - 1] % workers.length;
        RequestHandleWorker worker = workers[index];
        worker.acceptRequest(event);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initWorkers();
    }

    public static void main(String[] args) {
        int i = 55 % 5;
        System.out.println(i);
    }
}
