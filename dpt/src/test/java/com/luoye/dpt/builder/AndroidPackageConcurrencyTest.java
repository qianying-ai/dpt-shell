package com.luoye.dpt.builder;

import com.luoye.dpt.model.Instruction;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AndroidPackageConcurrencyTest {

    @Test
    public void instructionMapIsSafeForConcurrentWritesAndIteratesByDexIndex() throws Exception {
        Map<Integer, List<Instruction>> map = AndroidPackage.newInstructionMap();
        int entries = 128;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            for (int i = entries - 1; i >= 0; --i) {
                final int dexIndex = i;
                pool.submit(() -> {
                    start.await();
                    map.put(dexIndex, Collections.emptyList());
                    return null;
                });
            }
            start.countDown();
            pool.shutdown();
            Assert.assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        Assert.assertEquals(entries, map.size());
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < entries; ++i) {
            expected.add(i);
        }
        Assert.assertEquals(expected, new ArrayList<>(map.keySet()));
        Assert.assertEquals(Arrays.asList(0, entries - 1),
                Arrays.asList(map.keySet().iterator().next(), ((java.util.SortedMap<Integer, ?>) map).lastKey()));
    }
}
