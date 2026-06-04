package com.od.formula;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 解析类 Excel 公式中的单元格结点坐标。
 * <p>
 * 本表结点：{@code B5}、{@code G6}、{@code D2} 等当前工作表引用。<br>
 * 跨表结点：{@code 表单1!B5}、{@code form1!G6}、{@code 'Sheet Name'!A1} 等带工作表前缀的引用。
 * <p>
 * 循环场景请使用 {@link ReusableParser}，可复用内部缓冲与集合并避免重复扫描。
 */
public final class FormulaNodeParser {

    /** 本表结点集合的默认初始容量。 */
    private static final int DEFAULT_LOCAL_NODE_CAPACITY = 16;
    /** 跨表结点集合的默认初始容量。 */
    private static final int DEFAULT_CROSS_SHEET_NODE_CAPACITY = 8;
    /** 公式字符缓冲区的默认长度。 */
    private static final int DEFAULT_CHAR_BUFFER_SIZE = 128;
    /** 单元格地址拼装缓冲区的默认长度。 */
    private static final int DEFAULT_ADDRESS_BUFFER_SIZE = 8;
    /** Excel 列名最大长度（XFD）。 */
    private static final int MAX_COLUMN_NAME_LENGTH = 3;

    private FormulaNodeParser() {
    }

    /**
     * 一次解析返回本表、跨表及全部结点。
     */
    public record FormulaNodes(
            Set<String> localNodes,
            Set<String> crossSheetNodes,
            Set<String> allNodes) {
    }

    /**
     * 一次扫描解析公式，适合循环中按需取三类结点。
     */
    public static FormulaNodes parse(String formulaText) {
        Objects.requireNonNull(formulaText, "formulaText");
        var localNodeSet = new LinkedHashSet<String>(DEFAULT_LOCAL_NODE_CAPACITY);
        var crossSheetNodeSet = new LinkedHashSet<String>(DEFAULT_CROSS_SHEET_NODE_CAPACITY);
        var allNodeSet = new LinkedHashSet<String>(DEFAULT_LOCAL_NODE_CAPACITY);
        scanFormula(formulaText, localNodeSet, crossSheetNodeSet, allNodeSet);
        return new FormulaNodes(
                Collections.unmodifiableSet(localNodeSet),
                Collections.unmodifiableSet(crossSheetNodeSet),
                Collections.unmodifiableSet(allNodeSet));
    }

    /**
     * 返回公式中的本表结点集合。
     */
    public static Set<String> getLocalNodes(String formulaText) {
        return parse(formulaText).localNodes();
    }

    /**
     * 返回公式中的跨表结点集合。
     */
    public static Set<String> getCrossSheetNodes(String formulaText) {
        return parse(formulaText).crossSheetNodes();
    }

    /**
     * 返回公式中的本表与跨表结点合集，按公式中出现顺序去重。
     */
    public static Set<String> getAllNodes(String formulaText) {
        return parse(formulaText).allNodes();
    }

    /**
     * 可复用解析器，适用于循环调用。
     * <p>
     * {@link #parse(String)} 始终返回同一 {@link FormulaNodes} 实例；
     * 下次调用会清空并覆盖内部集合，请在下次 {@code parse} 前消费结果。
     */
    public static final class ReusableParser {
        private final FormulaScanner formulaScanner = new FormulaScanner();
        private final Set<String> localNodeSet = new LinkedHashSet<>(DEFAULT_LOCAL_NODE_CAPACITY);
        private final Set<String> crossSheetNodeSet = new LinkedHashSet<>(DEFAULT_CROSS_SHEET_NODE_CAPACITY);
        private final Set<String> allNodeSet = new LinkedHashSet<>(DEFAULT_LOCAL_NODE_CAPACITY);
        private final FormulaNodes cachedParseResult = new FormulaNodes(
                Collections.unmodifiableSet(localNodeSet),
                Collections.unmodifiableSet(crossSheetNodeSet),
                Collections.unmodifiableSet(allNodeSet));

