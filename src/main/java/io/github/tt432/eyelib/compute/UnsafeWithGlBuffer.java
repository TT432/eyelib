package io.github.tt432.eyelib.compute;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL44.GL_DYNAMIC_STORAGE_BIT;
import static org.lwjgl.opengl.GL45.*;

/**
 * @author TT432
 */
@Slf4j
class UnsafeWithGlBuffer {
    public int glPointer;
    public long pointer;
    public int capacity;
    public int writeOffset;

    public UnsafeWithGlBuffer(int baseSize) {
        glPointer = glCreateBuffers();
        capacity = 1024 * 4 * baseSize;
        this.pointer = MemoryUtil.nmemAlloc(capacity);
        glNamedBufferStorage(glPointer, capacity, GL_DYNAMIC_STORAGE_BIT);
    }

    public long reserve(int bytes) {
        int i = this.writeOffset;
        int j = i + bytes;
        this.ensureCapacity(j);
        this.writeOffset = j;
        return this.pointer + (long) i;
    }

    private void ensureCapacity(int size) {
        if (size > this.capacity) {
            int i = Math.min(this.capacity, 2097152);
            int j = Math.max(this.capacity + i, size);
            this.resize(j);
            resizeGl(j);
        }
    }

    private void resizeGl(int newSize) {
        glDeleteBuffers(glPointer);
        glPointer = glCreateBuffers();
        glNamedBufferStorage(glPointer, newSize, GL_DYNAMIC_STORAGE_BIT);
    }

    private void resize(int newSize) {
        this.pointer = MemoryUtil.nmemRealloc(this.pointer, newSize);
        log.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", this.capacity, newSize);
        if (this.pointer == 0L) {
            throw new OutOfMemoryError("Failed to resize buffer from " + this.capacity + " bytes to " + newSize + " bytes");
        } else {
            this.capacity = newSize;
        }
    }

    void reset() {
        writeOffset = 0;
    }

    void bind(int index) {
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, index, glPointer);
    }

    void upload() {
        nglNamedBufferSubData(glPointer, 0, writeOffset, pointer);
    }
}
