package fun.aegis.utils.features.price;

import net.minecraft.component.ComponentMap;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import java.util.regex.Pattern;

public class PriceParser {
    private final Pattern funTimePricePattern = Pattern.compile("\\$(\\d+(?:\\s\\d{3})*(?:\\.\\d{2})?)");

    public int getPrice(ItemStack stack) {
        ComponentMap tag = stack.getComponents();
        if (tag == null) return -1;
        String componentString = tag.toString();
        String price = StringUtils.substringBetween(componentString, "literal{ $", "}[style={color=green}]");
        if (price == null || price.isEmpty()) {
            String customName = stack.getName().getString();
            if (customName != null) {
                java.util.regex.Matcher matcher = funTimePricePattern.matcher(customName);
                if (matcher.find()) {
                    price = matcher.group(1);
                }
            }
        }
        if (price == null || price.isEmpty()) return -1;
        try {
            price = price.replaceAll("[\\s,]", "");
            return Integer.parseInt(price);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
