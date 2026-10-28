package rich.util.modules.autoparser;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import rich.modules.impl.misc.autoparser.AutoParser;

public class DiscountSliderWidget extends SliderWidget {

    public DiscountSliderWidget(int x, int y, int width, int height, int initialValue) {
        super(x, y, width, height, Text.literal("Уменьшить цены на: " + initialValue + "%"), (initialValue - 10) / 80.0);
    }

    @Override
    protected void updateMessage() {
        int percent = (int) (this.value * 80) + 10;
        this.setMessage(Text.literal("Уменьшить цены на: " + percent + "%"));
    }

    @Override
    protected void applyValue() {
        int percent = (int) (this.value * 80) + 10;
        AutoParser parser = AutoParser.getInstance();
        if (parser != null) {
            parser.setDiscountPercent(percent);
        }
    }
}