package io.github.tt432.eyelib.common.bedrock.model.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UvFaces implements Serializable {
    /**
     * Specifies the UV's for the face that stretches along the x and z axes, and
     * faces the -y axis
     */
    private FaceUv down;
    /**
     * Specifies the UV's for the face that stretches along the z and y axes, and
     * faces the x axis
     */
    private FaceUv east;
    /**
     * Specifies the UV's for the face that stretches along the x and y axes, and
     * faces the -z axis.
     */
    private FaceUv north;
    /**
     * Specifies the UV's for the face that stretches along the x and y axes, and
     * faces the z axis
     */
    private FaceUv south;
    /**
     * Specifies the UV's for the face that stretches along the x and z axes, and
     * faces the y axis
     */
    private FaceUv up;
    /**
     * Specifies the UV's for the face that stretches along the z and y axes, and
     * faces the -x axis
     */
    private FaceUv west;
}
