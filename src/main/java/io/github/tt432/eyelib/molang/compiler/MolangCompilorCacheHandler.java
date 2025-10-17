package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangValue;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 单独的缓存处理器，将缓存逻辑与 H2 持久化集中管理。
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MolangCompilorCacheHandler implements MolangCompileCache {
    private static final Path CACHE_DIR = Paths.get(".cache", "eyelib", "compile");

    // H2 数据库配置（文件持久化）
    private static final String DB_URL = "jdbc:h2:file:./.cache/eyelib/compile/molang_cache_db;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    // 内存缓存（实例级）
    private final Map<String, MolangValue.MolangFunction> expressionCache = new HashMap<>();
    private final Map<String, MolangCompileHandler.CompiledClassInfo> classCache = new HashMap<>();

    // 自动导出相关字段
    private static final long AUTO_EXPORT_DELAY_MS = 5000; // 5秒
    private final AtomicReference<ScheduledFuture<?>> currentExportTask = new AtomicReference<>();
    private final ScheduledExecutorService autoExportExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "MolangCache-AutoExport");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean initialized = false;
    private static final String CLASS_NAME_PREFIX = "CompiledMolang$";
    private static final MolangCompilorCacheHandler INSTANCE = new MolangCompilorCacheHandler();

    public static MolangCompilorCacheHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void ensureInitialized() {
        if (!initialized) {
            synchronized (MolangCompilorCacheHandler.class) {
                if (!initialized) {
                    try {
                        H2Cache.initialize();
                        this.importCache();
                        initialized = true;
                        log.info("MolangCompilorCacheHandler initialized");
                    } catch (Exception e) {
                        log.warn("Cache init failed, continue without DB", e);
                        try {
                            H2Cache.close();
                        } catch (SQLException ignored) {
                        }
                        initialized = true; // 避免重复尝试
                    }
                }
            }
        }
    }

    @Override
    public MolangValue.MolangFunction getCachedFunction(String expression) {
        return expressionCache.get(expression);
    }

    @Override
    public void putFunctionCache(String expression, MolangValue.MolangFunction func) {
        expressionCache.put(expression, func);
    }

    @Override
    public MolangCompileHandler.CompiledClassInfo getClassInfoByClassName(String className) {
        MolangCompileHandler.CompiledClassInfo info = classCache.get(className);
        if (info != null) return info;
        // 仅查 v2
        info = H2Cache.getByClassNameV2(className);
        if (info != null) classCache.put(className, info);
        return info;
    }

    /**
     * 通过原始表达式查询持久化缓存，并在命中时同步到内存类缓存。
     */
    @Override
    public MolangCompileHandler.CompiledClassInfo getClassInfoByExpression(String expression) {
        MolangCompileHandler.CompiledClassInfo info = H2Cache.getByExpressionV2(expression);
        if (info != null) {
            classCache.put(info.className(), info);
        }
        return info;
    }

    /**
     * 为表达式分配或获取稳定的数字ID与类名（会在 v2 表中预留）。
     */
    @Override
    public MolangCompileCache.ClassNameId reserveClassNameForExpression(String expression) {
        MolangCompileCache.ClassNameId meta = H2Cache.getIdAndClassNameByExpressionV2(expression);
        if (meta != null) return meta;
        return H2Cache.reserveIdAndClassName(expression);
    }

    @Override
    public void upsertCompiledClassInfo(MolangCompileHandler.CompiledClassInfo info) {
        classCache.put(info.className(), info);
        try {
            H2Cache.upsertV2(info);
        } catch (SQLException e) {
            log.error("Persist compiled class {} to H2 failed", info.className(), e);
        }
        this.markCacheModified();
    }

    @Override
    public void exportCache() {
        try {
            Files.createDirectories(CACHE_DIR);
            int exported = H2Cache.syncMapToDb(classCache);
            log.info("Exported {} compiled expressions to H2 DB", exported);
        } catch (Exception e) {
            log.error("Failed to export cache to H2", e);
        }
    }

    @Override
    public void importCache() {
        try {
            Files.createDirectories(CACHE_DIR);
            int imported = H2Cache.loadAllInto(classCache);
            log.info("Imported {} compiled expressions from H2 DB", imported);
        } catch (Exception e) {
            log.error("Failed to import cache from H2", e);
        }
    }

    @Override
    public void shutdown() {
        ScheduledFuture<?> currentTask = currentExportTask.getAndSet(null);
        if (currentTask != null) {
            currentTask.cancel(false);
        }
        try {
            this.exportCache();
            log.info("Final cache export on shutdown");
        } catch (Exception e) {
            log.error("Final export failed", e);
        }
        autoExportExecutor.shutdown();
        try {
            if (!autoExportExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                autoExportExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            autoExportExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            H2Cache.close();
        } catch (Exception e) {
            log.error("Failed to close H2 cache", e);
        }
    }

    public void markCacheModified() {
        scheduleAutoExport();
    }

    private void scheduleAutoExport() {
        ScheduledFuture<?> previousTask = currentExportTask.get();
        if (previousTask != null) {
            previousTask.cancel(false);
        }
        ScheduledFuture<?> newTask = autoExportExecutor.schedule(() -> {
            try {
                exportCache();
                log.debug("Auto exported cache after {}ms of inactivity", AUTO_EXPORT_DELAY_MS);
            } catch (Exception e) {
                log.error("Auto export failed", e);
            } finally {
                currentExportTask.compareAndSet(Thread.currentThread().isInterrupted() ? null : currentExportTask.get(), null);
            }
        }, AUTO_EXPORT_DELAY_MS, TimeUnit.MILLISECONDS);
        currentExportTask.set(newTask);
    }

    // v1 冲突探测方法已废弃：类名分配改为使用序列预留

    private static String generateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(content.hashCode());
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * H2 持久化缓存管理
     */
    static final class H2Cache {
        private static Connection conn;
        // 避免每次查询都重复执行 DDL，建立一次后标记
        private static volatile boolean schemaReady = false;

        static void initialize() throws SQLException, IOException {
            Files.createDirectories(CACHE_DIR);
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            ensureSchema();
        }

        /**
         * 确保 H2 schema/table/sequence 就绪；若未建立则创建。一旦失败记录日志并保持可继续运行。
         */
        private static void ensureSchema() {
            try {
                if (conn == null || conn.isClosed()) {
                    Files.createDirectories(CACHE_DIR);
                    conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                }
                if (schemaReady) {
                    return;
                }
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS compiled_classes_v2 (" +
                            "id BIGINT PRIMARY KEY," +
                            "original_expression CLOB NOT NULL," +
                            "original_expression_hash VARCHAR(64)," +
                            "class_name VARCHAR(255) NOT NULL UNIQUE," +
                            "bytecode BLOB NOT NULL," +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ")");
                    // 迁移支持：旧表可能不存在哈希列
                    st.executeUpdate("ALTER TABLE compiled_classes_v2 ADD COLUMN IF NOT EXISTS original_expression_hash VARCHAR(64)");
                    // 使用哈希列来保证唯一性与索引支持
                    st.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_compiled_classes_v2_expr_hash ON compiled_classes_v2(original_expression_hash)");
                    st.executeUpdate("CREATE SEQUENCE IF NOT EXISTS compiled_class_id START WITH 1 INCREMENT BY 1");
                }
                schemaReady = true;
            } catch (SQLException | IOException e) {
                log.warn("H2 ensureSchema failed", e);
            }
        }

        static int loadAllInto(Map<String, MolangCompileHandler.CompiledClassInfo> target) {
            ensureSchema();
            if (conn == null) return 0;
            int count = 0;
            String v2sql = "SELECT original_expression, class_name, bytecode FROM compiled_classes_v2";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(v2sql)) {
                while (rs.next()) {
                    String expr = rs.getString(1);
                    String cn = rs.getString(2);
                    byte[] bytes = rs.getBytes(3);
                    target.put(cn, new MolangCompileHandler.CompiledClassInfo(expr, cn, bytes));
                    count++;
                }
            } catch (SQLException e) {
                log.error("H2 loadAllInto v2 failed", e);
            }
            return count;
        }

        static int syncMapToDb(Map<String, MolangCompileHandler.CompiledClassInfo> source) throws SQLException {
            ensureSchema();
            int updated = 0;
            // 迭代快照以避免并发修改导致的 ConcurrentModificationException
            List<MolangCompileHandler.CompiledClassInfo> snapshot = new ArrayList<>(source.values());
            for (MolangCompileHandler.CompiledClassInfo info : snapshot) {
                upsertV2(info);
                updated++;
            }
            return updated;
        }

        static void deleteAll() throws SQLException {
            ensureSchema();
            if (conn == null) return;
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM compiled_classes_v2");
            }
        }

        static void close() throws SQLException {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }

        // ===== v2 支持：数字ID与稳定类名映射 =====

        static long nextId() {
            ensureSchema();
            if (conn == null) return -1;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT NEXT VALUE FOR compiled_class_id")) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            } catch (SQLException e) {
                log.error("H2 nextId failed", e);
            }
            return -1;
        }

        static MolangCompileHandler.CompiledClassInfo getByClassNameV2(String className) {
            ensureSchema();
            if (conn == null) return null;
            String sql = "SELECT original_expression, class_name, bytecode FROM compiled_classes_v2 WHERE class_name = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, className);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String expr = rs.getString(1);
                        String cn = rs.getString(2);
                        byte[] bytes = rs.getBytes(3);
                        return new MolangCompileHandler.CompiledClassInfo(expr, cn, bytes);
                    }
                }
            } catch (SQLException e) {
                log.error("H2 getByClassNameV2 failed for {}", className, e);
            }
            return null;
        }

        static MolangCompileHandler.CompiledClassInfo getByExpressionV2(String expression) {
            ensureSchema();
            if (conn == null) return null;
            String hash = generateContentHash(expression);
            String sqlHash = "SELECT original_expression, class_name, bytecode FROM compiled_classes_v2 WHERE original_expression_hash = ? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlHash)) {
                ps.setString(1, hash);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String expr = rs.getString(1);
                        String cn = rs.getString(2);
                        byte[] bytes = rs.getBytes(3);
                        return new MolangCompileHandler.CompiledClassInfo(expr, cn, bytes);
                    }
                }
            } catch (SQLException e) {
                log.error("H2 getByExpressionV2 by hash failed", e);
            }

            return null;
        }

        static MolangCompileCache.ClassNameId getIdAndClassNameByExpressionV2(String expression) {
            ensureSchema();
            if (conn == null) return null;
            String hash = generateContentHash(expression);
            String sqlHash = "SELECT id, class_name FROM compiled_classes_v2 WHERE original_expression_hash = ? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlHash)) {
                ps.setString(1, hash);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        String cn = rs.getString(2);
                        return new MolangCompileCache.ClassNameId(id, cn);
                    }
                }
            } catch (SQLException e) {
                log.error("H2 getIdAndClassNameByExpressionV2 by hash failed", e);
            }

            return null;
        }

        static MolangCompileCache.ClassNameId reserveIdAndClassName(String expression) {
            ensureSchema();
            MolangCompileCache.ClassNameId exists = getIdAndClassNameByExpressionV2(expression);
            if (exists != null) return exists;
            long id = nextId();
            if (id <= 0) {
                String hash = generateContentHash(expression);
                String cn = CLASS_NAME_PREFIX + hash;
                return new MolangCompileCache.ClassNameId(-1, cn);
            }
            String className = CLASS_NAME_PREFIX + id;
            String sql = "INSERT INTO compiled_classes_v2 (id, original_expression, original_expression_hash, class_name, bytecode) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.setClob(2, new StringReader(expression));
                ps.setString(3, generateContentHash(expression));
                ps.setString(4, className);
                ps.setBlob(5, conn.createBlob());
                ps.executeUpdate();
                return new MolangCompileCache.ClassNameId(id, className);
            } catch (SQLException e) {
                log.debug("Reserve v2 failed, fallback to read existing", e);
                MolangCompileCache.ClassNameId meta = getIdAndClassNameByExpressionV2(expression);
                if (meta != null) return meta;
                String hash = generateContentHash(expression);
                String cn = CLASS_NAME_PREFIX + hash;
                return new MolangCompileCache.ClassNameId(-1, cn);
            }
        }

        static void updateV2BytesById(long id, byte[] bytes) {
            ensureSchema();
            if (conn == null) return;
            String sql = "UPDATE compiled_classes_v2 SET bytecode = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBinaryStream(1, new ByteArrayInputStream(bytes), bytes.length);
                ps.setLong(2, id);
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    log.warn("H2 updateV2BytesById affected 0 rows for id {}", id);
                }
            } catch (SQLException e) {
                log.error("H2 updateV2BytesById failed for id {}", id, e);
            }
        }

        static boolean upsertV2(MolangCompileHandler.CompiledClassInfo info) throws SQLException {
            ensureSchema();
            if (conn == null) return false;
            MolangCompileCache.ClassNameId meta = getIdAndClassNameByExpressionV2(info.originalExpression());
            if (meta != null) {
                String sql = "UPDATE compiled_classes_v2 SET bytecode = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setBinaryStream(1, new ByteArrayInputStream(info.bytecode()), info.bytecode().length);
                    ps.setLong(2, meta.id());
                    int affected = ps.executeUpdate();
                    if (affected == 0) {
                        // 回退：若按 id 未更新，尝试按 class_name 更新（应不常见）
                        String sqlByName = "UPDATE compiled_classes_v2 SET bytecode = ?, updated_at = CURRENT_TIMESTAMP WHERE class_name = ?";
                        try (PreparedStatement psName = conn.prepareStatement(sqlByName)) {
                            psName.setBinaryStream(1, new ByteArrayInputStream(info.bytecode()), info.bytecode().length);
                            psName.setString(2, info.className());
                            int byName = psName.executeUpdate();
                            if (byName == 0) {
                                log.warn("H2 upsertV2 affected 0 rows for id {}, class_name {}", meta.id(), info.className());
                            }
                        }
                    }
                    return true;
                }
            } else {
                Long parsedId = parseIdFromClassName(info.className());
                if (parsedId == null) return false;
                String sql = "INSERT INTO compiled_classes_v2 (id, original_expression, original_expression_hash, class_name, bytecode) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, parsedId);
                    ps.setClob(2, new StringReader(info.originalExpression()));
                    ps.setString(3, generateContentHash(info.originalExpression()));
                    ps.setString(4, info.className());
                    ps.setBinaryStream(5, new ByteArrayInputStream(info.bytecode()), info.bytecode().length);
                    int affected = ps.executeUpdate();
                    if (affected == 0) {
                        log.warn("H2 upsertV2 insert affected 0 rows for parsedId {}", parsedId);
                    }
                    return true;
                } catch (SQLException e) {
                    log.debug("Insert v2 failed, try update by expression", e);
                    meta = getIdAndClassNameByExpressionV2(info.originalExpression());
                    if (meta != null) {
                        String sql2 = "UPDATE compiled_classes_v2 SET bytecode = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                        try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                            ps2.setBinaryStream(1, new ByteArrayInputStream(info.bytecode()), info.bytecode().length);
                            ps2.setLong(2, meta.id());
                            int affected2 = ps2.executeUpdate();
                            if (affected2 == 0) {
                                log.warn("H2 upsertV2 fallback update affected 0 rows for id {}", meta.id());
                            }
                            return true;
                        }
                    }
                    throw e;
                }
            }
        }

        private static Long parseIdFromClassName(String className) {
            if (className == null) return null;
            if (!className.startsWith(CLASS_NAME_PREFIX)) return null;
            try {
                String num = className.substring(CLASS_NAME_PREFIX.length());
                return Long.parseLong(num);
            } catch (Exception e) {
                return null;
            }
        }
    }

}