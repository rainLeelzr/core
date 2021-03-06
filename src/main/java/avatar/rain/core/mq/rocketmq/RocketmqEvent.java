package avatar.rain.core.mq.rocketmq;

import avatar.rain.core.util.log.LogUtil;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.context.ApplicationEvent;

import java.io.UnsupportedEncodingException;

public class RocketmqEvent extends ApplicationEvent {

    private DefaultMQPushConsumer consumer;

    private MessageExt messageExt;

    private String topic;

    private String tag;

    private byte[] body;

    private String stringMessage;

    public RocketmqEvent(MessageExt msg, DefaultMQPushConsumer consumer) throws Exception {
        super(msg);
        this.topic = msg.getTopic();
        this.tag = msg.getTags();
        this.body = msg.getBody();
        this.consumer = consumer;
        this.messageExt = msg;
    }

    public String getStringMsg() {
        if (this.stringMessage == null) {
            try {
                this.stringMessage = new String(this.body, "utf-8");
            } catch (UnsupportedEncodingException e) {
                LogUtil.getLogger().error(e.getMessage(), e);
            }
        }

        return this.stringMessage;
    }

    public String getTopic() {
        return topic;
    }

    public String getTag() {
        return tag;
    }

    public byte[] getBody() {
        return body;
    }
}
