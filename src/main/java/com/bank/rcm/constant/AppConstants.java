package com.bank.rcm.constant;

/**
 * 应用全局常量定义
 * <p>包含系统配置、日志前缀、时间阈值等常量</p>
 */
public class AppConstants {
    
    // ============ 日志前缀 ============
    /** OB业务日志前缀 */
    public static final String LOG_OB_PREFIX = "【OB日志】";
    
    /** Cube接口验证日志前缀 */
    public static final String LOG_CUBE_PREFIX = "【Cube验证】";
    
    /** 性能监控日志前缀 */
    public static final String LOG_MONITOR_PREFIX = "【性能监控】";
    
    /** Excel读取日志前缀 */
    public static final String LOG_EXCEL_PREFIX = "【Excel处理】";
    
    // ============ 时间阈值（毫秒） ============
    /** 性能监控 - 正常耗时阈值（INFO级别） */
    public static final long PERF_THRESHOLD_INFO = 1000L;
    
    /** 性能监控 - 警告耗时阈值（WARN级别） */
    public static final long PERF_THRESHOLD_WARN = 3000L;
    
    // ============ 线程池配置 ============
    /** Cube异步校验线程池 - 核心线程数 */
    public static final int EXECUTOR_CUBE_CORE_SIZE = 15;
    
    /** Cube异步校验线程池 - 最大线程数 */
    public static final int EXECUTOR_CUBE_MAX_SIZE = 30;
    
    /** Cube异步校验线程池 - 队列容量 */
    public static final int EXECUTOR_CUBE_QUEUE_CAPACITY = 500;
    
    /** Cube异步校验线程池 - 线程名前缀 */
    public static final String EXECUTOR_CUBE_THREAD_PREFIX = "CubeAsync-";
    
    // ============ 业务配置 ============
    /** 批量处理每批记录数 */
    public static final int BATCH_SIZE = 100;
    
    /** CEAM ID拆分分隔符 */
    public static final String CEAM_SPLIT_CHAR = "\\|";
    
    /** Cube接口模拟调用延迟（毫秒） */
    public static final long CUBE_CALL_DELAY_MS = 200;
    
    /** 模拟数据中合法数据的概率阈值 */
    public static final int VALID_DATA_PROBABILITY = 100;
}