        public FormulaNodes parse(String formulaText) {
            Objects.requireNonNull(formulaText, "formulaText");
            localNodeSet.clear();
            crossSheetNodeSet.clear();
            allNodeSet.clear();
            formulaScanner.reset(formulaText, localNodeSet, crossSheetNodeSet, allNodeSet);
            formulaScanner.scan();
            return cachedParseResult;
        }
    }

    private static void scanFormula(
            String formulaText,
            Set<String> localNodeSet,
            Set<String> crossSheetNodeSet,
            Set<String> allNodeSet) {
        var formulaScanner = new FormulaScanner();
        formulaScanner.reset(formulaText, localNodeSet, crossSheetNodeSet, allNodeSet);
        formulaScanner.scan();
    }

    /**
     * 公式字符扫描器：线性扫描文本，识别单元格引用并写入目标集合。
     */
    private static final class FormulaScanner {
        private char[] formulaChars = new char[DEFAULT_CHAR_BUFFER_SIZE];
        private int contentLength;
        private int currentIndex;
        private Set<String> localNodeSet;
        private Set<String> crossSheetNodeSet;
        private Set<String> allNodeSet;
        private char[] cellAddressBuffer = new char[DEFAULT_ADDRESS_BUFFER_SIZE];

        void reset(
                String formulaText,
                Set<String> localNodeSet,
                Set<String> crossSheetNodeSet,
                Set<String> allNodeSet) {
            this.localNodeSet = localNodeSet;
            this.crossSheetNodeSet = crossSheetNodeSet;
            this.allNodeSet = allNodeSet;
            this.currentIndex = 0;
            loadNormalizedFormulaChars(formulaText);
        }

        /** 去掉首尾空白与 leading {@code =}，写入可复用字符缓冲。 */
        private void loadNormalizedFormulaChars(String formulaText) {
            int textEnd = formulaText.length();
            int textStart = 0;
            while (textStart < textEnd && isWhitespace(formulaText.charAt(textStart))) {
                textStart++;
            }
            while (textEnd > textStart && isWhitespace(formulaText.charAt(textEnd - 1))) {
                textEnd--;
            }
            if (textStart < textEnd && formulaText.charAt(textStart) == '=') {
                textStart++;
            }

            contentLength = textEnd - textStart;
            if (formulaChars.length < contentLength) {
                formulaChars = new char[Math.max(contentLength, formulaChars.length << 1)];
            }
            formulaText.getChars(textStart, textEnd, formulaChars, 0);
        }

        void scan() {
            scanExpression();
        }

        /** 扫描一段表达式，遇到 {@code )} 时结束（供函数参数嵌套使用）。 */
        private void scanExpression() {
            while (currentIndex < contentLength) {
                skipWhitespace();
                if (isAtEnd()) {
                    return;
                }
                if (isClosingParenthesis()) {
                    return;
                }
                if (consumeCurrentToken()) {
                    continue;
                }
                currentIndex++;
            }
        }

        private boolean isAtEnd() {
            return currentIndex >= contentLength;
        }

        private boolean isClosingParenthesis() {
            return formulaChars[currentIndex] == ')';
        }

        /** 根据当前字符类型分发处理逻辑。 */
        private boolean consumeCurrentToken() {
            char currentChar = formulaChars[currentIndex];
            return switch (currentChar) {
                case '(' -> consumeOpenParenthesis();
                case '\'' -> consumeSingleQuotedSegment();
                case '"' -> consumeDoubleQuotedString();
                case ';', ',' -> consumeArgumentSeparator();
                default -> consumeOtherCharacter(currentChar);
            };
        }

        private boolean consumeOpenParenthesis() {
            currentIndex++;
            scanExpression();
            return true;
        }

        private boolean consumeSingleQuotedSegment() {
            if (tryParseCrossSheetCellReference()) {
                return true;
            }
            skipSingleQuotedString();
            return true;
        }

        private boolean consumeDoubleQuotedString() {
            skipDoubleQuotedString();
            return true;
        }

