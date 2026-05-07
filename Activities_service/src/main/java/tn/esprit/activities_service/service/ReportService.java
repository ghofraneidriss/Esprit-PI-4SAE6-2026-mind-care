package tn.esprit.activities_service.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.activities_service.entity.GameResult;
import tn.esprit.activities_service.repository.GameResultRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Patient PDF reports: A4, MindCare CVP branding (same logo as web sidebar/navbar), English copy.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    /** Cap rows in PDF so typical reports stay on one A4 page */
    private static final int PDF_ACTIVITY_HISTORY_MAX_ROWS = 12;

    private final PerformanceAnalysisService performanceService;
    private final GameResultRepository gameResultRepository;

    /** Navy — aligned with officiel dashboard / incident PDF accents */
    private static final DeviceRgb PRIMARY = new DeviceRgb(30, 58, 95);
    private static final DeviceRgb TEXT_MAIN = new DeviceRgb(30, 41, 59);
    private static final DeviceRgb TEXT_MUTED = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb BORDER_LIGHT = new DeviceRgb(226, 232, 240);
    private static final DeviceRgb HEADER_BG = new DeviceRgb(241, 245, 249);
    private static final DeviceRgb ROW_ALT = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb GREEN = new DeviceRgb(34, 197, 94);
    private static final DeviceRgb ORANGE = new DeviceRgb(249, 115, 22);
    private static final DeviceRgb RED = new DeviceRgb(239, 68, 68);
    private static final DeviceRgb YELLOW = new DeviceRgb(234, 179, 8);
    private static final DeviceRgb BLUE_SOFT = new DeviceRgb(59, 130, 246);

    private static final Map<String, String> THEME_EN = new LinkedHashMap<>();

    static {
        THEME_EN.put("MEMORY", "Memory");
        THEME_EN.put("CONCENTRATION", "Concentration");
        THEME_EN.put("LOGIC", "Logic");
        THEME_EN.put("LANGUAGE", "Language");
        THEME_EN.put("ORIENTATION", "Orientation");
        THEME_EN.put("VISUAL", "Visual recognition");
        THEME_EN.put("CALCULATION", "Calculation");
        THEME_EN.put("ATTENTION", "Attention");
    }

    public byte[] generatePatientReport(Long patientId) throws Exception {
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(pdfOutputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(32, 36, 44, 36);

        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontItalic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterEventHandler(fontItalic, TEXT_MUTED));

        PerformanceAnalysisService.PatientPerformance perf = performanceService.analyzePatient(patientId);
        List<GameResult> history = gameResultRepository.findByPatientIdOrderByCompletedAtDesc(patientId);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH);
        String today = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH).format(new Date());
        byte[] logoBytes = loadLogoBytes();

        // ==================== HEADER (logo + MindCare CVP) ====================
        Table headerOuter = new Table(UnitValue.createPercentArray(new float[]{62, 38}))
                .useAllAvailableWidth();

        Cell brandCell = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        Table brandInner = new Table(UnitValue.createPercentArray(new float[]{22, 78}))
                .useAllAvailableWidth();
        Cell logoCell = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (logoBytes != null && logoBytes.length > 0) {
            Image logo = new Image(ImageDataFactory.create(logoBytes));
            logo.setWidth(32);
            logo.setHeight(32);
            logoCell.add(logo);
        } else {
            logoCell.add(new Paragraph("").setMinHeight(32));
        }
        brandInner.addCell(logoCell);
        Cell titleCell = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph("MindCare").setFont(fontBold).setFontSize(16).setFontColor(PRIMARY))
                .add(new Paragraph("CVP · Cognitive Vitality Program").setFont(fontRegular).setFontSize(8).setFontColor(TEXT_MUTED))
                .add(new Paragraph("Patient cognitive performance report").setFont(fontRegular).setFontSize(10).setFontColor(TEXT_MAIN));
        brandInner.addCell(titleCell);
        brandCell.add(brandInner);
        headerOuter.addCell(brandCell);

        Cell metaCell = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .add(new Paragraph("Generated").setFont(fontBold).setFontSize(9).setFontColor(TEXT_MUTED))
                .add(new Paragraph(today).setFont(fontRegular).setFontSize(10).setFontColor(TEXT_MAIN))
                .add(new Paragraph("Ref. MC-" + patientId + "-" + (System.currentTimeMillis() % 100000))
                        .setFont(fontRegular).setFontSize(8).setFontColor(TEXT_MUTED).setMarginTop(4));
        headerOuter.addCell(metaCell);
        doc.add(headerOuter);

        doc.add(new Paragraph().setBorderBottom(new SolidBorder(PRIMARY, 1f)).setMarginTop(4).setMarginBottom(8));

        String patientName = perf.patientName != null && !perf.patientName.isEmpty()
                ? perf.patientName : "Patient #" + patientId;

        // ==================== CLINICAL SUMMARY ====================
        doc.add(new Paragraph("Clinical interpretation")
                .setFont(fontBold).setFontSize(11).setFontColor(PRIMARY).setMarginBottom(4));
        for (String line : buildClinicalSummaryLines(perf, patientName)) {
            doc.add(new Paragraph(line)
                    .setFont(fontRegular).setFontSize(9).setFontColor(TEXT_MAIN)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(3));
        }
        doc.add(new Paragraph(
                "This automated summary supports clinical follow-up and is not a stand-alone diagnosis. "
                        + "Results should be integrated with history, examination, and imaging as appropriate.")
                .setFont(fontItalic).setFontSize(8).setFontColor(TEXT_MUTED)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginBottom(8));

        // ==================== PATIENT INFO ====================
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginBottom(8);

        infoTable.addCell(createInfoCell("Patient name", patientName, fontBold, fontRegular));
        infoTable.addCell(createInfoCell("Patient ID", String.valueOf(patientId), fontBold, fontRegular));
        infoTable.addCell(createInfoCell("Sessions completed", String.valueOf(perf.totalQuizzes), fontBold, fontRegular));
        infoTable.addCell(createInfoCell("Composite score",
                String.format(Locale.ENGLISH, "%.0f%% — %s", perf.globalScore, classifyLevelEn(perf.globalScore)),
                fontBold, fontRegular));
        doc.add(infoTable);

        // ==================== THEME PERFORMANCE ====================
        doc.add(new Paragraph("Performance by cognitive domain")
                .setFont(fontBold).setFontSize(11).setFontColor(PRIMARY).setMarginBottom(4));

        if (perf.themeScores != null && !perf.themeScores.isEmpty()) {
            Table themeTable = new Table(UnitValue.createPercentArray(new float[]{5, 24, 12, 12, 14, 14, 19 }))
                    .useAllAvailableWidth()
                    .setMarginBottom(8);

            String[] themeHeaders = {"", "Domain", "Score", "Sessions", "Avg. time", "Trend", "Level"};
            for (String h : themeHeaders) {
                themeTable.addHeaderCell(new Cell()
                        .setBackgroundColor(HEADER_BG)
                        .setBorder(new SolidBorder(BORDER_LIGHT, 0.5f))
                        .setPadding(4)
                        .add(new Paragraph(h).setFont(fontBold).setFontSize(8).setFontColor(TEXT_MAIN)));
            }

            for (PerformanceAnalysisService.ThemeScore ts : perf.themeScores) {
                DeviceRgb scoreColor = getScoreColor(ts.avgScore);
                DeviceRgb levelColor = getLevelColor(ts.level);
                String domainLabel = themeLabelEn(ts.theme);

                themeTable.addCell(themeCell(ts.icon, fontRegular, 9));
                themeTable.addCell(themeCell(domainLabel, fontRegular, 8));
                themeTable.addCell(themeCellColored(String.format(Locale.ENGLISH, "%.0f%%", ts.avgScore), fontBold, 9, scoreColor));
                themeTable.addCell(themeCell(String.valueOf(ts.quizCount), fontRegular, 8));
                themeTable.addCell(themeCell(String.format(Locale.ENGLISH, "%.1f s/item", ts.avgResponseTime), fontRegular, 8));
                themeTable.addCell(themeCell(getTrendTextEn(ts.trend), fontRegular, 8));
                themeTable.addCell(themeCellColored(getLevelTextEn(ts.level), fontBold, 8, levelColor));
            }
            doc.add(themeTable);
        } else {
            doc.add(new Paragraph("No domain-level performance data available.")
                    .setFont(fontItalic).setFontSize(9).setFontColor(TEXT_MUTED).setMarginBottom(8));
        }

        // ==================== HISTORY ====================
        doc.add(new Paragraph("Activity history (most recent first)")
                .setFont(fontBold).setFontSize(11).setFontColor(PRIMARY).setMarginBottom(4));

        if (!history.isEmpty()) {
            Table histTable = new Table(UnitValue.createPercentArray(new float[]{24, 11, 11, 12, 12, 14, 16 }))
                    .useAllAvailableWidth()
                    .setMarginBottom(6);

            String[] histHeaders = {"Activity", "Type", "Score", "Correct", "Time", "Risk", "Date"};
            for (String h : histHeaders) {
                histTable.addHeaderCell(new Cell()
                        .setBackgroundColor(HEADER_BG)
                        .setBorder(new SolidBorder(BORDER_LIGHT, 0.5f))
                        .setPadding(3)
                        .add(new Paragraph(h).setFont(fontBold).setFontSize(7).setFontColor(TEXT_MAIN)));
            }

            List<GameResult> displayResults = history.subList(0, Math.min(PDF_ACTIVITY_HISTORY_MAX_ROWS, history.size()));
            boolean alt = false;

            for (GameResult gr : displayResults) {
                DeviceRgb rowBg = alt ? ROW_ALT : new DeviceRgb(255, 255, 255);
                alt = !alt;

                String title = gr.getActivityTitle() != null ? gr.getActivityTitle() : "Activity #" + gr.getActivityId();
                if (title.length() > 36) title = title.substring(0, 33) + "...";

                String type = "QUIZ".equalsIgnoreCase(gr.getActivityType()) ? "Quiz" : "Photo";
                String score = gr.getTotalQuestions() != null && gr.getTotalQuestions() > 0
                        ? String.format(Locale.ENGLISH, "%.0f%%",
                        (double) gr.getCorrectAnswers() / gr.getTotalQuestions() * 100)
                        : "—";
                String correct = gr.getCorrectAnswers() + "/" + (gr.getTotalQuestions() != null ? gr.getTotalQuestions() : "—");
                String time = gr.getTimeSpentSeconds() != null ? gr.getTimeSpentSeconds() + " s" : "—";
                String risk = gr.getRiskLevel() != null ? riskLabelEn(gr.getRiskLevel()) : "—";
                String date = gr.getCompletedAt() != null ? sdf.format(gr.getCompletedAt()) : "—";

                histTable.addCell(histCell(title, fontRegular, 7, rowBg));
                histTable.addCell(histCell(type, fontRegular, 7, rowBg));
                histTable.addCell(histCell(score, fontBold, 7, rowBg));
                histTable.addCell(histCell(correct, fontRegular, 7, rowBg));
                histTable.addCell(histCell(time, fontRegular, 7, rowBg));
                histTable.addCell(histCellColored(risk, fontBold, rowBg, getRiskColor(gr.getRiskLevel())));
                histTable.addCell(histCell(date, fontRegular, 6, rowBg));
            }
            doc.add(histTable);

            if (history.size() > PDF_ACTIVITY_HISTORY_MAX_ROWS) {
                doc.add(new Paragraph("… and " + (history.size() - PDF_ACTIVITY_HISTORY_MAX_ROWS) + " additional record(s) not shown.")
                        .setFont(fontItalic).setFontSize(7).setFontColor(TEXT_MUTED).setMarginBottom(4));
            }
        } else {
            doc.add(new Paragraph("No activity results on file.")
                    .setFont(fontItalic).setFontSize(9).setFontColor(TEXT_MUTED).setMarginBottom(8));
        }

        // ==================== RECOMMENDATIONS ====================
        doc.add(new Paragraph("Personalized recommendations")
                .setFont(fontBold).setFontSize(11).setFontColor(PRIMARY).setMarginBottom(4));

        if (perf.recommendations != null && !perf.recommendations.isEmpty()) {
            for (PerformanceAnalysisService.Recommendation rec : perf.recommendations) {
                DeviceRgb priorityColor = getPriorityColor(rec.priority);

                Table recTable = new Table(UnitValue.createPercentArray(new float[]{2, 98 }))
                        .useAllAvailableWidth()
                        .setMarginBottom(6);

                Cell colorBar = new Cell()
                        .setBackgroundColor(priorityColor)
                        .setBorder(Border.NO_BORDER)
                        .setWidth(3);
                recTable.addCell(colorBar);

                Cell contentCell = new Cell().setBorder(new SolidBorder(BORDER_LIGHT, 0.5f))
                        .setBackgroundColor(ColorConstants.WHITE)
                        .setPadding(6);

                String themeEn = rec.theme != null && !rec.theme.isEmpty()
                        ? themeLabelEn(rec.theme)
                        : (rec.themeLabel != null ? rec.themeLabel : "");

                Paragraph header = new Paragraph()
                        .add(new Text(getPriorityLabelEn(rec.priority)).setFont(fontBold).setFontSize(9).setFontColor(priorityColor))
                        .add(new Text("  "))
                        .add(new Text(themeEn).setFont(fontRegular).setFontSize(8).setFontColor(TEXT_MUTED));
                contentCell.add(header);

                contentCell.add(new Paragraph(buildEnglishRecommendationMessage(rec))
                        .setFont(fontRegular).setFontSize(8).setFontColor(TEXT_MAIN).setMarginTop(2));

                if (rec.suggestedQuizTitles != null && !rec.suggestedQuizTitles.isEmpty()) {
                    Paragraph quizList = new Paragraph("Suggested activities: ")
                            .setFont(fontBold).setFontSize(8).setFontColor(TEXT_MUTED).setMarginTop(3);
                    quizList.add(new Text(String.join(", ", rec.suggestedQuizTitles))
                            .setFont(fontRegular).setFontColor(PRIMARY));
                    contentCell.add(quizList);
                }

                recTable.addCell(contentCell);
                doc.add(recTable);
            }
        } else {
            doc.add(new Paragraph("No automated recommendations at this time.")
                    .setFont(fontItalic).setFontSize(9).setFontColor(TEXT_MUTED).setMarginBottom(8));
        }

        doc.add(new Paragraph().setBorderTop(new SolidBorder(BORDER_LIGHT, 0.5f)).setMarginTop(6));
        doc.add(new Paragraph("Generated by MindCare CVP — cognitive monitoring platform for Alzheimer care pathways.")
                .setFont(fontItalic).setFontSize(7).setFontColor(TEXT_MUTED).setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
        doc.add(new Paragraph("© " + Calendar.getInstance().get(Calendar.YEAR) + " MindCare. Confidential — clinical use.")
                .setFont(fontItalic).setFontSize(6).setFontColor(TEXT_MUTED).setTextAlignment(TextAlignment.CENTER));

        doc.close();
        return pdfOutputStream.toByteArray();
    }

    private byte[] loadLogoBytes() {
        try (InputStream is = getClass().getResourceAsStream("/branding/alzcare-logo-3d.png")) {
            if (is == null) {
                return null;
            }
            return is.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private List<String> buildClinicalSummaryLines(PerformanceAnalysisService.PatientPerformance perf, String patientName) {
        List<String> lines = new ArrayList<>();
        if (perf.totalQuizzes <= 0) {
            lines.add("No completed cognitive sessions are available for " + patientName
                    + ". Consider assigning quizzes or photo activities, then re-run this report.");
            return lines;
        }

        lines.add(String.format(Locale.ENGLISH,
                "Composite performance is %.0f%% (%s), based on %d completed session(s) on the MindCare CVP platform.",
                perf.globalScore, classifyLevelEn(perf.globalScore), perf.totalQuizzes));

        if (perf.themeScores != null && !perf.themeScores.isEmpty()) {
            PerformanceAnalysisService.ThemeScore lowest = perf.themeScores.get(0);
            PerformanceAnalysisService.ThemeScore highest = perf.themeScores.get(perf.themeScores.size() - 1);
            lines.add(String.format(Locale.ENGLISH,
                    "Relative strengths and gaps: strongest domain is %s (%.0f%%); the lowest domain is %s (%.0f%%). "
                            + "Prioritize follow-up and exercises that target weaker domains while maintaining strengths.",
                    themeLabelEn(highest.theme), highest.avgScore,
                    themeLabelEn(lowest.theme), lowest.avgScore));

            long improving = perf.themeScores.stream().filter(ts -> "UP".equals(ts.trend)).count();
            long declining = perf.themeScores.stream().filter(ts -> "DOWN".equals(ts.trend)).count();
            if (improving > 0 || declining > 0) {
                lines.add(String.format(Locale.ENGLISH,
                        "Trend signals: %d domain(s) show recent improvement; %d show decline vs earlier attempts. "
                                + "Use trends to adjust intensity and frequency of training, not as sole outcome measures.",
                        improving, declining));
            }
        }

        return lines;
    }

    private String buildEnglishRecommendationMessage(PerformanceAnalysisService.Recommendation rec) {
        if (rec == null) {
            return "";
        }
        if ("MAINTAIN".equals(rec.type)
                || ("LOW".equals(rec.priority) && isGeneralThemeLabel(rec.themeLabel))) {
            return "Continue the current activity plan to preserve strong cognitive performance across domains.";
        }
        String theme = rec.theme != null && !rec.theme.isEmpty()
                ? themeLabelEn(rec.theme)
                : (rec.themeLabel != null ? rec.themeLabel : "This domain");
        double sc = rec.currentScore;

        if ("CRITICAL".equals(rec.priority) || sc < 20) {
            return String.format(Locale.ENGLISH,
                    "%s performance is critically low (%.0f%%). Increase structured practice in this area and consider closer clinical review.",
                    theme, sc);
        }
        if ("HIGH".equals(rec.priority)) {
            return String.format(Locale.ENGLISH,
                    "%s needs improvement (%.0f%%). Add repeated quizzes and spaced practice in this domain.",
                    theme, sc);
        }
        return String.format(Locale.ENGLISH,
                "%s is in the mid range (%.0f%%). Additional short sessions should help consolidate skills.",
                theme, sc);
    }

    private boolean isGeneralThemeLabel(String themeLabel) {
        if (themeLabel == null) {
            return false;
        }
        String t = themeLabel.trim();
        return "Général".equalsIgnoreCase(t) || "General".equalsIgnoreCase(t);
    }

    private Cell createInfoCell(String label, String value, PdfFont fontBold, PdfFont fontRegular) {
        return new Cell().setBorder(new SolidBorder(BORDER_LIGHT, 0.5f))
                .setBackgroundColor(ColorConstants.WHITE)
                .setPadding(5)
                .add(new Paragraph(label).setFont(fontBold).setFontSize(8).setFontColor(TEXT_MUTED))
                .add(new Paragraph(value).setFont(fontRegular).setFontSize(10).setFontColor(TEXT_MAIN));
    }

    private Cell themeCell(String text, PdfFont font, float size) {
        return new Cell().setBorder(new SolidBorder(BORDER_LIGHT, 0.5f))
                .setBackgroundColor(ColorConstants.WHITE)
                .setPadding(3)
                .add(new Paragraph(text).setFont(font).setFontSize(size).setFontColor(TEXT_MAIN));
    }

    private Cell themeCellColored(String text, PdfFont font, float size, DeviceRgb color) {
        return new Cell().setBorder(new SolidBorder(BORDER_LIGHT, 0.5f))
                .setBackgroundColor(ColorConstants.WHITE)
                .setPadding(3)
                .add(new Paragraph(text).setFont(font).setFontSize(size).setFontColor(color));
    }

    private Cell histCell(String text, PdfFont font, float size, Color bg) {
        return new Cell().setBorder(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setBackgroundColor(bg).setPadding(3)
                .add(new Paragraph(text).setFont(font).setFontSize(size).setFontColor(TEXT_MAIN));
    }

    private Cell histCellColored(String text, PdfFont font, Color bg, DeviceRgb textColor) {
        return new Cell().setBorder(new SolidBorder(BORDER_LIGHT, 0.3f))
                .setBackgroundColor(bg).setPadding(3)
                .add(new Paragraph(text).setFont(font).setFontSize(7).setFontColor(textColor));
    }

    private String classifyLevelEn(double score) {
        if (score >= 80) return "Excellent";
        if (score >= 60) return "Good";
        if (score >= 40) return "Fair";
        if (score >= 20) return "Low";
        return "Critical";
    }

    private String themeLabelEn(String themeKey) {
        if (themeKey == null) return "";
        return THEME_EN.getOrDefault(themeKey.toUpperCase(Locale.ROOT), themeKey);
    }

    private String getTrendTextEn(String trend) {
        if (trend == null) return "—";
        return switch (trend) {
            case "UP" -> "Improving";
            case "DOWN" -> "Declining";
            case "STABLE" -> "Stable";
            case "INSUFFICIENT" -> "N/A";
            default -> trend;
        };
    }

    private String getLevelTextEn(String level) {
        if (level == null) return "—";
        return switch (level) {
            case "EXCELLENT" -> "Excellent";
            case "BON" -> "Good";
            case "MOYEN" -> "Fair";
            case "FAIBLE" -> "Low";
            case "CRITIQUE" -> "Critical";
            default -> level;
        };
    }

    private String getPriorityLabelEn(String priority) {
        if (priority == null) return "";
        return switch (priority) {
            case "CRITICAL" -> "CRITICAL";
            case "HIGH" -> "HIGH PRIORITY";
            case "MEDIUM" -> "MEDIUM";
            case "LOW" -> "MAINTENANCE";
            default -> priority;
        };
    }

    private String riskLabelEn(String risk) {
        if (risk == null) return "—";
        return switch (risk.toUpperCase(Locale.ROOT)) {
            case "LOW" -> "Low";
            case "MEDIUM" -> "Medium";
            case "HIGH" -> "High";
            case "CRITICAL" -> "Critical";
            default -> risk;
        };
    }

    private DeviceRgb getScoreColor(double score) {
        if (score >= 80) return GREEN;
        if (score >= 60) return BLUE_SOFT;
        if (score >= 40) return YELLOW;
        if (score >= 20) return ORANGE;
        return RED;
    }

    private DeviceRgb getLevelColor(String level) {
        if (level == null) return TEXT_MUTED;
        return switch (level) {
            case "EXCELLENT" -> GREEN;
            case "BON" -> new DeviceRgb(52, 211, 153);
            case "MOYEN" -> YELLOW;
            case "FAIBLE" -> ORANGE;
            case "CRITIQUE" -> RED;
            default -> TEXT_MUTED;
        };
    }

    private DeviceRgb getRiskColor(String risk) {
        if (risk == null) return TEXT_MUTED;
        return switch (risk.toUpperCase(Locale.ROOT)) {
            case "LOW" -> GREEN;
            case "MEDIUM" -> YELLOW;
            case "HIGH" -> ORANGE;
            case "CRITICAL" -> RED;
            default -> TEXT_MUTED;
        };
    }

    private DeviceRgb getPriorityColor(String priority) {
        if (priority == null) return TEXT_MUTED;
        return switch (priority) {
            case "CRITICAL" -> RED;
            case "HIGH" -> ORANGE;
            case "MEDIUM" -> YELLOW;
            case "LOW" -> GREEN;
            default -> TEXT_MUTED;
        };
    }

    private static final class FooterEventHandler implements IEventHandler {
        private final PdfFont fontIt;
        private final DeviceRgb muted;

        FooterEventHandler(PdfFont fontIt, DeviceRgb muted) {
            this.fontIt = fontIt;
            this.muted = muted;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int n = pdf.getPageNumber(page);
            Rectangle ps = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdf);
            Rectangle footer = new Rectangle(ps.getLeft() + 32, ps.getBottom() + 10, ps.getWidth() - 64, 24);
            try (Canvas canvas = new Canvas(pdfCanvas, footer, true)) {
                canvas.add(new Paragraph("MindCare CVP · Confidential · Page " + n)
                        .setFont(fontIt).setFontSize(6).setFontColor(muted)
                        .setTextAlignment(TextAlignment.CENTER));
            }
        }
    }
}
