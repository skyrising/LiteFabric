package de.skyrising.litefabric.liteloader.modconfig;

public interface ConfigPanel {
    String getPanelTitle();
    int getContentHeight();
    void onPanelShown(ConfigPanelHost host);
    void onPanelResize(ConfigPanelHost host);
    void onPanelHidden();
    void onTick(ConfigPanelHost host);
    void drawPanel(ConfigPanelHost host, int mouseX, int mouseY, float partialTicks);
    void mousePressed(ConfigPanelHost host, int mouseX, int mouseY, int mouseButton);
    void mouseReleased(ConfigPanelHost host, int mouseX, int mouseY, int mouseButton);
    void mouseMoved(ConfigPanelHost host, int mouseX, int mouseY);
    void keyPressed(ConfigPanelHost host, char keyChar, int keyCode);
}
