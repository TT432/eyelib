package io.github.tt432.eyelib.bridge.client.render;

/**
 * 批次作用域：由 {@link RenderSink#openBatch(BatchPolicy)} 返回，{@code close()} 结束批次。
 *
 * <p>实现契约：
 * <ul>
 *   <li>{@link #close()} 幂等；<b>只有最外层</b>作用域的 close 真正绘制（sink 内深度计数归零时）。</li>
 *   <li>作用域内 {@link RenderSink#flush()} 不绘制；作用域外行为与不开批次时完全一致。</li>
 *   <li>作用域必须 try-with-resources 使用：调用方漏掉 close 时 sink 只能在下一次 flush
 *       丢弃并释放批次缓冲（几何丢失，且会 WARN 一次）。</li>
 * </ul>
 *
 * @author TT432
 */
public interface BatchScope extends AutoCloseable {

    /** 结束本作用域；幂等，最外层 close 才真正绘制。 */
    @Override
    void close();

    /** 当前统计快照（生命周期累计值，见 {@link BatchStats}）。 */
    BatchStats stats();

    /** 本作用域是否仍打开（已 close 或 sink 批次已结束时为 false）。 */
    boolean isOpen();

    /**
     * 立即刷的保守默认：不支持批次的 sink 返回它，所有调用都退化为「关闭时什么也不做」。
     * 不支持批次时 {@link RenderSink#openBatch(BatchPolicy)} 会先 flush，行为与不开批次一致。
     */
    BatchScope NOOP = new BatchScope() {
        @Override
        public void close() {
            // 无缓冲可刷：立即实现的语义已在 openBatch 的 flush() 里完成。
        }

        @Override
        public BatchStats stats() {
            return new BatchStats(0, 0, 0, 0, 0);
        }

        @Override
        public boolean isOpen() {
            return false;
        }
    };
}
