package com.erp.ai.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 调用作用域缓存单元测试。
 */
class AiInvocationScopeTest {

    @AfterEach
    void tearDown() {
        AiInvocationScope.close();
    }

    /**
     * 验证作用域开启后同一缓存键只会计算一次。
     */
    @Test
    void shouldComputeOnceWithinScope() {
        AiInvocationScope.open();
        AtomicInteger invocationCount = new AtomicInteger();

        for (int index = 0; index < 5; index++) {
            Boolean value = AiInvocationScope.compute("perm:a", () -> {
                invocationCount.incrementAndGet();
                return Boolean.TRUE;
            });
            Assertions.assertTrue(value);
        }

        Assertions.assertEquals(1, invocationCount.get());
    }

    /**
     * 验证未开启作用域时直接透传，不做任何缓存。
     *
     * <p>这一点很重要：缓存只在明确开启作用域的请求/异步任务里生效，
     * 其他调用路径必须保持实时判权语义。</p>
     */
    @Test
    void shouldNotCacheWithoutScope() {
        AtomicInteger invocationCount = new AtomicInteger();

        for (int index = 0; index < 3; index++) {
            AiInvocationScope.compute("perm:a", invocationCount::incrementAndGet);
        }

        Assertions.assertFalse(AiInvocationScope.isActive());
        Assertions.assertEquals(3, invocationCount.get());
    }

    /**
     * 验证 null 结果同样被缓存，不会因为返回空而反复回源。
     */
    @Test
    void shouldCacheNullResult() {
        AiInvocationScope.open();
        AtomicInteger invocationCount = new AtomicInteger();

        for (int index = 0; index < 3; index++) {
            Object value = AiInvocationScope.compute("nullable", () -> {
                invocationCount.incrementAndGet();
                return null;
            });
            Assertions.assertNull(value);
        }

        Assertions.assertEquals(1, invocationCount.get());
    }

    /**
     * 验证主动失效后会重新计算。
     */
    @Test
    void shouldRecomputeAfterEvict() {
        AiInvocationScope.open();
        AtomicInteger invocationCount = new AtomicInteger();

        AiInvocationScope.compute("cfg", invocationCount::incrementAndGet);
        AiInvocationScope.evict("cfg");
        AiInvocationScope.compute("cfg", invocationCount::incrementAndGet);

        Assertions.assertEquals(2, invocationCount.get());
    }

    /**
     * 验证关闭作用域后缓存被清理，避免线程复用造成数据串号。
     */
    @Test
    void shouldClearCacheOnClose() {
        AiInvocationScope.open();
        AiInvocationScope.compute("perm:a", () -> Boolean.TRUE);
        AiInvocationScope.close();

        Assertions.assertFalse(AiInvocationScope.isActive());

        AiInvocationScope.open();
        AtomicInteger invocationCount = new AtomicInteger();
        AiInvocationScope.compute("perm:a", invocationCount::incrementAndGet);
        Assertions.assertEquals(1, invocationCount.get());
    }
}
