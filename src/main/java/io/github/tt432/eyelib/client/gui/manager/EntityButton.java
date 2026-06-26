package io.github.tt432.eyelib.client.gui.manager;

/**
 * @author TT432
 */
public final class EntityButton {
    final String key;
    final String name;
    final String icon;
    final GuiAnimator animator = new GuiAnimator(5);

    public EntityButton(String key, String name, String icon) {
        this.key = key;
        this.name = name;
        this.icon = icon;
    }

    public String key() {
        return key;
    }

    public String name() {
        return name;
    }

    public String icon() {
        return icon;
    }

    public GuiAnimator animator() {
        return animator;
    }
}
