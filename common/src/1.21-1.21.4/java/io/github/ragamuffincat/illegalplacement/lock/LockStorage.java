package io.github.ragamuffincat.illegalplacement.lock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class LockStorage {

    private LockStorage() {
    }

    public static LockStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(LockStore::new, (tag, registries) -> LockStore.read(tag), null), LockStore.FILE_ID);
    }
}
