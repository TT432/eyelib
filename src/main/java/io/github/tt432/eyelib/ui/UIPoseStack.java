package io.github.tt432.eyelib.ui;

/**
 * MC 无关的坐标变换栈，用于在 {@link UIGraphics#pose()} 中执行 push/pop/translate/scale/rotate。
 *
 * @author TT432
 */
public interface UIPoseStack {
    void pushPose();

    void popPose();

    void translate(double x, double y, double z);

    void scale(float x, float y, float z);

    void rotateX(float degrees);

    void rotateY(float degrees);
}
