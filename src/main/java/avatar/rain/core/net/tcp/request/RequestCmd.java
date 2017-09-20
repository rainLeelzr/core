package avatar.rain.core.net.tcp.request;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestCmd {

    /**
     * 协议号
     */
    @AliasFor("value")
    String url();

    /**
     * 需要解析成的protobuf对应java对象的限定类名
     */
    String protobuf();
}
