package com.haro.inventory.waitingroom.service;

/**
 * Centralizes Redis key naming for the waiting room.
 * <p>
 * All per-resource keys share the {@code {resourceId}} hash tag so that
 * (a) Redis Cluster routes them to the same slot, and
 * (b) Lua scripts can touch any of them atomically.
 */
public final class WaitingRoomKeys {

    public static final String ROOMS_REGISTRY = "wr:rooms";

    private WaitingRoomKeys() {
    }

    public static String seq(String resourceId) {
        return "wr:{" + resourceId + "}:seq";
    }

    public static String queue(String resourceId) {
        return "wr:{" + resourceId + "}:queue";
    }

    public static String active(String resourceId) {
        return "wr:{" + resourceId + "}:active";
    }

    public static String tokenHashPrefix(String resourceId) {
        return "wr:{" + resourceId + "}:tok:";
    }

    public static String tokenHash(String resourceId, String token) {
        return tokenHashPrefix(resourceId) + token;
    }

    public static String userIndex(String resourceId) {
        return "wr:{" + resourceId + "}:uidx";
    }

    public static String config(String resourceId) {
        return "wr:{" + resourceId + "}:cfg";
    }
}
