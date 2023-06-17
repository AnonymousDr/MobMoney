package com.anderhurtado.test;

import dev.watchwolf.entities.Position;
import dev.watchwolf.entities.entities.Chicken;
import dev.watchwolf.entities.entities.Entity;
import dev.watchwolf.entities.entities.EntityType;
import dev.watchwolf.entities.items.Item;
import dev.watchwolf.entities.items.ItemType;
import dev.watchwolf.tester.AbstractTest;
import dev.watchwolf.tester.TesterConnector;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(Test.class)
public class Test extends AbstractTest {

    private float moneyValue;

    @Override
    public String getConfigFile() {
        return "src/test/config.yml";
    }

    @ParameterizedTest
    @ArgumentsSource(Test.class)
    public void killing(TesterConnector connector) throws Exception {
        // Spawning entity
        Position entitySpawn = connector.getPlayerPosition("Anonymous_Dr");
        Entity entity = new Chicken(entitySpawn);
        connector.spawnEntity(entity);

        connector.addOnMessage(this::onReceiveMessage);

        // TODO Read money
        float oldMoney = readMoney(connector);

        // Killing
        entity = Arrays.stream(connector.server.getEntities(entitySpawn,3)).filter(e -> e.getType().equals(EntityType.CHICKEN)).findFirst().orElseThrow(() -> new RuntimeException("Chicken spawned but not found"));
        connector.giveItem("Anonymous_Dr", new Item(ItemType.DIAMOND_SWORD));
        connector.getClientPetition("Anonymous_Dr").attack(entity);

        // Checking money
        float newMoney = readMoney(connector);

        assertEquals(oldMoney+1, newMoney);

    }

    public float readMoney(TesterConnector connector) throws IOException, InterruptedException {
        synchronized (this) {
            connector.runCommand("money Anonymous_Dr");
            this.wait(5000);
            return moneyValue;
        }
    }

    public void onReceiveMessage(String username, String message) {
        System.out.println(username +", " + message);
        if(message.startsWith("Dinero de Anonymous_Dr : $")) {
            moneyValue = Float.parseFloat(message.substring(26));
            synchronized (this) {
                this.notify();
            }
        }
    }
}
