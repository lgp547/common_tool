package future;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;


import log.Logger;

/**
 * @author lgp zzh
 */
public class FutureQueryTool {

    private static final Logger log = new Logger();

    /**
     * 调用方的类名和方法名
     */
    private final String querySource;

    /**
     * 执行的线程池
     */
    private final Executor executorService;

    /**
     * 一次查询的所有 future
     */
    private final List<CompletableFuture<?>> futures = new ArrayList<>();

    /**
     * 是否开启追踪输出
     */
    private final Boolean enableTrace;

    /**
     * 保存所有的future的调用栈信息
     */
    private Map<CompletableFuture<?>, String> traceInfo;

    /**
     * 默认不开始追踪
     */
    public FutureQueryTool(Executor executorService) {
        this(executorService, false);
    }

    public FutureQueryTool(Executor executorService, Boolean enableTrace) {
        if (executorService == null || enableTrace == null) {
            throw new IllegalArgumentException("FutureQuery executorService or enableTrace is null");
        }
        this.executorService = executorService;
        this.enableTrace = enableTrace;
        this.querySource = Arrays.stream(Thread.currentThread().getStackTrace()).filter(e -> !e.getClassName().equals(Thread.class.getName())).filter(e -> !e.getClassName().equals(FutureQueryTool.class.getName())).findFirst().map(e -> e.getClassName().substring(e.getClassName().lastIndexOf(".")).replace(".", "") + "#" + e.getMethodName()).orElse("") + System.currentTimeMillis();
        if (enableTrace) {
            traceInfo = new HashMap<>();
        }
    }

    private <V> void assembleFuture(Supplier<V> supplier, Consumer<V> onComplete) {
        if (enableTrace) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String stackTraceElementStr = stackTrace.length > 3 ? stackTrace[3].toString() : "";

            CompletableFuture<V> future = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                V v = supplier.get();
                long endTime = System.currentTimeMillis();
                log.info("test.FutureQueryTool [querySource {}] [task {}] run time {}ms", querySource, stackTraceElementStr, endTime - startTime);
                return v;
            }, executorService).whenComplete((v, e) -> {
                if (e == null) {
                    onComplete.accept(v);
                }
            });

            futures.add(future);
            traceInfo.put(future, stackTraceElementStr);
        } else {
            CompletableFuture<V> future = CompletableFuture.supplyAsync(supplier, executorService).whenComplete((v, e) -> {
                if (e == null) {
                    onComplete.accept(v);
                }
            });

            futures.add(future);
        }
    }

    /**
     * 使用方自行评估多少就该退出，不要一直占用线程资源
     * 注意：超过执行时间的future会进行中断
     */
    public void allGet(Integer timeoutSeconds) {
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException exception) {
            if (enableTrace) {
                List<String> progressingTask = futures.stream().filter(Predicate.not(CompletableFuture::isDone)).map(traceInfo::get).collect(Collectors.toList());
                log.error("test.FutureQueryTool [querySource {}] query interrupted or timeout. non done tasks {}", querySource, String.join("\n\t", progressingTask), exception);
            } else {
                log.error("test.FutureQueryTool [querySource {}] query interrupted or timeout {}", querySource, exception);
            }
            throw new CancellationException(exception.getMessage());
        } catch (ExecutionException executionException) {
            log.error("test.FutureQueryTool [querySource {}] query exception {}", querySource, executionException);
            throw new IllegalArgumentException(executionException.getMessage());
        } finally {
            futures.stream().filter(Predicate.not(CompletableFuture::isDone)).forEach(e -> e.cancel(true));
            futures.clear();
        }
    }

    public void query(Runnable runnable) {
        query(() -> {
            runnable.run();
            return null;
        });
    }

    public <V> AtomicReference<V> query(Supplier<V> supplier) {
        AtomicReference<V> value = new AtomicReference<>();
        assembleFuture(supplier, value::set);
        return value;
    }

    public <K, V> Map<K, V> queryMap(Supplier<List<V>> supplier, Function<V, K> keyFunction) {
        Map<K, V> value = new HashMap<>();
        assembleFuture(supplier, v -> value.putAll(v.stream().collect(Collectors.toMap(keyFunction, Function.identity()))));
        return value;
    }

    public <K, V> Map<K, List<V>> queryGroup(Supplier<List<V>> supplier, Function<V, K> keyFunction) {
        Map<K, List<V>> value = new HashMap<>();
        assembleFuture(supplier, v -> value.putAll(v.stream().collect(Collectors.groupingBy(keyFunction))));
        return value;
    }

    public <K, V> Map<K, V> queryMap(Supplier<Map<K, V>> supplier) {
        Map<K, V> value = new HashMap<>();
        assembleFuture(supplier, value::putAll);
        return value;
    }

    public <V> List<V> queryList(Supplier<List<V>> supplier) {
        List<V> value = new ArrayList<>();
        assembleFuture(supplier, value::addAll);
        return value;
    }

    public <V> Set<V> querySet(Supplier<Set<V>> supplier) {
        Set<V> value = new HashSet<>();
        assembleFuture(supplier, value::addAll);
        return value;
    }
}
