package formula;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormulaCoordinateReplacerTest {

    private static FormulaCoordinateReplacer.ReplacementTable table(
            String[][] local, String[][] sheet) {
        var b = FormulaCoordinateReplacer.ReplacementTable.builder();
        if (local != null) {
            for (String[] pair : local) {
                b.local(pair[0], pair[1]);
            }
        }
        if (sheet != null) {
            for (String[] pair : sheet) {
                b.sheet(pair[0], pair[1]);
            }
        }
        return b.build();
    }

    private static void assertReplace(String expected, String input, FormulaCoordinateReplacer.ReplacementTable table) {
        assertEquals(expected, FormulaCoordinateReplacer.replace(input, table));
    }

    @Nested
    class MainScenario {

        @Test
        void replacesLocalAndSheetRefsWithoutSubstringCollision() {
            var table = table(
                    new String[][]{
                            {"B3", "C3"},
                            {"B31", "C32"},
                            {"H3", "H4"},
                            {"C4:C8", "D4:D8"},
                    },
                    new String[][]{{"表单2!G8", "表单2!K8"}});

            String in = "=B3*(B31+H3)+SUM(C4:C8)+表单2!G8";
            assertEquals("=C3*(C32+H4)+SUM(D4:D8)+表单2!K8",
                    FormulaCoordinateReplacer.replace(in, table));
        }
    }

    @Nested
    class EmptyAndUnmapped {

        @Test
        void emptyTableReturnsSameInstanceContent() {
            String formula = "=A1+B2";
            var empty = FormulaCoordinateReplacer.ReplacementTable.builder().build();
            assertEquals(formula, FormulaCoordinateReplacer.replace(formula, empty));
        }

        @Test
        void onlyMappedRefsAreReplaced() {
            var table = table(new String[][]{{"A1", "Z9"}}, null);
            assertEquals("=Z9+B2+C3", FormulaCoordinateReplacer.replace("=A1+B2+C3", table));
        }

        @Test
        void unmappedSheetRefLeftUnchanged() {
            var table = table(new String[][]{{"G8", "H8"}}, null);
            assertEquals("=表单2!G8", FormulaCoordinateReplacer.replace("=表单2!G8", table));
        }
    }

    @Nested
    class LocalRefs {

        @Test
        void replacesMultipleOccurrences() {
            var table = table(new String[][]{{"A1", "B2"}}, null);
            assertEquals("=B2+B2", FormulaCoordinateReplacer.replace("=A1+A1", table));
        }

        @Test
        void lowercaseColumnMatchesNormalizedKey() {
            var table = table(new String[][]{{"B3", "C3"}}, null);
            assertEquals("=C3", FormulaCoordinateReplacer.replace("=b3", table));
        }

        @Test
        void exactMappingWinsOverNormalized() {
            var table = table(new String[][]{{"$B$3", "X1"}}, null);
            assertEquals("=X1", FormulaCoordinateReplacer.replace("=$B$3", table));
        }

        @Test
        void threeLetterColumn() {
            var table = table(new String[][]{{"XFD1", "XFE1"}}, null);
            assertEquals("=XFE1", FormulaCoordinateReplacer.replace("=XFD1", table));
        }

        @ParameterizedTest
        @CsvSource({
                "=$B$3, =$C$3",
                "=$B3,  =$C3",
                "=B$3,  =C$3",
        })
        void absoluteVariantsPreserveDollarPositions(String input, String expected) {
            var table = table(new String[][]{{"B3", "C3"}}, null);
            assertEquals(expected, FormulaCoordinateReplacer.replace(input, table));
        }

        @Test
        void absoluteRangePreservesDollarsOnBothEnds() {
            var table = table(new String[][]{{"A1:B2", "C3:D4"}}, null);
            assertEquals("=$C$3:$D$4",
                    FormulaCoordinateReplacer.replace("=$A$1:$B$2", table));
        }

        @Test
        void rangeMappingReplacesWholeRegion() {
            var table = table(new String[][]{{"C4:C8", "D4:D8"}}, null);
            assertEquals("=SUM(D4:D8)", FormulaCoordinateReplacer.replace("=SUM(C4:C8)", table));
        }

        @Test
        void refInsideParentheses() {
            var table = table(new String[][]{{"B3", "C3"}}, null);
            assertEquals("=(C3)*2", FormulaCoordinateReplacer.replace("=(B3)*2", table));
        }
    }

    @Nested
    class SheetRefs {

        @Test
        void sheetQuotedNameUsesFullTokenMapping() {
            var table = table(null, new String[][]{{"'My Sheet'!A1", "'My Sheet'!B2"}});
            assertEquals("='My Sheet'!B2+1",
                    FormulaCoordinateReplacer.replace("='My Sheet'!A1+1", table));
        }

        @Test
        void sheetNameWithEscapedApostrophe() {
            var table = table(null, new String[][]{
                    {"'Year''2024'!A1", "'Year''2024'!B1"},
            });
            String in = "='Year''2024'!A1";
            assertEquals("='Year''2024'!B1", FormulaCoordinateReplacer.replace(in, table));
        }

        @Test
        void sheetMappingTakesPrecedenceOverLocalOnSameCell() {
            var table = table(
                    new String[][]{{"G8", "H8"}},
                    new String[][]{{"表单2!G8", "表单2!K8"}});
            assertEquals("=表单2!K8", FormulaCoordinateReplacer.replace("=表单2!G8", table));
        }

        @Test
        void multipleSheetsInOneFormula() {
            var table = table(null, new String[][]{
                    {"S1!A1", "S1!B1"},
                    {"S2!A1", "S2!C1"},
            });
            String in = "=S1!A1+S2!A1";
            assertEquals("=S1!B1+S2!C1", FormulaCoordinateReplacer.replace(in, table));
        }

        @Test
        void sheetRefNotInMapDoesNotApplyLocalToQualifiedPart() {
            var table = table(new String[][]{{"G8", "H8"}}, null);
            assertEquals("=表单2!G8", FormulaCoordinateReplacer.replace("=表单2!G8", table));
        }
    }

    @Nested
    class StringLiterals {

        @Test
        void doesNotReplaceInsideStringLiteral() {
            var table = table(new String[][]{{"B3", "C3"}}, null);
            assertEquals("=\"B3\"+C3", FormulaCoordinateReplacer.replace("=\"B3\"+B3", table));
        }

        @Test
        void preservesEscapedQuotesInString() {
            var table = table(new String[][]{{"A1", "Z9"}}, null);
            assertEquals("=\"say \"\"A1\"\"\"+Z9",
                    FormulaCoordinateReplacer.replace("=\"say \"\"A1\"\"\"+A1", table));
        }

        @Test
        void stringContainingSheetLikeTextUnchanged() {
            var table = table(new String[][]{{"A1", "B2"}}, null);
            assertEquals("=\"表单2!G8\"+B2",
                    FormulaCoordinateReplacer.replace("=\"表单2!G8\"+A1", table));
        }
    }

    @Nested
    class OperatorsAndFunctions {

        @ParameterizedTest
        @CsvSource({
                "=A1+B2, =Z9+B2",
                "=A1-B2, =Z9-B2",
                "=A1*B2, =Z9*B2",
                "=A1/B2, =Z9/B2",
                "=A1^B2, =Z9^B2",
                "=A1&B2, =Z9&B2",
                "=A1=B2, =Z9=B2",
                "=A1<B2, =Z9<B2",
                "=A1>B2, =Z9>B2",
                "=A1>=B2, =Z9>=B2",
                "=A1<=B2, =Z9<=B2",
        })
        void replacesRefAdjacentToOperators(String input, String expected) {
            var table = table(new String[][]{{"A1", "Z9"}}, null);
            assertEquals(expected, FormulaCoordinateReplacer.replace(input, table));
        }

        @Test
        void sumAndIfWithRanges() {
            var table = table(
                    new String[][]{
                            {"A1:A5", "B1:B5"},
                            {"A1", "B1"},
                            {"C1", "D1"},
                    },
                    null);
            String in = "=SUM(A1:A5)+IF(C1>0,A1,A5)";
            assertEquals("=SUM(B1:B5)+IF(D1>0,B1,A5)",
                    FormulaCoordinateReplacer.replace(in, table));
        }

        @Test
        void functionNameWithoutDigitsNotTreatedAsRef() {
            var table = table(new String[][]{{"SUM", "XXX"}}, null);
            assertEquals("=SUM(A1)", FormulaCoordinateReplacer.replace("=SUM(A1)", table));
        }

        @Test
        void whitespaceAroundRefs() {
            var table = table(new String[][]{{"A1", "B2"}}, null);
            assertEquals("= B2 + 1", FormulaCoordinateReplacer.replace("= A1 + 1", table));
        }

        @Test
        void commaSeparatedFunctionArgs() {
            var table = table(new String[][]{{"A1", "X1"}, {"B2", "Y2"}, {"C3", "Z3"}}, null);
            assertEquals("=SUM(X1,Y2,Z3)",
                    FormulaCoordinateReplacer.replace("=SUM(A1,B2,C3)", table));
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void functionEndingWithDigitsParsedAsRefLikeToken() {
            // LOG10 会被解析为列 LOG + 行 10；无映射时 LOG10 原样，括号内 A1 仍替换
            var table = table(new String[][]{{"A1", "B1"}}, null);
            assertEquals("=LOG10(B1)", FormulaCoordinateReplacer.replace("=LOG10(A1)", table));
        }

        @Test
        void onlyEqualsSign() {
            var table = table(new String[][]{{"A1", "B1"}}, null);
            assertEquals("=", FormulaCoordinateReplacer.replace("=", table));
        }

        @Test
        void noRefsOnlyNumbers() {
            var table = table(new String[][]{{"A1", "B1"}}, null);
            assertEquals("=1+2*3", FormulaCoordinateReplacer.replace("=1+2*3", table));
        }

        @Test
        void adjacentRefsOnlyTrailingPartMayParse() {
            // A1 后紧跟 C 无边界不解析；尾部 C2 可解析并替换
            var table = table(new String[][]{{"A1", "B1"}, {"C2", "D2"}}, null);
            assertEquals("=A1D2", FormulaCoordinateReplacer.replace("=A1C2", table));
        }
    }

    @Nested
    class NormalizeRefKey {

        @ParameterizedTest
        @CsvSource({
                "B3, B3",
                "$B$3, B3",
                "b3, B3",
                "C4:C8, C4:C8",
                "$a$1:$b$2, A1:B2",
        })
        void normalizeStripsDollarsAndUppercasesColumns(String input, String expected) {
            assertEquals(expected, FormulaCoordinateReplacer.normalizeRefKey(input));
        }
    }

    @Nested
    class PreserveAbsoluteMarkers {

        @Test
        void singleCellRowAbsolute() {
            assertEquals("C$3",
                    FormulaCoordinateReplacer.preserveAbsoluteMarkers("B$3", "C3"));
        }

        @Test
        void singleCellColumnAbsolute() {
            assertEquals("$C3",
                    FormulaCoordinateReplacer.preserveAbsoluteMarkers("$B3", "C3"));
        }

        @Test
        void rangeBothSides() {
            assertEquals("$C$1:$D$2",
                    FormulaCoordinateReplacer.preserveAbsoluteMarkers("$A$1:$B$2", "C1:D2"));
        }

        @Test
        void noDollarsReturnsTargetAsIs() {
            assertEquals("C3",
                    FormulaCoordinateReplacer.preserveAbsoluteMarkers("B3", "C3"));
        }
    }

    @Nested
    class ResolveReplacementBehavior {

        @Test
        void sheetHitReturnsReplacementEvenWhenLocalHasSameSuffix() {
            var table = table(
                    new String[][]{{"G8", "H8"}},
                    new String[][]{{"表单2!G8", "表单2!K8"}});
            String replaced = FormulaCoordinateReplacer.replace("=表单2!G8", table);
            assertEquals("=表单2!K8", replaced);
        }

        @Test
        void mixedLocalAndSheetInComplexFormula() {
            var table = table(
                    new String[][]{
                            {"A1", "A2"},
                            {"B1", "B2"},
                    },
                    new String[][]{{"Ext!A1", "Ext!A9"}});
            String in = "=A1+Ext!A1+B1";
            assertEquals("=A2+Ext!A9+B2", FormulaCoordinateReplacer.replace(in, table));
        }
    }

    @Nested
    class SubstringCollision {

        @ParameterizedTest
        @CsvSource({
                "B1, C1, =B1+B11, =C1+B11",
                "B1, C1, =B11+B1, =B11+C1",
                "B3, C3, =B3+B31+B311, =C3+B31+B311",
                "B31, C32, =B31+B3, =C32+B3",
                "AA1, BB1, =AA1+AA10, =BB1+AA10",
                "AA10, BB10, =AA10+AA1, =BB10+AA1",
        })
        void distinctRefsDoNotCrossReplace(String from, String to, String input, String expected) {
            var table = table(new String[][]{{from, to}}, null);
            assertReplace(expected, input, table);
        }

        @Test
        void collisionWhenBothMappingsPresent() {
            var table = table(
                    new String[][]{{"B3", "C3"}, {"B31", "C32"}, {"B311", "C311"}},
                    null);
            assertReplace("=C3+C32+C311", "=B3+B31+B311", table);
        }

        @Test
        void manySimilarRefsInOneExpression() {
            var table = table(
                    new String[][]{
                            {"A1", "Z1"},
                            {"A10", "Z10"},
                            {"A100", "Z100"},
                    },
                    null);
            assertReplace("=Z1+Z10+Z100+A1000",
                    "=A1+A10+A100+A1000", table);
        }
    }

    @Nested
    class RealWorldFormulas {

        @Test
        void vlookupWithSheetRange() {
            var table = table(
                    new String[][]{{"A1", "X1"}},
                    new String[][]{{"数据表!A1:C20", "数据表!D1:F20"}});
            String in = "=VLOOKUP(A1,数据表!A1:C20,2,0)";
            assertReplace("=VLOOKUP(X1,数据表!D1:F20,2,0)", in, table);
        }

        @Test
        void nestedIfWithSheetAndLocal() {
            var table = table(
                    new String[][]{{"B3", "C3"}, {"A1", "A9"}},
                    new String[][]{{"表单2!G8", "表单2!K8"}});
            assertReplace("=IF(A9>0,表单2!K8,C3)",
                    "=IF(A1>0,表单2!G8,B3)", table);
        }

        @Test
        void indexMatchStyle() {
            var table = table(
                    new String[][]{
                            {"B2:B100", "C2:C100"},
                            {"E1", "F1"},
                    },
                    null);
            assertReplace("=INDEX(C2:C100,MATCH(F1,C2:C100,0))",
                    "=INDEX(B2:B100,MATCH(E1,B2:B100,0))", table);
        }

        @Test
        void sumproductTwoRanges() {
            var table = table(
                    new String[][]{
                            {"A1:A10", "B1:B10"},
                            {"C1:C10", "D1:D10"},
                    },
                    null);
            assertReplace("=SUMPRODUCT(B1:B10,D1:D10)",
                    "=SUMPRODUCT(A1:A10,C1:C10)", table);
        }

        @Test
        void offsetLikeRefInExpression() {
            var table = table(new String[][]{{"D5", "E6"}, {"F5", "G6"}}, null);
            assertReplace("=E6+G6*2", "=D5+F5*2", table);
        }

        @Test
        void deepNestedFunctions() {
            var table = table(
                    new String[][]{{"A1", "P1"}, {"B2", "Q2"}, {"C3", "R3"}},
                    null);
            assertReplace("=IF(P1>0,MAX(Q2,R3),MIN(Q2,R3))",
                    "=IF(A1>0,MAX(B2,C3),MIN(B2,C3))", table);
        }
    }

    @Nested
    class SheetRefsExtended {

        @Test
        void sheetNameWithDotAndUnderscore() {
            var table = table(null, new String[][]{{"cfg.v1!A1", "cfg.v1!Z9"}});
            assertReplace("=cfg.v1!Z9", "=cfg.v1!A1", table);
        }

        @Test
        void numericSheetName() {
            var table = table(null, new String[][]{{"2024!B2", "2024!C3"}});
            assertReplace("=2024!C3", "=2024!B2", table);
        }

        @Test
        void sheetQualifiedRangeFullToken() {
            var table = table(null, new String[][]{
                    {"报表!C4:C8", "报表!D4:D8"},
            });
            assertReplace("=SUM(报表!D4:D8)", "=SUM(报表!C4:C8)", table);
        }

        @Test
        void quotedSheetWithRangeAndDollars() {
            var table = table(null, new String[][]{
                    {"'Q1 Report'!$A$1:$B$2", "'Q1 Report'!$C$3:$D$4"},
            });
            assertReplace("='Q1 Report'!$C$3:$D$4",
                    "='Q1 Report'!$A$1:$B$2", table);
        }

        @Test
        void chineseSheetWithLocalUnmappedKeepsWhole() {
            var table = table(new String[][]{{"A1", "Z1"}}, null);
            assertReplace("=汇总!A1+Z1", "=汇总!A1+A1", table);
        }

        @Test
        void twoSheetRangesPlusLocal() {
            var table = table(
                    new String[][]{{"Z9", "W9"}},
                    new String[][]{
                            {"东!A1:A3", "东!B1:B3"},
                            {"西!A1:A3", "西!B1:B3"},
                    });
            assertReplace("=SUM(东!B1:B3,西!B1:B3)+W9",
                    "=SUM(东!A1:A3,西!A1:A3)+Z9", table);
        }
    }

    @Nested
    class LocalRefsExtended {

        @Test
        void doubleColumnLetters() {
            var table = table(new String[][]{{"AA100", "AB101"}}, null);
            assertReplace("=AB101", "=AA100", table);
        }

        @Test
        void singleCellMaxWidthColumn() {
            var table = table(new String[][]{{"XFD1048576", "XFE1048576"}}, null);
            assertReplace("=XFE1048576", "=XFD1048576", table);
        }

        @Test
        void rangeWithMixedAbsoluteEnds() {
            var table = table(new String[][]{{"A$1:B$2", "C$3:D$4"}}, null);
            assertReplace("=C$3:D$4", "=A$1:B$2", table);
        }

        @Test
        void rangeViaNormalizedKeyWithDollarsOnInput() {
            var table = table(new String[][]{{"C4:C8", "D4:D8"}}, null);
            assertReplace("=SUM($D$4:$D$8)", "=SUM($C$4:$C$8)", table);
        }

        @Test
        void replacementTargetAlsoHasDollars() {
            var table = table(new String[][]{{"A1", "$B$2"}}, null);
            assertReplace("=$B$2", "=A1", table);
        }

        @ParameterizedTest
        @ValueSource(strings = {"=A1+B2", "=A1-B2", "=A1<>B2", "=(A1)", "=NOT(A1)"})
        void refBeforeOrAfterDelimiters(String formula) {
            var table = table(new String[][]{{"A1", "X1"}}, null);
            String expected = formula.replace("A1", "X1");
            assertReplace(expected, formula, table);
        }
    }

    @Nested
    class StringLiteralsExtended {

        @Test
        void multipleStringSegments() {
            var table = table(new String[][]{{"A1", "Z1"}, {"B2", "Y2"}}, null);
            assertReplace("=\"A1\"&Z1&\"B2\"&Y2",
                    "=\"A1\"&A1&\"B2\"&B2", table);
        }

        @Test
        void stringEndsFormula() {
            var table = table(new String[][]{{"A1", "Z1"}}, null);
            assertReplace("=\"only\"", "=\"only\"", table);
        }

        @Test
        void unclosedQuoteTreatsRemainderAsLiteralAndAppendsClosingQuote() {
            var table = table(new String[][]{{"A1", "Z1"}}, null);
            String in = "=\"open+A1";
            String out = FormulaCoordinateReplacer.replace(in, table);
            // 未闭合时整段视为字符串字面量，末尾补一个 "；字面量内的 A1 不替换
            assertEquals("=\"open+A1\"", out);
            assertEquals(-1, out.indexOf("Z1"));
        }
    }

    @Nested
    class WhitespaceAndLayout {

        @Test
        void newlineAfterRef() {
            var table = table(new String[][]{{"A1", "B1"}}, null);
            assertReplace("=B1\n+C2", "=A1\n+C2", table);
        }

        @Test
        void tabBetweenRefs() {
            var table = table(new String[][]{{"A1", "X1"}, {"B2", "Y2"}}, null);
            assertReplace("=X1\t+Y2", "=A1\t+B2", table);
        }

        @Test
        void crlfAroundFunction() {
            var table = table(new String[][]{{"A1:A2", "B1:B2"}}, null);
            assertReplace("=SUM(\r\nB1:B2\r\n)",
                    "=SUM(\r\nA1:A2\r\n)", table);
        }
    }

    @Nested
    class OperatorsExtended {

        @Test
        void notEqualOperator() {
            var table = table(new String[][]{{"A1", "P1"}, {"B2", "Q2"}}, null);
            assertReplace("=P1<>Q2", "=A1<>B2", table);
        }

        @Test
        void unaryMinusBeforeRef() {
            var table = table(new String[][]{{"A1", "B1"}}, null);
            assertReplace("=-B1", "=-A1", table);
        }

        @Test
        void percentAfterRefBlocksParsing() {
            var table = table(new String[][]{{"A1", "B1"}}, null);
            // % 非引用边界，A1% 整体不当作坐标 token
            assertReplace("=A1%", "=A1%", table);
        }

        @Test
        void chainedComparisonsInIf() {
            var table = table(
                    new String[][]{{"C1", "D1"}, {"C2", "D2"}, {"C3", "D3"}},
                    null);
            assertReplace("=IF(AND(D1>D2,D2>D3),1,0)",
                    "=IF(AND(C1>C2,C2>C3),1,0)", table);
        }
    }

    @Nested
    class EdgeCasesExtended {

        @Test
        void scientificLikeTokenMayParseAsCell() {
            var table = table(new String[][]{{"E1", "F1"}}, null);
            // 1E2 解析为列 E 行 2；无 1E2 映射时原样
            assertReplace("=1E2+F1", "=1E2+E1", table);
        }

        @Test
        void atSignNotSheetPrefixButLocalRefStillReplaced() {
            var table = table(new String[][]{{"A1", "B1"}}, null);
            assertReplace("=@B1+B1", "=@A1+B1", table);
        }

        @Test
        void bracketNotExternalWorkbook() {
            var table = table(new String[][]{{"A1", "B1"}}, null);
            assertReplace("=[Book1]Sheet1!A1", "=[Book1]Sheet1!A1", table);
        }

        @Test
        void emptyFormulaString() {
            var table = table(new String[][]{{"A1", "B1"}}, null);
            assertReplace("", "", table);
        }

        @Test
        void onlyTextNoEquals() {
            var table = table(new String[][]{{"A1", "B1"}}, null);
            assertReplace("hello", "hello", table);
        }

        @Test
        void hashLiteralNotARef() {
            var table = table(new String[][]{{"A1", "B1"}}, null);
            assertReplace("=#N/A+B1", "=#N/A+B1", table);
        }
    }

    @Nested
    class NormalizeRefKeyExtended {

        @ParameterizedTest
        @CsvSource({
                "$B$3, B3",
                "a1:b2, A1:B2",
                "XFD9, XFD9",
                "aa10:ab99, AA10:AB99",
        })
        void moreNormalizationCases(String raw, String expected) {
            assertEquals(expected, FormulaCoordinateReplacer.normalizeRefKey(raw));
        }
    }

    @Nested
    class PreserveAbsoluteMarkersExtended {

        @ParameterizedTest
        @CsvSource({
                "B3, C3, C3",
                "$B3, C3, $C3",
                "B$3, C3, C$3",
                "$B$3, C3, $C$3",
                "$B$3, $C$3, $C$3",
        })
        void parameterizedSingleCell(String src, String tgt, String expected) {
            assertEquals(expected, FormulaCoordinateReplacer.preserveAbsoluteMarkers(src, tgt));
        }

        @Test
        void rangeLeftAbsoluteRightRelative() {
            assertEquals("$C$1:D2",
                    FormulaCoordinateReplacer.preserveAbsoluteMarkers("$A$1:B2", "C1:D2"));
        }
    }

    @Nested
    class ReplacementTableContract {

        @Test
        void builderLastWriteWins() {
            var built = FormulaCoordinateReplacer.ReplacementTable.builder()
                    .local("A1", "B1")
                    .local("A1", "C1")
                    .build();
            assertReplace("=C1", "=A1", built);
        }

        @Test
        void builtTableIsImmutableSnapshot() {
            HashMap<String, String> local = new HashMap<>();
            local.put("A1", "B1");
            var built = new FormulaCoordinateReplacer.ReplacementTable(local, Map.of());
            local.put("A1", "CHANGED");
            assertReplace("=B1", "=A1", built);
        }

        @Test
        void nullFromInBuilderThrows() {
            assertThrows(NullPointerException.class,
                    () -> FormulaCoordinateReplacer.ReplacementTable.builder().local(null, "B1"));
            assertThrows(NullPointerException.class,
                    () -> FormulaCoordinateReplacer.ReplacementTable.builder().local("A1", null));
        }

        @Test
        void replaceRejectsNullFormula() {
            var t = FormulaCoordinateReplacer.ReplacementTable.builder().build();
            assertThrows(NullPointerException.class, () -> FormulaCoordinateReplacer.replace(null, t));
        }
    }

    @Nested
    class BatchReplace {

        @Test
        void sameTableProducesConsistentResults() {
            var table = table(new String[][]{{"A1", "X1"}}, null);
            List<String> inputs = List.of("=A1+1", "=SUM(A1)", "=IF(A1>0,A1,0)");
            List<String> expected = List.of("=X1+1", "=SUM(X1)", "=IF(X1>0,X1,0)");
            for (int i = 0; i < inputs.size(); i++) {
                assertReplace(expected.get(i), inputs.get(i), table);
            }
        }
    }

    @Nested
    class RegressionScenarios {

        @Test
        void originalUserExampleUnchanged() {
            var table = table(
                    new String[][]{
                            {"B3", "C3"},
                            {"B31", "C32"},
                            {"H3", "H4"},
                            {"C4:C8", "D4:D8"},
                    },
                    new String[][]{{"表单2!G8", "表单2!K8"}});
            assertReplace("=C3*(C32+H4)+SUM(D4:D8)+表单2!K8",
                    "=B3*(B31+H3)+SUM(C4:C8)+表单2!G8", table);
        }

        @Test
        void sheetMissWithLocalPresentNoPartialReplace() {
            var table = table(
                    new String[][]{{"G8", "ZZ8"}, {"A1", "A2"}},
                    new String[][]{{"其他表!G8", "其他表!H8"}});
            assertReplace("=表单2!G8+A2", "=表单2!G8+A2", table);
            assertReplace("=其他表!H8+A2", "=其他表!H8+A2", table);
        }
    }
}
