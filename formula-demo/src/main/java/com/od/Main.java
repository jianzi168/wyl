package com.od;

import com.od.formula.CellCoordinateConverter;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;

        int[] roundTripResult = runRoundTripTests();
        passed += roundTripResult[0];
        failed += roundTripResult[1];

        int[] directResult = runDirectConversionTests();
        passed += directResult[0];
        failed += directResult[1];

        int[] invalidResult = runInvalidAddressTests();
        passed += invalidResult[0];
        failed += invalidResult[1];

        int[] bufferResult = runBufferTests();
        passed += bufferResult[0];
        failed += bufferResult[1];

        System.out.println();
        System.out.println("合计: 通过 " + passed + ", 失败 " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** @return int[]{passed, failed} */
    private static int[] runRoundTripTests() {
        int passed = 0;
        int failed = 0;

        record Case(String excel, int row, int col) {}

        Case[] cases = {
                new Case("A1", 0, 0),
                new Case("B5", 4, 1),
                new Case("Z99", 98, 25),
                new Case("AA26", 25, 26),
                new Case("AZ100", 99, 51),
                new Case("XFD1", 0, CellCoordinateConverter.MAX_COLUMN_INDEX),
                new Case("A1048576", CellCoordinateConverter.MAX_ROW_INDEX, 0),
                new Case("XFD1048576", CellCoordinateConverter.MAX_ROW_INDEX, CellCoordinateConverter.MAX_COLUMN_INDEX),
        };

        System.out.println("=== 往返转换 (Excel → 数字 → Excel) ===");
        for (Case testCase : cases) {
            try {
                int[] numeric = CellCoordinateConverter.toNumeric(testCase.excel());
                String back = CellCoordinateConverter.toExcel(numeric);

                if (numeric[0] != testCase.row() || numeric[1] != testCase.col()) {
                    fail("往返", testCase.excel(),
                            "数字坐标期望 [" + testCase.row() + ", " + testCase.col() + "], 实际 "
                                    + Arrays.toString(numeric));
                    failed++;
                    continue;
                }
                if (!back.equals(testCase.excel())) {
                    fail("往返", testCase.excel(), "还原期望 " + testCase.excel() + ", 实际 " + back);
                    failed++;
                    continue;
                }
                ok("往返", testCase.excel() + " ↔ " + Arrays.toString(numeric));
                passed++;
            } catch (Exception exception) {
                fail("往返", testCase.excel(), exception.toString());
                failed++;
            }
        }

        System.out.println();
        System.out.println("=== 大小写与 $ 前缀 ===");
        String[][] aliasCases = {
                {"b5", "B5"},
                {"$C$10", "C10"},
                {"$aa$1", "AA1"},
        };
        for (String[] aliasCase : aliasCases) {
            try {
                int[] fromAlias = CellCoordinateConverter.toNumeric(aliasCase[0]);
                int[] fromCanonical = CellCoordinateConverter.toNumeric(aliasCase[1]);
                String back = CellCoordinateConverter.toExcel(fromAlias);

                if (!Arrays.equals(fromAlias, fromCanonical)) {
                    fail("别名", aliasCase[0], "与 " + aliasCase[1] + " 数字坐标不一致: "
                            + Arrays.toString(fromAlias) + " vs " + Arrays.toString(fromCanonical));
                    failed++;
                } else if (!back.equals(aliasCase[1])) {
                    fail("别名", aliasCase[0], "还原为 " + back + ", 期望 " + aliasCase[1]);
                    failed++;
                } else {
                    ok("别名", aliasCase[0] + " → " + back);
                    passed++;
                }
            } catch (Exception exception) {
                fail("别名", aliasCase[0], exception.toString());
                failed++;
            }
        }

        return new int[] {passed, failed};
    }

    /** @return int[]{passed, failed} */
    private static int[] runDirectConversionTests() {
        int passed = 0;
        int failed = 0;

        System.out.println();
        System.out.println("=== 直接转换 API ===");

        if ("D2".equals(CellCoordinateConverter.toExcel(1, 3))) {
            ok("toExcel(row,col)", "toExcel(1, 3) = D2");
            passed++;
        } else {
            fail("toExcel(row,col)", "toExcel(1, 3)", "期望 D2");
            failed++;
        }

        int[] numeric = {9, 2};
        if ("C10".equals(CellCoordinateConverter.toExcel(numeric))) {
            ok("toExcel(int[])", "toExcel([9, 2]) = C10");
            passed++;
        } else {
            fail("toExcel(int[])", Arrays.toString(numeric), "期望 C10");
            failed++;
        }

        return new int[] {passed, failed};
    }

    /** @return int[]{passed, failed} */
    private static int[] runInvalidAddressTests() {
        int passed = 0;
        int failed = 0;

        System.out.println();
        System.out.println("=== 非法地址应抛异常 ===");

        String[] invalidAddresses = {"", "5B", "A", "A0", "A1B2", "AAAA1", "A1048577"};
        for (String address : invalidAddresses) {
            if (expectIllegalArgument(() -> CellCoordinateConverter.toNumeric(address))) {
                ok("非法", "\"" + address + "\" → IllegalArgumentException");
                passed++;
            } else {
                fail("非法", address, "应抛出 IllegalArgumentException");
                failed++;
            }
        }

        if (expectIllegalArgument(() -> CellCoordinateConverter.toExcel(-1, 0))) {
            ok("非法", "toExcel(-1, 0) → IllegalArgumentException");
            passed++;
        } else {
            fail("非法", "toExcel(-1, 0)", "应抛出 IllegalArgumentException");
            failed++;
        }

        if (expectIllegalArgument(() -> CellCoordinateConverter.toExcel(new int[] {0}))) {
            ok("非法", "toExcel([0]) → IllegalArgumentException");
            passed++;
        } else {
            fail("非法", "toExcel([0])", "应抛出 IllegalArgumentException");
            failed++;
        }

        return new int[] {passed, failed};
    }

    /** @return int[]{passed, failed} */
    private static int[] runBufferTests() {
        int passed = 0;
        int failed = 0;

        System.out.println();
        System.out.println("=== 可复用 Buffer ===");

        var buffer = new CellCoordinateConverter.Buffer();
        int[] numeric = buffer.parse("B5");
        if (numeric[0] == 4 && numeric[1] == 1 && "B5".equals(buffer.format())) {
            ok("Buffer", "parse + format = B5");
            passed++;
        } else {
            fail("Buffer", "B5", Arrays.toString(numeric) + " / " + buffer.format());
            failed++;
        }

        int[] reused = new int[2];
        CellCoordinateConverter.parseNumeric("$D$12", reused, 0);
        if (reused[0] == 11 && reused[1] == 3 && "D12".equals(CellCoordinateConverter.toExcel(reused[0], reused[1]))) {
            ok("Buffer", "parseNumeric(dest) = [11, 3]");
            passed++;
        } else {
            fail("Buffer", "parseNumeric", Arrays.toString(reused));
            failed++;
        }

        return new int[] {passed, failed};
    }

    private static boolean expectIllegalArgument(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException ignored) {
            return true;
        }
    }

    private static void ok(String group, String message) {
        System.out.println("[通过] " + group + ": " + message);
    }

    private static void fail(String group, String input, String message) {
        System.out.println("[失败] " + group + " (" + input + "): " + message);
    }
}
