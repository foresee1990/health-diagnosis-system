package com.health.healthdiagnosis.controller;

import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.dto.response.ReportVO;
import com.health.healthdiagnosis.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 生成问诊报告
     * POST /api/consultations/{consultationId}/report
     */
    @PostMapping("/api/consultations/{consultationId}/report")
    public Result<ReportVO> generateReport(
            @PathVariable Long consultationId,
            HttpServletRequest httpRequest) throws IOException {

        Long userId = (Long) httpRequest.getAttribute("userId");
        ReportVO vo = reportService.generateReport(consultationId, userId);
        return Result.success("报告生成成功", vo);
    }

    /**
     * 查询问诊报告信息
     * GET /api/consultations/{consultationId}/report
     */
    @GetMapping("/api/consultations/{consultationId}/report")
    public Result<ReportVO> getReport(
            @PathVariable Long consultationId,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        ReportVO vo = reportService.getReportByConsultation(consultationId, userId);
        return Result.success("获取成功", vo);
    }

    /**
     * 下载 PDF 报告文件
     * GET /api/reports/{reportId}/file
     */
    @GetMapping("/api/reports/{reportId}/file")
    public void downloadReport(
            @PathVariable Long reportId,
            HttpServletRequest httpRequest,
            HttpServletResponse response) throws IOException {

        Long userId = (Long) httpRequest.getAttribute("userId");
        File file = reportService.getReportFile(reportId, userId);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"report_" + reportId + ".pdf\"");
        response.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
        }
    }
}
