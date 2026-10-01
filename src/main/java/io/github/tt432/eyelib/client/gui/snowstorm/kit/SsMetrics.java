package io.github.tt432.eyelib.client.gui.snowstorm.kit;
//? if >=1.20.1 {

/**
 * Snowstorm 视觉度量常量（common.css + Sidebar.vue CSS 实证值，ADR-0036 视觉对齐）。
 *
 * <p>单位均为 LDLib2 布局像素（gui 缩放缓）。颜色在
 * {@link io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme}。
 */
public interface SsMetrics {

    /** #sidebar_tab_bar height: 45px。 */
    int TAB_BAR_HEIGHT = 45;
    /** .sidebar_tab padding: 10px 4px（垂直/水平）。 */
    int TAB_PADDING_V = 10;
    int TAB_PADDING_H = 4;
    /** .input_subject h3 padding: 4px（+padding-left 12px）。 */
    int SUBJECT_TITLE_PADDING = 4;
    int SUBJECT_TITLE_PADDING_LEFT = 12;
    /** .input_group h4 padding: 10px / padding-left 12px。 */
    int GROUP_HEADER_PADDING = 10;
    int GROUP_HEADER_PADDING_LEFT = 12;
    /** .help_button 30×32。 */
    int HELP_BUTTON_WIDTH = 30;
    int HELP_BUTTON_HEIGHT = 32;
    /** .input_group > ul padding: 8px（右 2px）。 */
    int GROUP_BODY_PADDING = 8;
    /** input/select 高度（common.css：30px；sidebar 实测按 24px 档）。 */
    int INPUT_HEIGHT = 24;
    /** button padding: 8px 12px。 */
    int BUTTON_PADDING_V = 8;
    int BUTTON_PADDING_H = 12;
    /** 折叠指示条高度 32px。 */
    int FOLDED_INDICATOR_HEIGHT = 32;
    /** slider track 4px / thumb 20px 圆 2px accent 边。 */
    int SLIDER_TRACK_HEIGHT = 4;
    int SLIDER_THUMB_SIZE = 20;
    /** 图标渲染边长（lucide 24px 网格，MC 侧 12px 显示）。 */
    int ICON_SIZE = 12;
}
//?}
