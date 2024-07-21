package ltd.guimc.silencefix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.faiths.utils.ClientUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ltd.guimc.silencefix.netty.AESDecoder;
import ltd.guimc.silencefix.netty.AESEncoder;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static dev.faiths.utils.IMinecraft.mc;

public class MessageHandler
        extends SimpleChannelInboundHandler<JsonElement> {
    private static final Logger logger = LogManager.getLogger((String)"IRCMessageHandler");
    private final Map<Integer, Consumer<JsonObject>> id2Func = new HashMap<Integer, Consumer<JsonObject>>();
    private final SilenceFixIRC silenceFixIRC;
    private int retriedTimes = 0;

    public MessageHandler(SilenceFixIRC SilenceFixIRC) {
        this.silenceFixIRC = SilenceFixIRC;
        this.id2Func.put(0, this::handleLog);
        this.id2Func.put(1, this::handleAuthResult);
        this.id2Func.put(2, this::handleChat);
        this.id2Func.put(4, this::handleUnauthenticated);
        this.id2Func.put(5, this::handleQueryPlayer);
        this.id2Func.put(6, this::handleMyUserInfo);
        this.id2Func.put(7, this::handleMuted);
        this.id2Func.put(8, this::handleChatFromOther);
        this.id2Func.put(9, this::handleChatFromServer);
        this.id2Func.put(10, this::handlePermissionDenied);
        this.id2Func.put(11, this::handleClients);
        this.id2Func.put(13, this::handleVersion);
    }

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, JsonElement msg) {
        try {
            int id;
            if (!msg.isJsonObject()) {
                return;
            }
            JsonObject jo = msg.getAsJsonObject();
            JsonElement idElement = jo.get("id");
            JsonElement dataElement = jo.get("data");
            if (!idElement.isJsonPrimitive()) {
                return;
            }
            if (!dataElement.isJsonObject()) {
                return;
            }
            JsonObject data = dataElement.getAsJsonObject();
            try {
                id = idElement.getAsInt();
            } catch (NumberFormatException e) {
                return;
            }
            if (id == 3) {
                channelHandlerContext.pipeline().addAfter("frame_decoder", "aes_decoder", (ChannelHandler) new AESDecoder(this.silenceFixIRC.aesKey));
                channelHandlerContext.pipeline().replace("rsa_encoder", "aes_encoder", (ChannelHandler) new AESEncoder(this.silenceFixIRC.aesKey));
                this.silenceFixIRC.sendPacket(Messages.createQueryVersion("LiquidBounce"));
                this.silenceFixIRC.sendPacket(Messages.createQueryClients());
                this.silenceFixIRC.sendPacket(Messages.createSetMinecraftProfile(getUUID()));
            } else {
                Consumer consumer = this.id2Func.get(id);
                if (consumer != null) {
                    consumer.accept(data);
                } else {
                    ClientUtils.displayChatMessage("[SFIRC] Failed to parse packet: " + id);
                    logger.warn("[SFIRC] Failed to parse packet: " + id + " -> " + data.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getUUID() {
        return mc.thePlayer == null ? mc.getSession().getPlayerID() : mc.thePlayer.getUniqueID().toString();
    }

    public void handleLog(JsonObject data) {
        String message = MessageHandler.getStringOrNull(data, "message");
        logger.warn("Server log: {}", (Object)message);
        SilenceFixIRC.LogCallback callback = (SilenceFixIRC.LogCallback)this.silenceFixIRC.callbackMap.get("log");
        if (callback != null) {
            callback.callback(message);
        }
    }

    public void handleAuthResult(JsonObject data) {
        int code = Integer.parseInt(MessageHandler.getStringOrNull(data, "code"));
        String message = MessageHandler.getStringOrNull(data, "message");
        SilenceFixIRC.AuthCallback authCallback = (SilenceFixIRC.AuthCallback)this.silenceFixIRC.callbackMap.get("auth_callback");
        if (authCallback != null) {
            authCallback.callback(message);
        }
    }

    public void handleChat(JsonObject data) {
        Minecraft mc = Minecraft.getMinecraft();
        String message = MessageHandler.getStringOrNull(data, "message");
        if (/*IRCModule.Instance.getState() && */mc.theWorld != null) {
            ClientUtils.displayChatMessage("[SFIRC] " + message);
        }
    }

    public void handleUnauthenticated(JsonObject data) {
        ClientUtils.displayChatMessage("[SFIRC] Unauthenticated");
        logger.warn("Unauthenticated");
        if (!Messages.name.isEmpty() && retriedTimes <= 10) {
            silenceFixIRC.sendPacket(Messages.createLogin(Messages.name, Messages.pass, silenceFixIRC.hwid));
            retriedTimes += 1;
        }
    }

    public void handleQueryPlayer(JsonObject data) {
        String name = MessageHandler.getStringOrNull(data, "name");
        if (name.isEmpty()) {
            return;
        }
        String rank = MessageHandler.getStringOrNull(data, "rank");
        String level = MessageHandler.getStringOrNull(data, "level");
        String mcUUID = MessageHandler.getStringOrNull(data, "mcUUID");
        int type = data.get("type").getAsInt();
        Minecraft.getMinecraft().addScheduledTask(() -> this.silenceFixIRC.ircUserMap.put(UUID.fromString(mcUUID), new IRCUser(IRCUserLevel.fromName(level), name, rank)));
    }

    public void handleMyUserInfo(JsonObject data) {
        String name = MessageHandler.getStringOrNull(data, "name");
        String rank = MessageHandler.getStringOrNull(data, "rank");
        String level = MessageHandler.getStringOrNull(data, "level");
        Minecraft.getMinecraft().addScheduledTask(() -> {
            IRCUser ircUser;
            silenceFixIRC.Instance.ircUser = ircUser = new IRCUser(IRCUserLevel.fromName(level), name, rank);
            // if (LiquidBounce.moduleManager != null) {
            //     LiquidBounce.moduleManager.onSetIRCUser(ircUser);
            // }
        });
    }

    public void handleMuted(JsonObject data) {
        if (Minecraft.getMinecraft().theWorld != null) {
            String reason = MessageHandler.getStringOrNull(data, "reason");
            String until = MessageHandler.getStringOrNull(data, "until");
            ClientUtils.displayChatMessage("[SFIRC] 你已被禁言至 " + until + " 因为：" + reason);
        }
    }

    public void handleChatFromOther(JsonObject data) {
        if (Minecraft.getMinecraft().theWorld != null) {
            String name = MessageHandler.getStringOrNull(data, "name");
            String rank = MessageHandler.getStringOrNull(data, "rank");
            String message = MessageHandler.getStringOrNull(data, "message");
            ClientUtils.displayChatMessage("[SFIRC] From " + name + "[" + rank + "]: " + message);
        }
    }

    public void handleChatFromServer(JsonObject data) {
        if (Minecraft.getMinecraft().theWorld != null) {
            String message = MessageHandler.getStringOrNull(data, "message");
            ClientUtils.displayChatMessage("[SFIRC] From server: " + message);
        }
    }

    public void handlePermissionDenied(JsonObject data) {
        logger.warn("Permission denied");
        if (Minecraft.getMinecraft().theWorld != null) {
            ClientUtils.displayChatMessage("[SFIRC] Permission denied");
        }
    }

    public void handleClients(JsonObject data) {
        if (Minecraft.getMinecraft().theWorld != null) {
            String[] parts;
            String message = MessageHandler.getStringOrNull(data, "message");
            for (String s : parts = message.split("\n")) {
                if (s.isEmpty()) continue;
                if (s.charAt(0) == '\\') {
                    char controlChar = s.charAt(1);
                    if (controlChar == 'm') {
                        ClientUtils.displayChatMessage("And more...");
                        continue;
                    }
                    if (controlChar == 'b') break;
                }
                String[] nameRank = s.split(":");
                ClientUtils.displayChatMessage(String.format("[SFIRC] %s[%s]", nameRank[0], nameRank[1]));
            }
            ClientUtils.displayChatMessage("[SFIRC] 总计：" + parts[parts.length - 1]);
        }
    }

    public void handleCommandOut(JsonObject data) {
        String message = MessageHandler.getStringOrNull(data, "message");
        logger.info("[IRCCommand]: " + message);
        if (Minecraft.getMinecraft().theWorld != null) {
            ClientUtils.displayChatMessage("[SFIRC] [IRCCommand]: " + message);
        }
    }

    public void handleVersion(JsonObject data) {
        String version = MessageHandler.getStringOrNull(data, "version");
        if (!"V8".equals(version)) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientUtils.displayChatMessage("[SFIRC] 有新版本：" + version);
                // Minecraft.getMinecraft().func_71400_g();
            });
        }
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        silenceFixIRC.Instance.reconnectTimes = 0;
        Runnable inactive = (Runnable)this.silenceFixIRC.callbackMap.get("active");
        if (inactive != null) {
            inactive.run();
        }
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Runnable inactive = (Runnable)this.silenceFixIRC.callbackMap.get("inactive");
        if (inactive != null) {
            inactive.run();
        }
    }

    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (Minecraft.getMinecraft().theWorld != null) {
            if (silenceFixIRC.Instance.reconnectTimes < 3) {
                ClientUtils.displayChatMessage("[SFIRC] 与IRC服务器断开连接，正在尝试重连。。。");
                silenceFixIRC.Instance.connect();
                ++silenceFixIRC.Instance.reconnectTimes;
            } else {
                ClientUtils.displayChatMessage("[SFIRC] 无法自动重连！ 可以使用 \".rirc\" 重连irc");
            }
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SilenceFixIRC.ExceptionCallback callback;
        Channel channel = ctx.channel();
        if (channel.isOpen()) {
            channel.close();
        }
        if ((callback = (SilenceFixIRC.ExceptionCallback)this.silenceFixIRC.callbackMap.get("exception_callback")) != null) {
            callback.callback(new Throwable("Error when handling: " + ctx.name(), cause));
        }
    }

    private static String getStringOrNull(JsonObject data, String name) {
        JsonElement jsonElement = data.get(name);
        if (jsonElement == null || !jsonElement.isJsonPrimitive()) {
            return null;
        }
        return jsonElement.getAsString();
    }
}
