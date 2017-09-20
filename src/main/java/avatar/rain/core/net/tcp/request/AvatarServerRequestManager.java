package avatar.rain.core.net.tcp.request;

import avatar.rain.core.net.tcp.request.worker.RequestHandleWorkerPool;

import javax.annotation.Resource;

/**
 * 请求管理器
 * 根据客户端的消息命令，分发到各个指定的方法中去处理请求
 */
public class AvatarServerRequestManager {

    /**
     * 处理客户端请求的线程池
     */
    @Resource
    private RequestHandleWorkerPool requestHandleWorkerPool;

    public void addRequest(ATCPRequest bizRequest) {
        this.requestHandleWorkerPool.putRequestInQueue(bizRequest);
    }

}
