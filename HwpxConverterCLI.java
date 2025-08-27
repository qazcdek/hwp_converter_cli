import kr.dogfoot.hwp2hwpx.Hwp2Hwpx;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.*;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Table;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.t.NormalText;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;

import java.io.File;
import java.util.*;

public class HwpxConverterCLI {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java HwpxConverterCLI <input1.hwp|hwpx> [<input2> ...]");
            System.exit(1);
        }

        Map<String, List<String>> resultByFile = new LinkedHashMap<>();

        for (String path : args) {
            File inputFile = new File(path);
            if (!inputFile.exists()) {
                System.err.println("File not found: " + path);
                System.exit(1);
            }

            try {
                final HWPXFile hwpx;
                if (path.toLowerCase().endsWith(".hwp")) {
                    HWPFile hwp = HWPReader.fromFile(inputFile);
                    hwpx = Hwp2Hwpx.toHWPX(hwp);
                } else {
                    hwpx = HWPXReader.fromFile(inputFile);
                }

                List<String> chunks = new ArrayList<>();
                int page = 0; 

                for (SectionXMLFile sectionFile : hwpx.sectionXMLFileList().items()) {
                    page++; 

                    for (Para para : sectionFile.paras()) {
                        StringBuilder textBuf = new StringBuilder();

                        for (Run run : para.runs()) {
                            for (int i = 0; i < run.countOfRunItem(); i++) {
                                RunItem item = run.getRunItem(i);

                                if (item instanceof T tItem) {
                                    if (tItem.isOnlyText()) {
                                        String text = tItem.onlyText();
                                        if (text != null && !text.isBlank()) textBuf.append(text);
                                    } else {
                                        for (TItem ti : tItem.items()) {
                                            if (ti instanceof NormalText nt) {
                                                String text = nt.text();
                                                if (text != null && !text.isBlank()) textBuf.append(text);
                                            }
                                        }
                                    }

                                } else if (item instanceof Table table) {
                                    String tableText = TextExtractor.extractFrom(
                                            table,
                                            TextExtractMethod.InsertControlTextBetweenParagraphText,
                                            new TextMarks()
                                                    .tableCellSeparatorAnd(" | ")
                                                    .tableRowSeparatorAnd("\n")
                                    );

                                    String tableJson = buildChunkJson(
                                            Collections.singletonList(page), // <-- 수정된 부분
                                            "table",
                                            inferSectionLevel(para),
                                            mostFrequentFontSize(para),
                                            null,
                                            null,
                                            tableText == null ? "" : tableText
                                    );
                                    chunks.add(tableJson);
                                }
                            }
                        }

                        String textContent = textBuf.toString().trim();
                        if (!textContent.isEmpty()) {
                            String textJson = buildChunkJson(
                                    Collections.singletonList(page), // <-- 수정된 부분
                                    "text",
                                    inferSectionLevel(para),
                                    mostFrequentFontSize(para),
                                    null,
                                    null,
                                    textContent
                            );
                            chunks.add(textJson);
                        }
                    }
                }

                resultByFile.put(path, chunks);

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        System.out.println(buildTopLevelJson(resultByFile));
    }

    /* ==================== Helpers ==================== */

    private static String buildChunkJson(List<Integer> pages,
                                         String type,
                                         String sectionLevel,
                                         int fontSize,
                                         double[] bboxOrNull,
                                         Object objectAlwaysNull,
                                         String content) {

        StringBuilder sb = new StringBuilder(256);
        sb.append("{");
        sb.append("\"page\":").append(pages.toString()).append(",");
        sb.append("\"type\":\"").append(escape(type)).append("\",");
        sb.append("\"section_level\":\"").append(escape(sectionLevel)).append("\",");

        sb.append("\"fontsize\":").append(fontSize).append(",");
        sb.append("\"pdf_path\":null,");

        if (bboxOrNull == null || bboxOrNull.length != 4) {
            sb.append("\"bbox\":null,");
        } else {
            sb.append("\"bbox\":[")
              .append(formatNum(bboxOrNull[0])).append(",")
              .append(formatNum(bboxOrNull[1])).append(",")
              .append(formatNum(bboxOrNull[2])).append(",")
              .append(formatNum(bboxOrNull[3])).append("],");
        }

        sb.append("\"object\":null,");
        sb.append("\"content\":\"").append(escape(content)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String buildTopLevelJson(Map<String, List<String>> byFile) {
        StringBuilder sb = new StringBuilder(Math.max(1024, byFile.size() * 256));
        sb.append("{");
        boolean firstFile = true;
        for (Map.Entry<String, List<String>> e : byFile.entrySet()) {
            if (!firstFile) sb.append(",");
            firstFile = false;

            sb.append("\"").append(escape(e.getKey())).append("\":");
            sb.append("[");

            boolean first = true;
            for (String chunk : e.getValue()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(chunk);
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String inferSectionLevel(Para para) {
        return "body";
    }

    private static int mostFrequentFontSize(Para para) {
        return -1;
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder((int) (s.length() * 1.1));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        return out.toString();
    }

    private static String formatNum(double v) {
        if (Math.rint(v) == v) return String.valueOf((long) v);
        return String.valueOf(v);
    }
}