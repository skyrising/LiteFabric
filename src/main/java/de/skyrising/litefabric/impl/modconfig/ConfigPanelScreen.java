package de.skyrising.litefabric.impl.modconfig;

import com.mojang.blaze3d.platform.GlStateManager;
import de.skyrising.litefabric.liteloader.Configurable;
import de.skyrising.litefabric.liteloader.LiteMod;
import de.skyrising.litefabric.liteloader.modconfig.ConfigPanel;
import de.skyrising.litefabric.liteloader.modconfig.ConfigPanelHost;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Constructor;

public class ConfigPanelScreen extends Screen implements ConfigPanelHost {
    private static final int MARGIN_LEFT = 40;
    private static final int MARGIN_RIGHT = 40;
    private static final int MARGIN_TOP = 40;
    private static final int MARGIN_BOTTOM = 0;
    private final Screen parent;
    private final LiteMod mod;
    private final ConfigPanel panel;
    protected String title;

    public ConfigPanelScreen(Screen parent, LiteMod mod) {
        this.parent = parent;
        this.mod = mod;
        this.title = mod.getName();
        if (!(mod instanceof Configurable)) throw new IllegalArgumentException(mod.getName() + " is not Configurable");
        try {
            Class<? extends ConfigPanel> panelClass = ((Configurable) mod).getConfigPanelClass();
            Constructor<? extends ConfigPanel> panelConstructor = panelClass.getConstructor();
            this.panel = panelConstructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() {
        title = panel.getPanelTitle();
        panel.onPanelShown(this);
    }

    @Override
    public void tick() {
        panel.onTick(this);
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        renderBackground();
        drawCenteredString(textRenderer, title, width / 2, 15, 0xffffff);
        GlStateManager.pushMatrix();
        GlStateManager.translated(MARGIN_LEFT, MARGIN_TOP, 0);
        panel.drawPanel(this, mouseX - MARGIN_LEFT, mouseY - MARGIN_TOP, delta);
        GlStateManager.popMatrix();
        super.render(mouseX, mouseY, delta);
    }

    @Override
    protected void mouseClicked(int x, int y, int mouseButton) {
        panel.mousePressed(this, x - MARGIN_LEFT, y - MARGIN_TOP, mouseButton);
    }

    @Override
    protected void mouseReleased(int x, int y, int mouseButton) {
        panel.mouseReleased(this, x - MARGIN_LEFT, y - MARGIN_TOP, mouseButton);
    }

    @Override
    protected void keyPressed(char chr, int keyCode) {
        panel.keyPressed(this, chr, keyCode);
    }

    @Override
    public void removed() {
        panel.onPanelHidden();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LiteMod> T getMod() {
        return (T) mod;
    }

    @Override
    public int getWidth() {
        return width - MARGIN_LEFT - MARGIN_RIGHT;
    }

    @Override
    public int getHeight() {
        return height - MARGIN_TOP - MARGIN_BOTTOM;
    }

    @Override
    public void close() {
        client.openScreen(parent);
    }
}
