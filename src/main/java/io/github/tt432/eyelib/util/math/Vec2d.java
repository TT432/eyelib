package io.github.tt432.eyelib.util.math;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author DustW
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vec2d {
    private double x;
    private double y;

    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
