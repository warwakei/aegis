package fun.aegis.utils.client.managers.event.impl;

import lombok.Getter;

public class EventLayer {

    @Getter
    protected boolean canceled = false;

    public void cancel() {
        this.canceled = true;
    }
    public void resume() {
        canceled = false;
    }
}