        private boolean consumeArgumentSeparator() {
            currentIndex++;
            return true;
        }

        private boolean consumeOtherCharacter(char currentChar) {
            if (isOperator(currentChar)) {
                currentIndex++;
                return true;
            }
            if (isDigit(currentChar)) {
                skipNumericLiteral();
                return true;
            }
            if (isIdentifierStart(currentChar)) {
                return consumeIdentifierSegment();
            }
            return false;
        }

        /** 依次尝试跨表引用、本表引用、函数调用；均失败则跳过普通标识符。 */
        private boolean consumeIdentifierSegment() {
            if (tryParseCrossSheetCellReference()
                    || tryParseLocalCellReference()
                    || tryParseFunctionCall()) {
                return true;
            }
            skipPlainIdentifier();
            return true;
        }

        /**
         * 尝试解析跨表引用，如 {@code 表单1!B5} 或 {@code 'Data Sheet'!E2:E10}。
         * 解析失败时回退扫描位置。
         */
        private boolean tryParseCrossSheetCellReference() {
            return attemptParse(() -> {
                String sheetName = readSheetNameBeforeExclamation();
                if (sheetName == null) {
                    return false;
                }
                skipWhitespace();
                if (!parseSingleCellReference(sheetName)) {
                    return false;
                }
                parseRangeSecondCellIfPresent(sheetName);
                return true;
            });
        }

        /** 尝试解析本表引用，如 {@code B5} 或 {@code D2:D9}。 */
        private boolean tryParseLocalCellReference() {
            return attemptParse(() -> {
                if (!parseSingleCellReference(null)) {
                    return false;
                }
                parseRangeSecondCellIfPresent(null);
                return true;
            });
        }

        /** 尝试解析函数调用，如 {@code SUM(...)}、{@code IF(...)}。 */
        private boolean tryParseFunctionCall() {
            return attemptParse(() -> {
                int functionNameStart = currentIndex;
                skipFunctionName();
                if (currentIndex == functionNameStart) {
                    return false;
                }
                skipWhitespace();
                if (isAtEnd() || formulaChars[currentIndex] != '(') {
                    return false;
                }
                currentIndex++;
                scanFunctionArguments();
                return true;
            });
        }

        /** 解析失败时回退到尝试前的位置。 */
        private boolean attemptParse(ParseAttempt parseAttempt) {
            int rollbackIndex = currentIndex;
            if (parseAttempt.run()) {
                return true;
            }
            currentIndex = rollbackIndex;
            return false;
        }

        @FunctionalInterface
        private interface ParseAttempt {
            boolean run();
        }

        /** 读取 {@code sheetName!} 中的 sheetName；若当前位置不是跨表前缀则返回 {@code null}。 */
        private String readSheetNameBeforeExclamation() {
            if (currentIndex < contentLength && formulaChars[currentIndex] == '\'') {
                return readQuotedSheetName();
            }
            return readUnquotedSheetName();
        }

        private String readQuotedSheetName() {
            int quotedStart = currentIndex;
            skipSingleQuotedString();
            skipWhitespace();
            if (isAtEnd() || formulaChars[currentIndex] != '!') {
                return null;
            }
            String quotedSheetName = new String(formulaChars, quotedStart, currentIndex - quotedStart);
            currentIndex++;
            return unescapeQuotedSheetName(quotedSheetName);
        }

        private String readUnquotedSheetName() {
            if (!isIdentifierStart(formulaChars[currentIndex])) {
                return null;
            }
            int sheetNameStart = currentIndex;
            skipSheetNameCharacters();
            if (currentIndex == sheetNameStart) {
                return null;
            }
            skipWhitespace();
            if (isAtEnd() || formulaChars[currentIndex] != '!') {
                currentIndex = sheetNameStart;
                return null;
            }
            currentIndex++;
            return new String(formulaChars, sheetNameStart, currentIndex - sheetNameStart - 1);
        }

