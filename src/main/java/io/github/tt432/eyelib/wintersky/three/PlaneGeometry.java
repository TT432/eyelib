package io.github.tt432.eyelib.wintersky.three;

import java.util.ArrayList;
import java.util.List;

/**
 * three.js r134 PlaneGeometry 移植（网格生成逻辑逐字）。
 */
public class PlaneGeometry extends BufferGeometry {

    public PlaneGeometry() {
        this(1, 1, 1, 1);
    }

    public PlaneGeometry(double width, double height, int widthSegments, int heightSegments) {
        super();
        this.type = "PlaneGeometry";

        this.parameters.put("width", width);
        this.parameters.put("height", height);
        this.parameters.put("widthSegments", widthSegments);
        this.parameters.put("heightSegments", heightSegments);

        double widthHalf = width / 2;
        double heightHalf = height / 2;

        int gridX = (int) Math.floor(widthSegments);
        int gridY = (int) Math.floor(heightSegments);

        int gridX1 = gridX + 1;
        int gridY1 = gridY + 1;

        double segmentWidth = width / gridX;
        double segmentHeight = height / gridY;

        //

        List<Integer> indices = new ArrayList<>();
        List<Double> vertices = new ArrayList<>();
        List<Double> normals = new ArrayList<>();
        List<Double> uvs = new ArrayList<>();

        for (int iy = 0; iy < gridY1; iy++) {
            double y = iy * segmentHeight - heightHalf;

            for (int ix = 0; ix < gridX1; ix++) {
                double x = ix * segmentWidth - widthHalf;

                vertices.add(x);
                vertices.add(-y);
                vertices.add(0.0);

                normals.add(0.0);
                normals.add(0.0);
                normals.add(1.0);

                uvs.add(ix / (double) gridX);
                uvs.add(1 - (iy / (double) gridY));
            }
        }

        for (int iy = 0; iy < gridY; iy++) {
            for (int ix = 0; ix < gridX; ix++) {
                int a = ix + gridX1 * iy;
                int b = ix + gridX1 * (iy + 1);
                int c = (ix + 1) + gridX1 * (iy + 1);
                int d = (ix + 1) + gridX1 * iy;

                indices.add(a);
                indices.add(b);
                indices.add(d);
                indices.add(b);
                indices.add(c);
                indices.add(d);
            }
        }

        this.setIndex(indices.stream().mapToInt(Integer::intValue).toArray());
        this.setAttribute("position", new Float32BufferAttribute(toDoubleArray(vertices), 3));
        this.setAttribute("normal", new Float32BufferAttribute(toDoubleArray(normals), 3));
        this.setAttribute("uv", new Float32BufferAttribute(toDoubleArray(uvs), 2));
    }

    private static double[] toDoubleArray(List<Double> list) {
        double[] out = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            out[i] = list.get(i);
        }
        return out;
    }
}
