package com.health.healthdiagnosis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.healthdiagnosis.dto.response.ReportVO;
import com.health.healthdiagnosis.entity.Consultation;
import com.health.healthdiagnosis.entity.Message;
import com.health.healthdiagnosis.entity.Report;
import com.health.healthdiagnosis.entity.User;
import com.health.healthdiagnosis.exception.BusinessException;
import com.health.healthdiagnosis.mapper.ConsultationMapper;
import com.health.healthdiagnosis.mapper.MessageMapper;
import com.health.healthdiagnosis.mapper.ReportMapper;
import com.health.healthdiagnosis.mapper.UserMapper;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.health.healthdiagnosis.common.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ConsultationMapper consultationMapper;
    private final MessageMapper messageMapper;
    private final ReportMapper reportMapper;
    private final UserMapper userMapper;

    @Value("${app.report.base-dir}")
    private String baseDir;

    @Transactional(rollbackFor = Exception.class)
    public ReportVO generateReport(Long consultationId, Long userId) throws IOException {
        // 1. 校验会话归属
        Consultation consultation = consultationMapper.selectById(consultationId);
        if (consultation == null || !consultation.getUserId().equals(userId)) {
            throw new BusinessException(ACCESS_DENIED, "ACCESS_DENIED");
        }

        // 2. 校验状态为 completed
        if (!"completed".equals(consultation.getStatus())) {
            throw new BusinessException(CONSULTATION_ALREADY_COMPLETED, "会话尚未结束，无法生成报告");
        }

        // 3. 幂等：报告已存在则直接返回
        Report existing = reportMapper.selectOne(
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getConsultationId, consultationId)
        );
        if (existing != null) {
            return toVO(existing);
        }

        // 4. 查询数据
        User user = userMapper.selectById(userId);
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConsultationId, consultationId)
                        .orderByAsc(Message::getCreatedAt)
        );

        // 5. 确定存储目录
        LocalDateTime now = LocalDateTime.now();
        String yearMonth = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        File dir = new File(baseDir + yearMonth);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "report_" + consultationId + "_" + System.currentTimeMillis() + ".pdf";
        String relativePath = yearMonth + "/" + fileName;
        File pdfFile = new File(dir, fileName);

        // 6. 生成 PDF
        generatePdf(pdfFile, consultation, user, messages);

        // 7. 写入 reports 表
        Report report = new Report();
        report.setConsultationId(consultationId);
        report.setFilePath(relativePath);
        report.setFileSize((int) pdfFile.length());
        report.setCreatedAt(now);
        reportMapper.insert(report);

        log.info("报告生成成功, consultationId={}, path={}", consultationId, relativePath);
        return toVO(report);
    }

    /**
     * 加载中文字体（嵌入式 TrueType，避免非嵌入 CJK 字体在部分阅读器中渲染出虚线）。
     * 优先级：classpath fonts/NotoSansSC-Regular.ttf → Windows 系统字体 → STSong-Light 兜底
     */
    private PdfFont loadChineseFont() throws IOException {
        // 1. 优先读取 classpath 字体（放 src/main/resources/fonts/NotoSansSC-Regular.ttf）
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("fonts/NotoSansSC-Regular.ttf")) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                return PdfFontFactory.createFont(bytes, PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            }
        }
        // 2. 回退：Windows 系统中文字体（黑体 / 宋体 / 雅黑，按顺序尝试）
        String[] candidates = {
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simsun.ttc,0",
            "C:/Windows/Fonts/msyh.ttc,0"
        };
        for (String path : candidates) {
            File fontFile = new File(path.contains(",") ? path.substring(0, path.indexOf(',')) : path);
            if (fontFile.exists()) {
                log.info("使用系统字体：{}", path);
                return PdfFontFactory.createFont(path, PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            }
        }
        // 3. 最终兜底（可能在某些阅读器中出现渲染问题）
        log.warn("未找到嵌入式中文字体，回退到 STSong-Light");
        return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
    }

    // 风险等级颜色
    private static final DeviceRgb COLOR_LOW    = new DeviceRgb(5,   150, 105);
    private static final DeviceRgb COLOR_MEDIUM = new DeviceRgb(217, 119,   6);
    private static final DeviceRgb COLOR_HIGH   = new DeviceRgb(220,  38,  38);
    private static final DeviceRgb COLOR_URGENT = new DeviceRgb(153,  27,  27);
    private static final DeviceRgb COLOR_GRAY   = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb COLOR_PRIMARY = new DeviceRgb(47, 128, 237);
    private static final DeviceRgb COLOR_AI_BG  = new DeviceRgb(235, 244, 255);

    private void generatePdf(File pdfFile, Consultation consultation, User user,
                              List<Message> messages) throws IOException {
        PdfFont font = loadChineseFont();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        try (PdfDocument pdf = new PdfDocument(new PdfWriter(pdfFile));
             Document doc = new Document(pdf)) {

            doc.setFont(font).setMargins(48, 48, 48, 48);

            // ── 标题区 ──────────────────────────────────────────
            doc.add(new Paragraph("健康问诊综合评估报告")
                    .setFontSize(22).setBold()
                    .setFontColor(COLOR_PRIMARY)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("AI-Assisted Health Consultation Report")
                    .setFontSize(10).setFontColor(COLOR_GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(" ").setFontSize(4));
            doc.add(new LineSeparator(new SolidLine(1f)));
            doc.add(new Paragraph(" ").setFontSize(6));

            // ── 基本信息 ────────────────────────────────────────
            addSectionTitle(doc, "基本信息");
            doc.add(new Paragraph("报告编号：R-" + String.format("%06d", consultation.getId()))
                    .setFontSize(11));
            doc.add(new Paragraph("患者账号：" + user.getUsername()).setFontSize(11));
            doc.add(new Paragraph("问诊开始：" + consultation.getCreatedAt().format(fmt))
                    .setFontSize(11));
            if (consultation.getCompletedAt() != null) {
                doc.add(new Paragraph("问诊结束：" + consultation.getCompletedAt().format(fmt))
                        .setFontSize(11));
            }
            doc.add(new Paragraph("报告生成：" + LocalDateTime.now().format(fmt)).setFontSize(11));
            doc.add(new Paragraph(" ").setFontSize(6));

            // ── 主诉 ────────────────────────────────────────────
            addSectionTitle(doc, "主诉");
            String chiefComplaint = consultation.getChiefComplaint();
            doc.add(new Paragraph(chiefComplaint != null && !chiefComplaint.isBlank()
                    ? chiefComplaint : "（未填写）").setFontSize(12));
            doc.add(new Paragraph(" ").setFontSize(6));

            // ── 风险评估 ────────────────────────────────────────
            addSectionTitle(doc, "风险评估");
            String risk = consultation.getRiskLevel();
            DeviceRgb riskColor = riskColor(risk);
            String riskDisplay = risk != null ? translateRisk(risk) : "未评估";
            doc.add(new Paragraph()
                    .add(new Text("风险等级：").setFontSize(12))
                    .add(new Text("  " + riskDisplay + "  ")
                            .setFontSize(12).setBold()
                            .setFontColor(riskColor)));
            doc.add(new Paragraph(buildAdvice(risk)).setFontSize(11).setFontColor(COLOR_GRAY));
            doc.add(new Paragraph(" ").setFontSize(6));

            // ── 最终诊断建议 ─────────────────────────────────────
            addSectionTitle(doc, "AI 诊断建议");
            List<String> adviceLines = messages.stream()
                    .filter(m -> "assistant".equals(m.getRole()))
                    .reduce((a, b) -> b)
                    .map(m -> stripMarkdownLines(m.getContent()))
                    .orElse(List.of(buildAdvice(risk)));
            for (String line : adviceLines) {
                if (line.isBlank()) {
                    doc.add(new Paragraph(" ").setFontSize(4));
                } else {
                    doc.add(new Paragraph(line).setFontSize(11));
                }
            }
            doc.add(new Paragraph(" ").setFontSize(6));

            // ── 完整问诊记录 ─────────────────────────────────────
            addSectionTitle(doc, "完整问诊对话记录");
            for (Message msg : messages) {
                if ("user".equals(msg.getRole())) {
                    doc.add(new Paragraph()
                            .add(new Text("患  者：").setFontSize(10).setBold().setFontColor(COLOR_GRAY))
                            .add(new Text(msg.getContent()).setFontSize(11)));
                } else {
                    List<String> lines = stripMarkdownLines(msg.getContent());
                    Paragraph p = new Paragraph()
                            .add(new Text("AI助手：").setFontSize(10).setBold().setFontColor(COLOR_PRIMARY));
                    doc.add(p);
                    for (String line : lines) {
                        if (!line.isBlank()) {
                            doc.add(new Paragraph(line).setFontSize(11).setMarginLeft(48));
                        }
                    }
                }
                doc.add(new Paragraph(" ").setFontSize(3));
            }
            doc.add(new Paragraph(" ").setFontSize(6));

            // ── 免责声明 ────────────────────────────────────────
            doc.add(new LineSeparator(new SolidLine(0.5f)));
            doc.add(new Paragraph(" ").setFontSize(4));
            doc.add(new Paragraph(
                    "【免责声明】本报告由 AI 辅助生成，内容仅供参考，不构成医疗诊断意见。" +
                    "如有不适，请及时前往正规医疗机构就诊，遵从医生建议。")
                    .setFontSize(9).setFontColor(COLOR_GRAY).setItalic());
        }
    }

    private void addSectionTitle(Document doc, String title) throws IOException {
        doc.add(new Paragraph(title)
                .setFontSize(13).setBold()
                .setFontColor(COLOR_PRIMARY)
                .setMarginBottom(4));
    }

    private DeviceRgb riskColor(String risk) {
        if (risk == null) return COLOR_GRAY;
        return switch (risk) {
            case "low"    -> COLOR_LOW;
            case "medium" -> COLOR_MEDIUM;
            case "high"   -> COLOR_HIGH;
            case "urgent" -> COLOR_URGENT;
            default       -> COLOR_GRAY;
        };
    }

    /** 将 Markdown 文本转为适合 PDF 纯文本展示的行列表（逐行处理，兼容 \r\n） */
    private List<String> stripMarkdownLines(String text) {
        if (text == null) return List.of();

        // 先去掉 think 块
        String clean = text.replaceAll("(?is)<think>.*?</think>", "");

        String[] lines = clean.split("\\r?\\n");
        List<String> result = new java.util.ArrayList<>();
        boolean inCodeBlock = false;

        for (String raw : lines) {
            String line = raw.trim();

            // 代码围栏切换
            if (line.startsWith("```") || line.startsWith("~~~")) {
                inCodeBlock = !inCodeBlock;
                continue;
            }
            if (inCodeBlock) continue;

            // 水平分隔线
            if (line.matches("[-*_]{3,}")) continue;

            // 标题行：去掉所有前导 #
            if (line.startsWith("#")) {
                line = line.replaceAll("^#{1,6}\\s*", "");
            }

            // 表格分隔行（|---|）
            if (line.matches("[\\|\\-:\\s]+")) continue;

            // 表格内容行
            if (line.startsWith("|")) {
                String inner = line.replaceAll("^\\|", "").replaceAll("\\|$", "");
                StringBuilder row = new StringBuilder();
                for (String cell : inner.split("\\|")) {
                    String c = cell.trim();
                    if (!c.isEmpty() && !c.matches("[-:]+")) {
                        if (!row.isEmpty()) row.append("  ");
                        row.append(c);
                    }
                }
                line = row.toString();
                if (line.isEmpty()) continue;
            }

            // 无序列表 → •
            if (line.matches("^[-*]\\s+.*")) {
                line = "• " + line.replaceAll("^[-*]\\s+", "");
            }

            // 去除加粗 / 斜体 / 行内代码 / 残留 |
            line = line.replaceAll("\\*{1,3}(.+?)\\*{1,3}", "$1")
                       .replaceAll("`([^`]+)`", "$1")
                       .replace("|", "  ");

            result.add(line);
        }
        return result;
    }

    private String translateRisk(String risk) {
        return switch (risk) {
            case "low"    -> "低风险（low）";
            case "medium" -> "中风险（medium）";
            case "high"   -> "高风险（high）";
            case "urgent" -> "紧急（urgent）";
            default       -> risk;
        };
    }

    private String buildAdvice(String risk) {
        if (risk == null) return "请根据实际症状咨询专业医生。";
        return switch (risk) {
            case "low"    -> "症状较轻，建议多休息、多饮水，如症状持续请就医。";
            case "medium" -> "症状需要关注，建议尽快前往社区医院就诊。";
            case "high"   -> "症状较重，建议尽快前往医院就诊，勿拖延。";
            case "urgent" -> "症状紧急，请立即前往急诊或拨打急救电话 120。";
            default       -> "请根据实际症状咨询专业医生。";
        };
    }

    public ReportVO getReportByConsultation(Long consultationId, Long userId) {
        Consultation consultation = consultationMapper.selectById(consultationId);
        if (consultation == null || !consultation.getUserId().equals(userId)) {
            throw new BusinessException(ACCESS_DENIED, "ACCESS_DENIED");
        }
        Report report = reportMapper.selectOne(
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getConsultationId, consultationId)
        );
        if (report == null) {
            throw new BusinessException(NOT_FOUND, "报告不存在");
        }
        return toVO(report);
    }

    public File getReportFile(Long reportId, Long userId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(NOT_FOUND, "报告不存在");
        }
        Consultation consultation = consultationMapper.selectById(report.getConsultationId());
        if (consultation == null || !consultation.getUserId().equals(userId)) {
            throw new BusinessException(ACCESS_DENIED, "ACCESS_DENIED");
        }
        return new File(baseDir + report.getFilePath());
    }

    private ReportVO toVO(Report report) {
        String downloadUrl = "/api/reports/" + report.getId() + "/file";
        return new ReportVO(report.getId(), downloadUrl, report.getFileSize(), report.getCreatedAt());
    }
}
