package ltd.guimc.silencefix;

public enum IRCUserLevel {
    FREE("Free", 0),
    PAID("Paid", 1),
    ADMINISTRATOR("Administrator", 2);

    private final String name;
    private final int priority;

    private IRCUserLevel(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public String getName() {
        return this.name;
    }

    public int getPriority() {
        return this.priority;
    }

    public static IRCUserLevel fromName(String name) {
        for (IRCUserLevel value : IRCUserLevel.values()) {
            if (!value.name.equals(name)) continue;
            return value;
        }
        return null;
    }
}
