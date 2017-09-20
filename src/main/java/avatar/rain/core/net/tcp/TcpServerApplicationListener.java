package avatar.rain.core.net.tcp;

import avatar.rain.core.api.ApiManager;
import avatar.rain.core.serialization.ProtobufSerializationManager;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.annotation.Resource;

@Configuration
@Conditional(TcpServerCondition.class)
public class TcpServerApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Resource
    private ApiManager apiManager;

    @Resource
    private ProtobufSerializationManager protobufSerializationManager;

    @Resource
    private TcpServer tcpServer;

    private boolean alreadyInitialized = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (alreadyInitialized) {
            return;
        }

        apiManager.init();
        protobufSerializationManager.init();
        new Thread("netty-starter") {

            @Override
            public void run() {
                tcpServer.start();
            }
        }.start();

        alreadyInitialized = true;
    }

}
