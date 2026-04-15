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
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
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

    private void generatePdf(File pdfFile, Consultation consultation, User user,
                              List<Message> messages) throws IOException {
        PdfFont font = loadChineseFont();

        try (PdfDocument pdf = new PdfDocument(new PdfWriter(pdfFile));
             Document doc = new Document(pdf)) {

            doc.setFont(font);

            // 标题
            doc.add(new Paragraph("健康问诊报告")
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold());

            doc.add(new Paragraph(" "));

            // 报告编号
            doc.add(new Paragraph("报告编号：R" + consultation.getId()).setFontSize(12));

            // 用户信息
            doc.add(new Paragraph("用户：" + user.getUsername()).setFontSize(12));

            // 问诊时间
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            doc.add(new Paragraph("问诊时间：" + consultation.getCreatedAt().format(fmt))
                    .setFontSize(12));
            if (consultation.getCompletedAt() != null) {
                doc.add(new Paragraph("结束时间：" + consultation.getCompletedAt().format(fmt))
                        .setFontSize(12));
            }

            doc.add(new Paragraph(" "));

            // 主诉
            doc.add(new Paragraph("【主诉】").setFontSize(14).setBold());
            doc.add(new Paragraph(consultation.getChiefComplaint() != null
                    ? consultation.getChiefComplaint() : "无").setFontSize(12));

            doc.add(new Paragraph(" "));

            // 对话记录
            doc.add(new Paragraph("【对话记录】").setFontSize(14).setBold());
            for (Message msg : messages) {
                String roleLabel = "user".equals(msg.getRole()) ? "患者" : "AI 医生";
                Paragraph p = new Paragraph();
                p.add(new Text(roleLabel + "：").setBold());
                p.add(new Text(msg.getContent()));
                p.setFontSize(11);
                doc.add(p);
                doc.add(new Paragraph(" ").setFontSize(4));
            }

            doc.add(new Paragraph(" "));

            // 风险评估
            doc.add(new Paragraph("【风险评估】").setFontSize(14).setBold());
            String risk = consultation.getRiskLevel();
            String riskDisplay = risk != null ? translateRisk(risk) : "未评估";
            doc.add(new Paragraph("风险等级：" + riskDisplay).setFontSize(12));

            doc.add(new Paragraph(" "));

            // 就医建议
            doc.add(new Paragraph("【就医建议】").setFontSize(14).setBold());
            doc.add(new Paragraph(buildAdvice(risk)).setFontSize(12));

            doc.add(new Paragraph(" "));

            // 免责声明
            doc.add(new Paragraph("【免责声明】").setFontSize(14).setBold());
            doc.add(new Paragraph(
                    "本报告内容仅供参考，不构成医疗诊断意见，如有不适请及时就医。")
                    .setFontSize(11)
                    .setItalic());
        }
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