        /**
         * 解析单个单元格地址（列名 + 行号）。
         *
         * @param sheetName 工作表名；{@code null} 表示本表引用
         */
        private boolean parseSingleCellReference(String sheetName) {
            return attemptParse(() -> {
                skipWhitespace();
                skipAbsoluteMarker();

                int columnStart = currentIndex;
                skipColumnLetters();
                int columnLength = currentIndex - columnStart;
                if (!isValidColumnLength(columnLength)) {
                    return false;
                }

                skipAbsoluteMarker();

                int rowNumberStart = currentIndex;
                skipRowDigits();
                if (rowNumberStart == currentIndex) {
                    return false;
                }

                String cellAddress = buildCellAddress(columnStart, columnLength, rowNumberStart, currentIndex);
                recordCellReference(sheetName, cellAddress);
                return true;
            });
        }

        private boolean isValidColumnLength(int columnLength) {
            return columnLength > 0 && columnLength <= MAX_COLUMN_NAME_LENGTH;
        }

        /** 若存在 {@code :} 区域后缀，继续解析第二个单元格。 */
        private void parseRangeSecondCellIfPresent(String sheetName) {
            skipWhitespace();
            if (isAtEnd() || formulaChars[currentIndex] != ':') {
                return;
            }
            currentIndex++;
            skipWhitespace();
            parseSingleCellReference(sheetName);
        }

        /** 将列名（大写）与行号拼成 {@code B5} 形式的地址。 */
        private String buildCellAddress(int columnStart, int columnLength, int rowNumberStart, int rowNumberEnd) {
            int rowNumberLength = rowNumberEnd - rowNumberStart;
            int addressLength = columnLength + rowNumberLength;
            ensureAddressBufferCapacity(addressLength);

            copyUppercaseColumn(columnStart, columnLength);
            System.arraycopy(formulaChars, rowNumberStart, cellAddressBuffer, columnLength, rowNumberLength);
            return new String(cellAddressBuffer, 0, addressLength);
        }

        private void ensureAddressBufferCapacity(int addressLength) {
            if (cellAddressBuffer.length < addressLength) {
                cellAddressBuffer = new char[Math.max(addressLength, cellAddressBuffer.length << 1)];
            }
        }

        private void copyUppercaseColumn(int columnStart, int columnLength) {
            for (int i = 0; i < columnLength; i++) {
                char columnChar = formulaChars[columnStart + i];
                cellAddressBuffer[i] = toUppercaseColumnChar(columnChar);
            }
        }

        private static char toUppercaseColumnChar(char columnChar) {
            return (columnChar >= 'a' && columnChar <= 'z') ? (char) (columnChar - 32) : columnChar;
        }

        private void recordCellReference(String sheetName, String cellAddress) {
            if (sheetName == null) {
                recordLocalNode(cellAddress);
            } else {
                recordCrossSheetNode(sheetName, cellAddress);
            }
        }

        private void scanFunctionArguments() {
            while (currentIndex < contentLength) {
                skipWhitespace();
                if (isAtEnd()) {
                    return;
                }
                if (formulaChars[currentIndex] == ')') {
                    currentIndex++;
                    return;
                }
                scanExpression();
            }
        }

        private void recordLocalNode(String cellAddress) {
            localNodeSet.add(cellAddress);
            allNodeSet.add(cellAddress);
        }

        private void recordCrossSheetNode(String sheetName, String cellAddress) {
            String crossSheetReference = sheetName + '!' + cellAddress;
            crossSheetNodeSet.add(crossSheetReference);
            allNodeSet.add(crossSheetReference);
        }

        private void skipWhitespace() {
            while (currentIndex < contentLength && isWhitespace(formulaChars[currentIndex])) {
                currentIndex++;
            }
        }

        private void skipAbsoluteMarker() {
            if (currentIndex < contentLength && formulaChars[currentIndex] == '$') {
                currentIndex++;
            }
        }

        private void skipColumnLetters() {
            while (currentIndex < contentLength && isColumnLetter(formulaChars[currentIndex])) {
                currentIndex++;
            }
        }

