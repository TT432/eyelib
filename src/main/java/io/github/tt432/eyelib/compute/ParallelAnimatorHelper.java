package io.github.tt432.eyelib.compute;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.model.GPUParallelModel;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_MAP_READ_BIT;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL43.glDispatchCompute;
import static org.lwjgl.opengl.GL44.GL_MAP_COHERENT_BIT;
import static org.lwjgl.opengl.GL44.GL_MAP_PERSISTENT_BIT;
import static org.lwjgl.opengl.GL45.*;

/**
 * @author TT432
 */
public class ParallelAnimatorHelper {
    private static final UnsafeWithGlBuffer animationOffsets = new UnsafeWithGlBuffer(4);
    private static final UnsafeWithGlBuffer animation = new UnsafeWithGlBuffer(4 * 3 * 4);
    private static final PersistentPoseBuffer poses = new PersistentPoseBuffer();
    private static final UnsafeWithGlBuffer entityBasePose = new UnsafeWithGlBuffer(16 * 4 + 12 * 4);

    public static List<Int2ObjectMap<PoseStack.Pose>> parallelAnimator(
            List<ModelWithAnimation<?>> modelWithAnimations,
            List<PoseStack.Pose> posesList
    ) {
        var animationInfo = GPUParallelModel.put(modelWithAnimations, posesList, animationOffsets, animation, entityBasePose);

        int posesSize = (16 + 12) * 4 * animationInfo.parallelNumGroups();
        poses.ensureCapacity(posesSize);

        animationOffsets.upload();
        animation.upload();
        entityBasePose.upload();

        glUseProgram(EyelibComputeShaders.getParallelAnimatorShader().program());
        animationOffsets.bind(0);
        animation.bind(1);
        poses.bind(2);
        entityBasePose.bind(3);

        glDispatchCompute(animationInfo.parallelNumGroups(), 1, 1);

        poses.lock();

        var result = GPUParallelModel.getAnimationBones(animationInfo.animationBones(), getPoseList());
        clear();
        return result;
    }

    public static void clear() {
        animationOffsets.reset();
        animation.reset();
        poses.reset();
        entityBasePose.reset();
    }

    private static List<PoseStack.Pose> getPoseList() {
        long ptr = poses.getReadablePointer();
        List<PoseStack.Pose> posesList = new ArrayList<>();
        int strideBytes = (16 + 12) * 4;
        var size = poses.writeOffset / strideBytes;
        for (int i = 0; i < size; i++) {
            int pos = i * strideBytes;
            int normalPos = pos + 16 * 4;
            posesList.add(new PoseStack.Pose(
                    new Matrix4f(MemoryUtil.memFloatBuffer(ptr + pos, 16)),
                    new Matrix3f(
                            MemoryUtil.memGetFloat(ptr + normalPos),
                            MemoryUtil.memGetFloat(ptr + normalPos + 4),
                            MemoryUtil.memGetFloat(ptr + normalPos + 8),
                            MemoryUtil.memGetFloat(ptr + normalPos + 16),
                            MemoryUtil.memGetFloat(ptr + normalPos + 20),
                            MemoryUtil.memGetFloat(ptr + normalPos + 24),
                            MemoryUtil.memGetFloat(ptr + normalPos + 32),
                            MemoryUtil.memGetFloat(ptr + normalPos + 36),
                            MemoryUtil.memGetFloat(ptr + normalPos + 40)
                    )
            ));
        }
        return posesList;
    }

    private static class PersistentPoseBuffer {
        private final int[] buffers;
        private final long[] pointers;
        private final long[] fences;
        private final int frameCount = 3;
        private int currentFrame = 0;
        private int lastReadyFrame = -1;
        private int capacity = 0;
        public int writeOffset = 0;

        public PersistentPoseBuffer() {
            buffers = new int[frameCount];
            pointers = new long[frameCount];
            fences = new long[frameCount];
        }

