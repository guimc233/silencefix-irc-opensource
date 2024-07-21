package ltd.guimc.silencefix;

import cn.hutool.core.codec.Base64;
import com.google.gson.JsonObject;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class Messages {
    public static String name = "";
    public static String pass = "";
    public static byte[] payload = Base64.decode(new byte[]{55, 90, 87, 88, 54, 90, 83, 57, 54, 111,
            43, 65, 53, 113, 75, 109, 52, 97, 83, 116, 53, 98, 71, 101, 55, 113, 87, 101, 55, 53, 97,
            100, 52, 53, 117, 54, 53, 90, 71, 83, 53, 89, 54, 102, 53, 54, 87, 101, 53, 90, 67, 118,
            53, 89, 113, 111, 54, 73, 97, 115, 55, 76, 113, 121, 53, 53, 75, 81, 53, 111, 101, 103, 52,
            112, 117, 73, 54, 76, 50, 51, 52, 55, 101, 115, 53, 54, 113, 78, 80, 43, 109, 109, 108, 103, 61, 61});

    public static JsonObject createHandshake(SecretKey var0) {
        JsonObject data = new JsonObject();
        data.addProperty("key", Base64.encode(var0.getEncoded()));
        data.addProperty("justsimpleproperty", "永劫无间，启动！");
        return Messages.pack(0, data);
    };

    public static JsonObject createRegister(String var0, String var1, String var2, String var3, String var4) {
        JsonObject data = new JsonObject();
        name = var0;
        pass = var1;
        data.addProperty("username", var0);
        data.addProperty("password", var1);
        data.addProperty("hardwareId", var2);
        data.addProperty("qq", var3);
        data.addProperty("code", var4);
        return Messages.pack(1, data);
    };

    public static JsonObject createLogin(String var0, String var1, String var2) {
        JsonObject data = new JsonObject();
        name = var0;
        pass = var1;
        data.addProperty("username", var0);
        data.addProperty("password", var1);
        data.addProperty("hardwareId", var2);
        return Messages.pack(2, data);
    };

    public static JsonObject createChat(String message) {
        JsonObject data = new JsonObject();
        data.addProperty("message", message);
        return Messages.pack(3, data);
    }

    public static JsonObject createSetMinecraftProfile(String mcUUID) {
        JsonObject data = new JsonObject();
        data.addProperty("mcUUID", mcUUID);
        return Messages.pack(4, data);
    }

    public static JsonObject createQueryPlayer(String mcUUID, int type) {
        JsonObject data = new JsonObject();
        data.addProperty("mcUUID", mcUUID);
        data.addProperty("type", type);
        return Messages.pack(5, data);
    }

    public static JsonObject createRequestEmailCode(String qq) {
        JsonObject data = new JsonObject();
        data.addProperty("qq", qq);
        return Messages.pack(6, data);
    }

    public static JsonObject createChatToOther(String name, String message) {
        JsonObject data = new JsonObject();
        data.addProperty("name", name);
        data.addProperty("message", message);
        return Messages.pack(7, data);
    }

    public static JsonObject createQueryClients() {
        return Messages.pack(8, new JsonObject());
    }

    public static JsonObject createCommand(String command) {
        JsonObject data = new JsonObject();
        data.addProperty("command", command);
        return Messages.pack(9, data);
    }

    public static JsonObject createQueryVersion(String clientName) {
        JsonObject data = new JsonObject();
        data.addProperty("clientName", clientName);
        return Messages.pack(10, data);
    }

    public static JsonObject createVerify() {
        JsonObject data = new JsonObject();
        data.addProperty("payload", new String(payload, StandardCharsets.UTF_8));
        return Messages.pack(11, data);
    }

    private static JsonObject pack(int id, JsonObject data) {
        JsonObject jo = new JsonObject();
        jo.addProperty("id", id);
        jo.add("data", data);
        return jo;
    }
}
