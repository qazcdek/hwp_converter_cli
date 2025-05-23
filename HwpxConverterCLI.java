import kr.dogfoot.hwp2hwpx.Hwp2Hwpx;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor;
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.t.NormalText;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.*;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Table;

import java.io.File;

public class HwpxConverterCLI {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java HwpxConverterCLI <input.hwp|hwpx>");
            System.exit(1);
        }

        File inputFile = new File(args[0]);
        if (!inputFile.exists()) {
            System.err.println("File not found: " + args[0]);
            System.exit(1);
        }

        try {
            final HWPXFile hwpx;
            if (args[0].toLowerCase().endsWith(".hwp")) {
                HWPFile hwp = HWPReader.fromFile(inputFile);
                hwpx = Hwp2Hwpx.toHWPX(hwp);
            } else {
                hwpx = HWPXReader.fromFile(inputFile);
            }

            StringBuilder sb = new StringBuilder();

            for (SectionXMLFile sectionFile : hwpx.sectionXMLFileList().items()) {
                for (Para para : sectionFile.paras()) {
                    for (Run run : para.runs()) {
                        for (int i = 0; i < run.countOfRunItem(); i++) {
                            RunItem item = run.getRunItem(i);
                            if (item instanceof T tItem) {
                                // 먼저 onlyText
                                if (tItem.isOnlyText()) {
                                    String text = tItem.onlyText();
                                    if (text != null && !text.isBlank()) {
                                        sb.append(text);
                                    }
                                } else {
                                    for (TItem ti : tItem.items()) {
                                        if (ti instanceof NormalText nt) {
                                            String text = nt.text();
                                            if (text != null && !text.isBlank()) {
                                                sb.append(text);
                                            }
                                        }
                                    }
                                }
                            } else if (item instanceof Table table) {
                                String tableText = TextExtractor.extractFrom(
                                    table,  // 직접 Table로 안전하게 접근
                                    TextExtractMethod.InsertControlTextBetweenParagraphText,
                                    new TextMarks()
                                        .tableCellSeparatorAnd(" | ")
                                        .tableRowSeparatorAnd("\n")
                                );
                                sb.append(tableText);
                            }
                        }
                        sb.append("\n");
                    }
                    sb.append("\n");
                }
            }

            System.out.println(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}