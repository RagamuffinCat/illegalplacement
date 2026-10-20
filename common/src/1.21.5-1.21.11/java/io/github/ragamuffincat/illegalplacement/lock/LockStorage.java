package io.github.ragamuffincat.illegalplacement.lock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class LockStorage {

    private static final SavedDataType<LockStore> TYPE = new SavedDataType<>(LockStore.FILE_ID, LockStore::new, LockStore.CODEC, null);

    private LockStorage() {
    }

    public static LockStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}