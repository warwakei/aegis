package rich.modules.impl.combat.macetarget.state;

public class MaceState {

    public enum Stage {
        PREPARE,
        FLYING_UP,
        TARGETTING,
        ATTACKING
    }

    public enum SwapPhase {
        IDLE, PRE_STOP, STOPPING, WAIT_STOP, PRE_SWAP, DO_SWAP, POST_SWAP, RESUMING
    }

    public enum FireworkPhase {
        IDLE, PRE_STOP, STOPPING, WAIT_STOP, PRE_SWAP, SWAP_TO_HAND, AWAIT_ITEM, USE, POST_USE, SWAP_BACK, RESUMING
    }
}