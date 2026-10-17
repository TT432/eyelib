package io.github.tt432.eyelib.bridge.client.model;

import io.github.tt432.eyelib.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ModelPart} / {@link PartPose} 跨版本字段访问的封装。
 *
 * <p>&lt;26.1 中 {@code PartPose} / {@code ModelPart.Polygon} / {@code ModelPart.Vertex} 为带公开字段的类，
 * 26.1 起改为 record（需访问器方法）。本 Port 收敛该差异，供 application 层
 * （如 {@code client/model/ModelPartModel}）以稳定 API 访问。
 *
 * @author TT432
 */
public interface ModelPartPort {

    /**
     * @return PartPose 的位移分量 {@code (x, y, z)}
     */
    static Vector3f posePosition(PartPose pose) {
        //? if <26.1 {
        return new Vector3f(pose.x, pose.y, pose.z);
        //?} else {
        return new Vector3f(pose.x(), pose.y(), pose.z());
        //?}
    }

    /**
     * @return PartPose 的旋转分量 {@code (xRot, yRot, zRot)}
     */
    static Vector3f poseRotation(PartPose pose) {
        //? if <26.1 {
        return new Vector3f(pose.xRot, pose.yRot, pose.zRot);
        //?} else {
        return new Vector3f(pose.xRot(), pose.yRot(), pose.zRot());
        //?}
    }

    /**
     * 将 {@link ModelPart.Cube} 转换为领域 {@link Model.Cube}。
     * 收敛 {@code Polygon}/{@code Vertex} 字段 vs 访问器差异（&lt;26.1 vs 26.1）。
     */
    static Model.Cube createCube(ModelPart.Cube cube) {
        List<Model.Face> faces = new ArrayList<>();
        //? if <26.1 {
        for (ModelPart.Polygon polygon : cube.polygons) {
            List<Model.Vertex> vertices = new ArrayList<>();

            for (ModelPart.Vertex vertex : polygon.vertices) {
                vertices.add(new Model.Vertex(vertex.pos, new Vector2f(vertex.u, vertex.v), polygon.normal));
            }

            faces.add(new Model.Face(vertices, polygon.normal));
        }
        //?} else {
        for (ModelPart.Polygon polygon : cube.polygons) {
            List<Model.Vertex> vertices = new ArrayList<>();
            Vector3fc normal = polygon.normal();

            for (ModelPart.Vertex vertex : polygon.vertices()) {
                vertices.add(new Model.Vertex(new Vector3f(vertex.x(), vertex.y(), vertex.z()), new Vector2f(vertex.u(), vertex.v()), normal));
            }

            faces.add(new Model.Face(vertices, normal));
        }
        //?}

        return new Model.Cube(faces);
    }
}
