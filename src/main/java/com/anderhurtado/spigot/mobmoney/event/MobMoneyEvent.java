package com.anderhurtado.spigot.mobmoney.event;

import org.bukkit.event.Event;

public abstract class MobMoneyEvent extends Event {

    public MobMoneyEvent() {}

    public MobMoneyEvent(boolean isAsync) {
        super(isAsync);
    }

}
