package formula;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaEchoMatcherTest {

    private static final long[] CELL_DIM_IDS = {10L, 20L, 30L};

    private static Map<String, List<int[]>> formCells(Map<String, int[][]> spec) {
        var map = new java.util.HashMap<String, List<int[]>>(spec.size());
        for (var e : spec.entrySet()) {
            var list = new ArrayList<int[]>(e.getValue().length);
            for (int[] pos : e.getValue()) {
                list.add(pos);
            }
            map.put(e.getKey(), list);
        }
        return map;
    }

    private static FormulaEchoMatcher.FormulaRecord row(
            String formula,
            String formulaPseudo,
            long[] formulaDimIds,
            String[] dagPseudos) {
        long[] dag = new long[dagPseudos.length];
        for (int i = 0; i < dagPseudos.length; i++) {
            dag[i] = 1000L + i;
        }
        return new FormulaEchoMatcher.FormulaRecord(
                1L, formula, formulaPseudo, formulaDimIds, dag, dagPseudos);
    }

    @Test
    void echoesToTablePositionWhenPseudoMatchesAfterReorder() {
        var record = row(
                "=B3+B4",
                "1001,2001,3001",
                CELL_DIM_IDS,
                new String[] {"1002,2001,3001", "1002,2002,3001"});

        var form = formCells(Map.of(
                "1001,2001,3001", new int[][] {{3, 4}},
                "1002,2001,3001", new int[][] {{3, 5}},
                "1002,2002,3001", new int[][] {{4, 5}}));

        var echo = FormulaEchoMatcher.buildEchoMapForFormOpen(CELL_DIM_IDS, form, List.of(record));

        assertEquals(Map.of("3,4", "=B3+B4"), echo);
    }

    @Test
    void reordersFormulaPseudoToCellDimOrderBeforeLookup() {
        var record = row(
                "=B3+B4",
                "2001,1001,3001",
                new long[] {20L, 10L, 30L},
                new String[] {"2001,1002,3001", "2002,1002,3001"});

        var form = formCells(Map.of(
                "1001,2001,3001", new int[][] {{1, 1}},
                "1002,2001,3001", new int[][] {{2, 1}},
                "1002,2002,3001", new int[][] {{2, 2}}));

        var echo = FormulaEchoMatcher.buildEchoMapForFormOpen(CELL_DIM_IDS, form, List.of(record));

        assertEquals(Map.of("1,1", "=B3+B4"), echo);
    }

    @Test
    void echoesToEveryPositionWhenOnePseudoMapsToMultipleCells() {
        var record = row("=B3", "1001,2001,3001", CELL_DIM_IDS, new String[] {"1002,2001,3001"});

        var form = formCells(Map.of(
                "1001,2001,3001", new int[][] {{3, 4}, {5, 6}},
                "1002,2001,3001", new int[][] {{3, 5}}));

        var echo = FormulaEchoMatcher.buildEchoMapForFormOpen(CELL_DIM_IDS, form, List.of(record));

        assertEquals(2, echo.size());
        assertEquals("=B3", echo.get("3,4"));
        assertEquals("=B3", echo.get("5,6"));
    }

    @Test
    void skipsWhenDagPseudoNotOnForm() {
        var record = row(
                "=B3+B4",
                "1001,2001,3001",
                CELL_DIM_IDS,
                new String[] {"1002,2001,3001", "1002,2002,3001"});

        var form = formCells(Map.of(
                "1001,2001,3001", new int[][] {{3, 4}},
                "1002,2001,3001", new int[][] {{3, 5}}));

        assertTrue(FormulaEchoMatcher.buildEchoMapForFormOpen(
                CELL_DIM_IDS, form, List.of(record)).isEmpty());
    }

    @Test
    void skipsCrossSheetFormula() {
        var record = row(
                "=B6+表单2!B6",
                "1001,2001,3001",
                CELL_DIM_IDS,
                new String[] {"1002,2001,3001", "1002,2002,3001"});

        var form = formCells(Map.of(
                "1001,2001,3001", new int[][] {{3, 4}},
                "1002,2001,3001", new int[][] {{3, 5}},
                "1002,2002,3001", new int[][] {{4, 5}}));

        assertTrue(FormulaEchoMatcher.buildEchoMapForFormOpen(
                CELL_DIM_IDS, form, List.of(record)).isEmpty());
    }

    @Test
    void batchEchoAllFormulasFromTable() {
        var f1 = row("=B3", "1001,2001,3001", CELL_DIM_IDS, new String[] {"1002,2001,3001"});
        var f2 = row("=C3", "1101,2101,3101", CELL_DIM_IDS, new String[] {"1102,2101,3101"});

        var form = formCells(Map.of(
                "1001,2001,3001", new int[][] {{0, 0}},
                "1002,2001,3001", new int[][] {{1, 0}},
                "1101,2101,3101", new int[][] {{0, 1}},
                "1102,2101,3101", new int[][] {{1, 1}}));

        var echo = FormulaEchoMatcher.buildEchoMapForFormOpen(
                CELL_DIM_IDS, form, List.of(f1, f2));

        assertEquals(2, echo.size());
        assertEquals("=B3", echo.get("0,0"));
        assertEquals("=C3", echo.get("0,1"));
    }
}
