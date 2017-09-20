package avatar.rain.core.net.tcp;

import avatar.rain.core.net.tcp.request.worker.RequestHandleWorkerPool;
import avatar.rain.core.net.tcp.channel.AvatarServerChannelInitializer;
import avatar.rain.core.net.tcp.request.AvatarServerRequestManager;
import avatar.rain.core.net.tcp.session.AvatarSessionManager;
import avatar.rain.core.serialization.ProtobufSerializationManager;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({TcpServer.class,
        RequestHandleWorkerPool.class,
        AvatarServerChannelInitializer.class,
        AvatarServerRequestManager.class,
        AvatarSessionManager.class,
        ProtobufSerializationManager.class,
        TcpServerApplicationListener.class})
public @interface EnableTcpServer {

}
