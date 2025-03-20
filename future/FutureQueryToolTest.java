package future;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureQueryToolTest {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);


    /**
     * 需求：返回 DataVO
     */
    public static void main(String[] args) {
        System.out.println("main start");

        // 常规写法，没利用用并发执行，需要等待
        long start = System.currentTimeMillis();
        DataVO dataVO1 = conventional();
        long end = System.currentTimeMillis();
        System.out.println("dataVO1 time " + (end - start) / 1000 + "秒");

        // 常规写法，用上并发，但代码可读性和可编码性差
        start = System.currentTimeMillis();
        DataVO dataVO2 = futureQuery(executorService);
        end = System.currentTimeMillis();
        System.out.println("dataVO2 time " + (end - start) / 1000 + "秒");

        // 用上工具类，并发易用易读易维护易追踪
        start = System.currentTimeMillis();
        DataVO dataVO3 = useFutureQueryTool(executorService);
        end = System.currentTimeMillis();
        System.out.println("dataVO3 time " + (end - start) / 1000 + "秒");

        System.out.println("main end");
        executorService.shutdown();
    }


    private static DataVO conventional() {
        List<Object> list1 = queryDbList1();
        List<Object> list2 = queryDbList2();
        Map<Object, Object> map1 = queryDbMap1();
        return new DataVO(list1, list2, map1);
    }

    private static DataVO futureQuery(ExecutorService executorService) {
        CompletableFuture<List<Object>> list1Future = CompletableFuture.supplyAsync(FutureQueryToolTest::queryDbList1, executorService);
        CompletableFuture<List<Object>> list2Future = CompletableFuture.supplyAsync(FutureQueryToolTest::queryDbList2, executorService);
        CompletableFuture<Map<Object, Object>> map1Future = CompletableFuture.supplyAsync(FutureQueryToolTest::queryDbMap1, executorService);
        List<Object> list1;
        List<Object> list2;
        Map<Object, Object> map1;
        try {
            list1 = list1Future.get(5, TimeUnit.SECONDS);
            list2 = list2Future.get(5, TimeUnit.SECONDS);
            map1 = map1Future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        return new DataVO(list1, list2, map1);
    }

    private static DataVO useFutureQueryTool(ExecutorService executorService) {
        FutureQueryTool futureQueryTool = new FutureQueryTool(executorService);

        List<Object> list1 = futureQueryTool.queryList(FutureQueryToolTest::queryDbList1);
        List<Object> list2 = futureQueryTool.queryList(FutureQueryToolTest::queryDbList2);
        Map<Object, Object> map1 = futureQueryTool.queryMap(FutureQueryToolTest::queryDbMap1);

        futureQueryTool.allGet(5);
        return new DataVO(list1, list2, map1);
    }

    private static List<Object> queryDbList1() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        return Collections.emptyList();
    }

    private static List<Object> queryDbList2() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
        return Collections.emptyList();
    }

    private static Map<Object, Object> queryDbMap1() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        return Collections.emptyMap();
    }

    private static class DataVO {
        List<Object> list1;

        List<Object> list2;

        Map<Object, Object> map1;

        public DataVO(List<Object> list1, List<Object> list2, Map<Object, Object> map1) {
            this.list1 = list1;
            this.list2 = list2;
            this.map1 = map1;
        }
    }


}
