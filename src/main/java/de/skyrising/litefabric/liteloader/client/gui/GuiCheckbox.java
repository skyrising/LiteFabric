package de.skyrising.litefabric.liteloader.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;

public class GuiCheckbox extends ButtonWidget {
    private static final int SIZE = 12;
    private static final int CHECK_BOX_SIZE = 11;
    private static final int CHECK_BOX_MARGIN = 2;
    private static final int EXTRA_MARGIN = 1;
    private static final int CHECK_BOX_SIZE_TOTAL = CHECK_BOX_SIZE + CHECK_BOX_MARGIN * 2 + EXTRA_MARGIN;
    private static final int TEXT_OFFSET_Y = (SIZE - 8) / 2;
    private static final int TEXT_OFFSET_X = CHECK_BOX_SIZE_TOTAL;
    public boolean checked;

    public GuiCheckbox(int id, int x, int y, String label) {
        super(id, x, y, MinecraftClient.getInstance().textRenderer.getStringWidth(label) + CHECK_BOX_SIZE_TOTAL, SIZE, label);
    }

    @Override
    public void method_891(MinecraftClient client, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        fill(x + CHECK_BOX_MARGIN, y, x + width, y + height - EXTRA_MARGIN, 0x33ffffff);
        hovered = mouseX >= x + CHECK_BOX_MARGIN && mouseY >= y && mouseX < x + width && mouseY < y + height - EXTRA_MARGIN;
        int x1 = x + CHECK_BOX_MARGIN;
        int x2 = x + CHECK_BOX_MARGIN + CHECK_BOX_SIZE;
        int y1 = y;
        int y2 = y + CHECK_BOX_SIZE;
        fill(x1, y1, x2, y2, 0xffffffff);
        fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xff333333);
        if (checked) {
            fill(x1 + 2, y1 + 2, x2 - 2, y2 - 2, 0xff00e000);
        }
        renderBg(client, mouseX, mouseY);
        int textColor = 0xe0e0e0;
        if (!active) {
            textColor = 0xa0a0a0;
        } else if (hovered) {
            textColor = 0xffffa0;
        }
        drawWithShadow(client.textRenderer, message, x + TEXT_OFFSET_X, y + TEXT_OFFSET_Y, textColor);
    }
}
