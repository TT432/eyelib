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
public class Vec4d {
    private double x;
    private double y;
    private double z;
    private double w;

    public void set(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
}
