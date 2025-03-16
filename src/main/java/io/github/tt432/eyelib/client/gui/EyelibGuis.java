package io.github.tt432.eyelib.client.gui;

import lombok.experimental.UtilityClass;

/**
 * @author TT432
 */
@UtilityClass
public class EyelibGuis {
    @FunctionalInterface
    public interface DrawStringAction {
        void draw(int xOffset, String string);
    }

    /**
     * example: getBoldString("123456789", 2, 6) // 12§l3456§r789
     */
    public String getBoldString(String value, int highlightStart, int highlightEnd) {
        String startSubString = value.substring(0, highlightStart);
        String centerSubString = "§l" + value.substring(highlightStart, highlightEnd);
        String endSubString = "§r" + value.substring(highlightEnd);
        return startSubString + centerSubString + endSubString;
    }
}
