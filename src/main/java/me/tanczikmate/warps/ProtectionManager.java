package me.tanczikmate.warps;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectionManager {

    private final Map<UUID, Long> protectedPlayers = new ConcurrentHashMap<>();

    public void protect(UUID uuid, int seconds) {
        if (seconds <= 0) {
            return;
        }
        protectedPlayers.put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }

    public boolean isProtected(UUID uuid) {
        Long expireTime = protectedPlayers.get(uuid);
        if (expireTime == null) {
            return false;
        }
        if (System.currentTimeMillis() < expireTime) {
            return true;
        }
        protectedPlayers.remove(uuid);
        return false;
    }

    public void removeProtection(UUID uuid) {
        protectedPlayers.remove(uuid);
    }
}
