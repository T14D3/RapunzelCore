package de.t14d3.rapunzelcore.modules.chat;

/**
 * Network payload for cross-server channel chat messages.
 *
 * <p>{@link #formattedComponentJson} is the Adventure Component serialized via
 * {@code GsonComponentSerializer.gson().serialize(component)}.</p>
 */
public class ChannelMessagePayload {
    private String channelName;
    private String formattedComponentJson;

    public ChannelMessagePayload() {
    }

    public ChannelMessagePayload(String channelName, String formattedComponentJson) {
        this.channelName = channelName;
        this.formattedComponentJson = formattedComponentJson;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getFormattedComponentJson() {
        return formattedComponentJson;
    }
}
