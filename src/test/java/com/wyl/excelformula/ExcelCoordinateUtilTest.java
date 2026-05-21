package com.wyl.excelformula;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;

class ExcelCoordinateUtilTest {

    @Test
    void columnLetterToIndex() {
        assertEquals(0, ExcelCoordinateUtil.columnLetterToIndex("A"));
        assertEquals(1, ExcelCoordinateUtil.columnLetterToIndex("B"));
        assertEquals(25, ExcelCoordinateUtil.columnLetterToIndex("Z"));
        assertEquals(26, ExcelCoordinateUtil.columnLetterToIndex("AA"));
        assertEquals(27, ExcelCoordinateUtil.columnLetterToIndex("AB"));
        assertEquals(51, ExcelCoordinateUtil.columnLetterToIndex("AZ"));
        assertEquals(52, ExcelCoordinateUtil.columnLetterToLetter(52));
        assertEquals(701, ExcelCoordinateUtil.columnLetterToIndex("ZZ"));
        assertEquals(702, ExcelCoordinateUtil.columnLetterToIndex("AAA"));
    }

    @Test
    void columnIndexToLetter() {
        assertEquals("A", ExcelCoordinateUtil.columnIndexToLetter(0));
        assertEquals("B", ExcelCoordinateUtil.columnIndexToLetter(1));
        assertEquals("Z", ExcelCoordinateUtil.columnIndexToLetter(25));
        assertEquals("AA", ExcelCoordinateUtil.columnIndexToLetter(26));
        assertEquals("AB", ExcelCoordinateUtil.columnIndexToLetter(27));
        assertEquals("AZ", ExcelCoordinateUtil.columnIndexToLetter(51));
        assertEquals("BA", ExcelCoordinateUtil.columnIndexToLetter(52));
        assertEquals("ZZ", ExcelCoordinateUtil.columnIndexToLetter(701));
        assertEquals("AAA", ExcelCoordinateUtil.columnIndexToLetter(702));
    }

    @Test
    void excelToIndices() {
        assertArrayEquals(new int[]{1, 1}, ExcelCoordinateUtil.excelToIndices("B2")); // B是第2列（索引1），2是第2行（索引1）
        assertArrayEquals(new int[]{0, 0}, ExcelCoordinateUtil.excelToIndices("A1"));
        assertArrayEquals(new int[]{25, 0}, ExcelCoordinateUtil.excelToIndices("Z1"));
        assertArrayEquals(new int[]{26, 0}, ExcelCoordinateUtil.excelToIndices("AA1"));
        assertArrayEquals(new int[]{27, 0}, ExcelCoordinateUtil.excelToIndices("AB1"));
    }

    @Test
    void indicesToExcel() {
        assertEquals("A1", ExcelCoordinateUtil.indicesToExcel(0, 0));
        assertEquals("B2", ExcelCoordinateUtil.indicesToExcel(1, 1));
        assertEquals("Z1", ExcelCoordinateUtil.indicesToExcel(25, 0));
        assertEquals("AA1", ExcelCoordinateUtil.indicesToExcel(26, 0));
        assertEquals("AB1", ExcelCoordinateUtil.indicesToExcel(27, 0));
    }

    @Test
    void excelToPair() {
        assertEquals("[2,2]", ExcelCoordinateUtil.excelToPair("B2"));
        assertEquals("[1,1]", ExcelCoordinateUtil.excelToPair("A1"));
        assertEquals("[26,1]", ExcelCoordinateUtil.excelToPair("Z1"));
        assertEquals("[27,1]", ExcelCoordinateUtil.excelToPair("AA1"));
    }

    @Test
    void pairToExcel() {
        assertEquals("A1", ExcelCoordinateUtil.pairToExcel("(1,1)"));
        assertEquals("B2", ExcelCoordinateUtil.pairToExcel("(2,2)"));
        assertEquals("B2", ExcelCoordinateUtil.pairToExcel("[2,2]"));
    }

    @Test
    void extractCellReferencesFromFormula() {
        String f1 = "=B2*(G3+B2+H4)+SUM(A1:B5)";
        Set<String> r1 = ExcelCoordinateUtil.extractCellReferencesFromFormula(f1);
        // 函数 SUM 的字母不应被误判为单元格引用
        assertTrue(r1.contains("B2"));
        assertTrue(r1.contains("G3"));
        assertTrue(r1.contains("H4"));
        assertTrue(r1.contains("A1"));
        assertTrue(r1.contains("B5"));
        assertFalse(r1.contains("SUM"));

        String f2 = "=IF(C5>0,MAX(D5,E5),MIN(F5,G5))";
        Set<String> r2 = ExcelCoordinateUtil.extractCellReferencesFromFormula(f2);
        // IF、MAX、MIN 不应被误判
        assertTrue(r2.contains("C5"));
        assertTrue(r2.contains("D5"));
        assertTrue(r2.contains("E5"));
        assertTrue(r2.contains("F5"));
        assertTrue(r2.contains("G5"));
        assertFalse(r2.contains("IF"));
        assertFalse(r2.contains("MAX"));
        assertFalse(r2.contains("MIN"));
    }

    @Test
    void replaceCellReferencesInFormula() {
        Map<String, String> map = Map.of(
                "B2", "Sheet2!X10",
                "G3", "Sheet2!Y20"
        );
        String original = "=B2*(G3+H4)+SUM(A1:B5)";
        String replaced = ExcelCoordinateUtil.replaceCellReferencesInFormula(original, map);
        // 仅替换 B2 与 G3，保留 H4、A1、B5 和 SUM
        assertTrue(replaced.contains("Sheet2!X10"));
        assertTrue(replaced.contains("Sheet2!Y20"));
        assertTrue(replaced.contains("H4"));
        assertTrue(replaced.contains("A1"));
        assertTrue(replaced.contains("B5"));
        assertTrue(replaced.contains("SUM"));
        assertFalse(replaced.contains("B2"));
        assertFalse(replaced.contains("G3"));
    }
}