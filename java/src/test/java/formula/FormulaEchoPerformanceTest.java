package formula;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 粗粒度性能冒烟：确保批量回显在约定规模下可在合理时间内完成（非严格 JMH）。
 */
class FormulaEchoPerformanceTest {

    @Test
    void batchEchoFiveThousandFormulasUnderTwoSeconds() {
        long[] cellDimIds = {10L, 20L, 30L};
        Map<String, List<int[]>> form = new HashMap<>(4096);
        for (int i = 0; i < 512; i++) {
            String key = (1000 + i) + "," + (2000 + i % 64) + ",3001";
            form.put(key, List.of(new int[] {i % 100, i % 50}));
        }

        List<FormulaEchoMatcher.FormulaRecord> formulas = new ArrayList<>(5000);
        for (int f = 0; f < 5000; f++) {
            int base = f % 512;
            String anchor = (1000 + base) + "," + (2000 + base % 64) + ",3001";
            String ref = (1000 + ((base + 1) % 512)) + "," + (2000 + ((base + 1) % 64)) + ",3001";
            String ref2 = (1000 + ((base + 2) % 512)) + "," + (2000 + ((base + 2) % 64)) + ",3001";
            formulas.add(new FormulaEchoMatcher.FormulaRecord(
                    f,
                    "=B3+B4",
                    anchor,
                    cellDimIds,
                    new long[] {f, f + 1},
                    new String[] {ref, ref2}));
        }

        long start = System.nanoTime();
        Map<String, String> echo =
                FormulaEchoMatcher.buildEchoMapForFormOpen(cellDimIds, form, formulas);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertFalse(echo.isEmpty());
        assertTrue(elapsedMs < 2000, "elapsedMs=" + elapsedMs);
    }
}
