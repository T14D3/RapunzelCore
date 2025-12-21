package de.t14d3.rapunzelcore.messaging;

/**
 * Transport envelope for RapunzelCore cross-server messages.
 *
 * <p>We use a single Minecraft plugin message channel (e.g. {@code rapunzelcore:bridge}) as the transport.
 * Inside that channel, we multiplex logical channels via {@link #channel} and route via {@link #target}.</p>
 */
public class NetworkEnvelope {

    public enum Target {
        /** Deliver to Velocity only (do not forward to backends). */
        PROXY,
        /** Deliver to all backend servers that currently have at least one player connected. */
        ALL,
        /** Deliver to one backend server (if it currently has at least one player connected). */
        SERVER
    }

    /** Envelope schema version for future-proofing. */
    private int v = 1;

    /** Logical channel (e.g. "chat.channel_message"). */
    private String channel;

    /** Payload data, typically JSON. */
    private String data;

    /** Routing target. */
    private Target target;

    /** When {@link #target} is {@link Target#SERVER}, this is the backend server name. */
    private String targetServer;

    /** Origin server name (Velocity backend name); filled/normalized by proxy. */
    private String sourceServer;

    /** Client timestamp (millis) for debugging only. */
    private long createdAt;

    public NetworkEnvelope() {
    }

    public NetworkEnvelope(String channel, String data, Target target, String targetServer, String sourceServer, long createdAt) {
        this.channel = channel;
        this.data = data;
        this.target = target;
        this.targetServer = targetServer;
        this.sourceServer = sourceServer;
        this.createdAt = createdAt;
    }

    public int getV() {
        return v;
    }

    public String getChannel() {
        return channel;
    }

    public String getData() {
        return data;
    }

    public Target getTarget() {
        return target;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public String getSourceServer() {
        return sourceServer;
    }

    public void setSourceServer(String sourceServer) {
        this.sourceServer = sourceServer;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
