package com.example.monitorenergtico;

public interface Listener {
    void setOptions();
    void setConfDevice();
    void setViewDevices();
    void setViewEnergyCons(String name, String topic);
    void setConsHistory();
    void InfConn(boolean isConnected);
}