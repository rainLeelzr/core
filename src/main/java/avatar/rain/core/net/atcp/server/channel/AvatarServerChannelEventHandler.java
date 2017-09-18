package avatar.rain.core.net.atcp.server.channel;

import avatar.rain.core.net.atcp.channel.BaseChannelEventHandler;
import avatar.rain.core.net.atcp.netpackage.BasePacket;
import avatar.rain.core.net.atcp.request.ATCPRequest;
import avatar.rain.core.net.atcp.server.request.AvatarServerRequestManager;
import avatar.rain.core.net.atcp.session.ATCPPSession;
import avatar.rain.core.net.atcp.session.AvatarSessionManager;
import avatar.rain.core.net.atcp.session.Session;
import avatar.rain.core.util.log.LogUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * channel事件被触发时，执行此类对应的事件处理方法
 */
public class AvatarServerChannelEventHandler extends BaseChannelEventHandler {

    private AvatarSessionManager sessionManager;

    private AvatarServerRequestManager requestManager;

    public AvatarServerChannelEventHandler(AvatarSessionManager sessionManager, AvatarServerRequestManager requestManager) {
        this.sessionManager = sessionManager;
        this.requestManager = requestManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 新的channel激活时，绑定channel与session的关系
        Channel channel = ctx.channel();

        Session session = sessionManager.getSession(channel);
        if (session != null) {
            return;
        }

        session = new ATCPPSession(ctx);
        sessionManager.addSession(ctx.channel(), session);

        LogUtil.getLogger().debug("服务器接收到客户端的连接，客户端ip：{}", channel.remoteAddress());

        super.channelRegistered(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext cx, Object object) {
        Session session = sessionManager.getSession(cx.channel());
        if (session == null) {
            LogUtil.getLogger().error("channelRead失败，channel对于的session为null");
            return;
        }

        BasePacket packet = (BasePacket) object;
        session.setUserId(packet.getUserId());

        ATCPRequest bizRequest = new ATCPRequest(packet, session);
        requestManager.addRequest(bizRequest);

        // ReferenceCountUtil.release(byteBuf);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
            IdleStateEvent event = (IdleStateEvent) evt;
            // todo 分发用户下线事件
            if (event.state() == IdleState.ALL_IDLE) {
                LogUtil.getLogger().debug("tcp超时没有读写操作，将主动关闭链接通道！");
                Session session = sessionManager.removeSession(ctx.channel());
                ctx.channel().close();
            }
        }
    }


    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Session session = sessionManager.removeSession(ctx.channel());
        LogUtil.getLogger().debug("成功关闭了一个tcp连接：{}, userId={}", ctx.channel().remoteAddress(), session.getUserId());
        // todo 分发用户下线事件
    }

}