        private void skipRowDigits() {
            while (currentIndex < contentLength && isDigit(formulaChars[currentIndex])) {
                currentIndex++;
            }
        }

        private void skipFunctionName() {
            while (currentIndex < contentLength && isFunctionNameChar(formulaChars[currentIndex])) {
                currentIndex++;
            }
        }

        private void skipSheetNameCharacters() {
            while (currentIndex < contentLength && isSheetNameChar(formulaChars[currentIndex])) {
                currentIndex++;
            }
        }

        private void skipNumericLiteral() {
            while (currentIndex < contentLength
                    && (isDigit(formulaChars[currentIndex]) || formulaChars[currentIndex] == '.')) {
                currentIndex++;
            }
        }

        private void skipPlainIdentifier() {
            while (currentIndex < contentLength && isIdentifierChar(formulaChars[currentIndex])) {
                currentIndex++;
            }
        }

        private void skipSingleQuotedString() {
            currentIndex++;
            while (currentIndex < contentLength && formulaChars[currentIndex] != '\'') {
                currentIndex++;
            }
            if (currentIndex < contentLength) {
                currentIndex++;
            }
        }

        private void skipDoubleQuotedString() {
            currentIndex++;
            while (currentIndex < contentLength) {
                if (isEscapedDoubleQuote()) {
                    currentIndex += 2;
                    continue;
                }
                if (formulaChars[currentIndex] == '"') {
                    currentIndex++;
                    return;
                }
                currentIndex++;
            }
        }

        private boolean isEscapedDoubleQuote() {
            return formulaChars[currentIndex] == '"'
                    && currentIndex + 1 < contentLength
                    && formulaChars[currentIndex + 1] == '"';
        }

        private static String unescapeQuotedSheetName(String quotedSheetName) {
            if (quotedSheetName.length() >= 2
                    && quotedSheetName.charAt(0) == '\''
                    && quotedSheetName.charAt(quotedSheetName.length() - 1) == '\'') {
                return quotedSheetName.substring(1, quotedSheetName.length() - 1).replace("''", "'");
            }
            return quotedSheetName;
        }

        private static boolean isIdentifierStart(char character) {
            return isColumnLetter(character) || isDigit(character) || character == '_' || character > 127;
        }

        private static boolean isIdentifierChar(char character) {
            return isColumnLetter(character) || isDigit(character) || character == '_'
                    || character == '.' || character > 127;
        }

        private static boolean isSheetNameChar(char character) {
            return isColumnLetter(character) || isDigit(character) || character == '_'
                    || character == ' ' || character == '.' || character > 127;
        }

        private static boolean isFunctionNameChar(char character) {
            return isColumnLetter(character) || isDigit(character) || character == '_'
                    || character == '.' || character > 127;
        }

        private static boolean isColumnLetter(char character) {
            return (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
        }

        private static boolean isDigit(char character) {
            return character >= '0' && character <= '9';
        }

        private static boolean isWhitespace(char character) {
            return character == ' ' || character == '\t' || character == '\n' || character == '\r';
        }

        private static boolean isOperator(char character) {
            return switch (character) {
                case '+', '-', '*', '/', '^', '&', '=', '<', '>', '%' -> true;
                default -> false;
            };
        }
    }

    public static void main(String[] args) {
        var sampleFormulas = List.of(
                "=B5+G6+SUM(D2:D9)",
                "=表单1!B5+form1!G6+SUM(D2:D9)",
                "=IF(A1>0,表单1!C3,SUM('Data Sheet'!E2:E10))",
                "=VLOOKUP(B2,form1!A1:D99,3,FALSE)+$D$2");

        var reusableParser = new ReusableParser();
        for (var formulaText : sampleFormulas) {
            var parseResult = reusableParser.parse(formulaText);
            System.out.println("formula: " + formulaText);
            System.out.println("  local: " + parseResult.localNodes());
            System.out.println("  cross: " + parseResult.crossSheetNodes());
            System.out.println("  all:   " + parseResult.allNodes());
        }
    }
}
