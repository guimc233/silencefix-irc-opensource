package ltd.guimc.silencefix;

import cn.hutool.crypto.digest.DigestUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.GenericFutureListener;
import ltd.guimc.silencefix.netty.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.client.Minecraft.logger;

public class SilenceFixIRC {
    public static SilenceFixIRC Instance;
    public IRCUser ircUser;
    public int reconnectTimes;
    public final Map<UUID, IRCUser> ircUserMap = new HashMap<>();
    public final Map<String, BaseCallback> callbackMap = new HashMap<>();
    EventLoopGroup workerGroup;
    Channel channel;
    SecretKey aesKey;
    public String hwid;

    public static void init() {
        Instance = new SilenceFixIRC();
        Instance.callbackMap.put("log", new ltd.guimc.silencefix.callback.LogCallback());
        Instance.callbackMap.put("auth_callback", new ltd.guimc.silencefix.callback.AuthCallback());
        Instance.callbackMap.put("exception_callback", new ltd.guimc.silencefix.callback.AuthCallback());
    }

    public void connect() throws Exception {
        hwid = generateHardwareId();
        final Bootstrap bootstrap = new Bootstrap();
        workerGroup = new NioEventLoopGroup();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(workerGroup);
        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel channel) {
                channel.pipeline().addLast("frame_decoder", new FrameDecoder());
                channel.pipeline().addLast("string_decoder", new StringDecoder(StandardCharsets.UTF_8));
                channel.pipeline().addLast("string_to_json", new StringToJsonDecoder());
                channel.pipeline().addLast("frame_encoder", new FrameEncoder());
                channel.pipeline().addLast("rsa_encoder", new RSAEncoder());
                channel.pipeline().addLast("string_encoder", new StringEncoder(StandardCharsets.UTF_8));
                channel.pipeline().addLast("json_to_string", new JsonToStringEncoder());
                channel.pipeline().addLast(new MessageHandler(SilenceFixIRC.this));
                SilenceFixIRC.this.channel = channel;
            }
        });
        bootstrap.connect("cn-wx.kuangmoge.xyz", 41201).sync().addListener(future -> {
            lambda$connect$1((ChannelFuture) future);
        });
    }

    public void shutdown() {
        if (this.channel != null && this.channel.isOpen()) {
            this.channel.close();
        }
        if (this.workerGroup != null && !this.workerGroup.isShutdown()) {
            this.workerGroup.shutdownGracefully();
        }
    }

    public void sendPacket(Object data) {
        Channel channel = this.channel;
        if (channel != null && channel.isOpen()) {
            if (channel.eventLoop().inEventLoop()) {
                channel.writeAndFlush(data).addListener((GenericFutureListener) ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            } else {
                channel.eventLoop().submit(() -> this.sendPacket(data));
            }
        } else {
            logger.warn("silencefix irc not connected");
        }
    }

    public Map<String, BaseCallback> getCallbackMap() {
        return this.callbackMap;
    }

    public Channel getChannel() {
        return this.channel;
    }

    private static SecretKey genAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // for example
        return keyGen.generateKey();
    };

    public static String generateHardwareId() throws Exception {
        String input = "sb";
        String sha256 = DigestUtil.sha256Hex(input);
        return sha256;
    };

    private /* synthetic */ void lambda$connect$1(ChannelFuture channelFuture) throws Exception {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (channelFuture.isSuccess()) {
                this.channel = channelFuture.channel();
                try {
                    this.aesKey = SilenceFixIRC.genAESKey();
                    this.sendPacket(Messages.createHandshake(this.aesKey));
                    this.sendPacket(Messages.createVerify());
                }
                catch (Exception e) {
                    this.channel.close();
                    ExceptionCallback callback = (ExceptionCallback) this.callbackMap.get("exception_callback");
                    if (callback != null) {
                        callback.callback(channelFuture.cause());
                    }
                }
            } else {
                ExceptionCallback callback = (ExceptionCallback)this.callbackMap.get("exception_callback");
                if (callback != null) {
                    callback.callback(channelFuture.cause());
                }
            }
        });
    }

    public String getUserTag(UUID uuid) {
        try {
            IRCUser userName = SilenceFixIRC.Instance.ircUserMap.get(uuid);
            String[] rank = new String[]{"免费", "付费", "管理"};
            return EnumChatFormatting.BLUE + "(SF|" + rank[userName.level.getPriority()] + "|" + userName.name + ") " + EnumChatFormatting.RESET;
        } catch (Exception ignored) {
            return "";
        }
    }

    public static interface BaseCallback<T> {
        public void callback(T t);
    }

    public static interface ExceptionCallback extends BaseCallback<Throwable> {}

    public static interface LogCallback extends BaseCallback<String> {}

    public static interface AuthCallback extends BaseCallback<String> {}
}
