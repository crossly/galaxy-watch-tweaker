package dev.ricky.galaxywatchtweaker.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class BluetoothReconnectControllerTest {
    static final class Device implements BluetoothReconnectController.Device {
        boolean enabled = true, bonded = true, connected, accepted = true;
        int requests;
        public boolean isEnabled() { return enabled; }
        public boolean isBonded() { return bonded; }
        public boolean isConnected() { return connected; }
        public boolean connect() { requests++; return accepted; }
    }

    @Test public void offlineBluetoothDoesNotRequestOrConsumeCooldown() {
        BluetoothReconnectController controller = new BluetoothReconnectController();
        Device device = new Device();
        device.enabled = false;
        assertFalse(controller.reconnect(1, device, 100));
        assertEquals(0, device.requests);
        device.enabled = true;
        assertTrue(controller.reconnect(1, device, 101));
    }
    @Test public void neverPairsAnUnbondedWatchOrReconnectsAConnectedWatch() {
        BluetoothReconnectController controller = new BluetoothReconnectController();
        Device device = new Device();
        device.bonded = false;
        assertFalse(controller.reconnect(1, device, 0));
        device.bonded = true;
        device.connected = true;
        assertFalse(controller.reconnect(1, device, 1));
        assertEquals(0, device.requests);
    }
    @Test public void acceptedAsyncRequestDoesNotClaimTheTransportIsConnected() {
        BluetoothReconnectController controller = new BluetoothReconnectController();
        Device device = new Device();
        assertTrue(controller.reconnect(1, device, 0));
        assertFalse(device.connected);
        assertFalse(controller.reconnect(1, device, 59_999));
        assertTrue(controller.reconnect(1, device, 60_000));
        assertEquals(2, device.requests);
    }
    @Test public void rejectedRequestIsThrottledAndDoesNotBlockOtherWatches() {
        BluetoothReconnectController controller = new BluetoothReconnectController();
        Device rejected = new Device();
        rejected.accepted = false;
        assertFalse(controller.reconnect(1, rejected, 0));
        assertFalse(controller.reconnect(1, rejected, 500));
        assertEquals(1, rejected.requests);
        assertTrue(controller.reconnect(2, new Device(), 500));
    }
}