        public void ensureCapacity(int size) {
            writeOffset = size;
            if (size <= capacity) return;

            capacity = size;
            int flags = GL_MAP_READ_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT;

            for (int i = 0; i < frameCount; i++) {
                if (buffers[i] != 0) {
                    glDeleteBuffers(buffers[i]);
                    buffers[i] = 0;
                }
                if (fences[i] != 0) {
                    glDeleteSync(fences[i]);
                    fences[i] = 0;
                }

                buffers[i] = glCreateBuffers();
                glNamedBufferStorage(buffers[i], size, flags);
                pointers[i] = nglMapNamedBufferRange(buffers[i], 0, size, flags);
            }
            currentFrame = 0;
            lastReadyFrame = -1;
        }

        public void bind(int index) {
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, index, buffers[currentFrame]);
        }

        public void lock() {
            if (fences[currentFrame] != 0) {
                glDeleteSync(fences[currentFrame]);
            }
            fences[currentFrame] = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
            currentFrame = (currentFrame + 1) % frameCount;
        }

        public long getReadablePointer() {
            int checkFrame = (currentFrame - 1 + frameCount) % frameCount;
            long fence = fences[checkFrame];

            if (fence != 0) {
                int status = glClientWaitSync(fence, GL_SYNC_FLUSH_COMMANDS_BIT, 0);
                if (status == GL_ALREADY_SIGNALED || status == GL_CONDITION_SATISFIED) {
                    lastReadyFrame = checkFrame;
                    return pointers[checkFrame];
                }
            }

            if (lastReadyFrame != -1) {
                return pointers[lastReadyFrame];
            }

            if (fence != 0) {
                glClientWaitSync(fence, GL_SYNC_FLUSH_COMMANDS_BIT, GL_TIMEOUT_IGNORED);
                lastReadyFrame = checkFrame;
                return pointers[checkFrame];
            }

            // Fallback if no fence (should not happen if ensureCapacity called)
            return pointers[checkFrame];
        }

        public void reset() {
            writeOffset = 0;
        }
    }

    public static void putMatrix3f(long address, Matrix3f matrix) {
        MemoryUtil.memPutFloat(address + 0L * 4L, matrix.m00());
        MemoryUtil.memPutFloat(address + 1L * 4L, matrix.m01());
        MemoryUtil.memPutFloat(address + 2L * 4L, matrix.m02());

        MemoryUtil.memPutFloat(address + 4L * 4L, matrix.m10());
        MemoryUtil.memPutFloat(address + 5L * 4L, matrix.m11());
        MemoryUtil.memPutFloat(address + 6L * 4L, matrix.m12());

        MemoryUtil.memPutFloat(address + 8L * 4L, matrix.m20());
        MemoryUtil.memPutFloat(address + 9L * 4L, matrix.m21());
        MemoryUtil.memPutFloat(address + 10L * 4L, matrix.m22());
    }

    public static void putMatrix4f(long address, Matrix4f matrix) {
        MemoryUtil.memPutFloat(address + 0L * 4L, matrix.m00());
        MemoryUtil.memPutFloat(address + 1L * 4L, matrix.m01());
        MemoryUtil.memPutFloat(address + 2L * 4L, matrix.m02());
        MemoryUtil.memPutFloat(address + 3L * 4L, matrix.m03());

        MemoryUtil.memPutFloat(address + 4L * 4L, matrix.m10());
        MemoryUtil.memPutFloat(address + 5L * 4L, matrix.m11());
        MemoryUtil.memPutFloat(address + 6L * 4L, matrix.m12());
        MemoryUtil.memPutFloat(address + 7L * 4L, matrix.m13());

        MemoryUtil.memPutFloat(address + 8L * 4L, matrix.m20());
        MemoryUtil.memPutFloat(address + 9L * 4L, matrix.m21());
        MemoryUtil.memPutFloat(address + 10L * 4L, matrix.m22());
        MemoryUtil.memPutFloat(address + 11L * 4L, matrix.m23());

        MemoryUtil.memPutFloat(address + 12L * 4L, matrix.m30());
        MemoryUtil.memPutFloat(address + 13L * 4L, matrix.m31());
        MemoryUtil.memPutFloat(address + 14L * 4L, matrix.m32());
        MemoryUtil.memPutFloat(address + 15L * 4L, matrix.m33());
    }
}
