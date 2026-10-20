package io.github.ragamuffincat.illegalplacement.mixin;

import io.github.ragamuffincat.illegalplacement.lock.LockHolder;
import io.github.ragamuffincat.illegalplacement.lock.LockStorage;
import io.github.ragamuffincat.illegalplacement.lock.LockStore;
import io.github.ragamuffincat.illegalplacement.server.PasteTicks;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements LockHolder {

    // Returned while the level is still being constructed and has no chunk source
    @Unique
    private static final LockStore ILLEGALPLACEMENT$EMPTY = new LockStore();

    @Unique
    private LockStore illegalplacement$locks;

    @Override
    public LockStore illegalplacement$getLocks() {
        LockStore store = this.illegalplacement$locks;
        if (store == null) {
            ServerLevel self = (ServerLevel) (Object) this;
            if (self.getChunkSource() == null) {
                return ILLEGALPLACEMENT$EMPTY;
            }
            store = LockStorage.get(self);
            this.illegalplacement$locks = store;
        }
        return store;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void illegalplacement$settlePaste(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        PasteTicks.tick((ServerLevel) (Object) this);
    }
}
