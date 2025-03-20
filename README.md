# FutureQueryTool
结构性并发工具类，做到易用易读易维护易追踪，见下面说明，详情见FutureQueryToolTest和FutureQueryTool类
```java
// 常规写法，没利用用并发执行，需要等待
private static DataVO conventional() {
    List<Object> list1 = queryDbList1();
    List<Object> list2 = queryDbList2();
    Map<Object, Object> map1 = queryDbMap1();
    return new DataVO(list1, list2, map1);
}

// 常规写法，用上并发，但代码可读性和可编码性差
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

// 用上工具类，并发易用易读易维护易追踪
private static DataVO useFutureQueryTool(ExecutorService executorService) {
    FutureQueryTool futureQueryTool = new FutureQueryTool(executorService);

    List<Object> list1 = futureQueryTool.queryList(FutureQueryToolTest::queryDbList1);
    List<Object> list2 = futureQueryTool.queryList(FutureQueryToolTest::queryDbList2);
    Map<Object, Object> map1 = futureQueryTool.queryMap(FutureQueryToolTest::queryDbMap1);

    futureQueryTool.allGet(5);
    return new DataVO(list1, list2, map1);
}
```