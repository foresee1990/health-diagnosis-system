package com.health.healthdiagnosis.dto.response;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import lombok.Data;
import java.util.List;

@Data
public class ConsultationPageResponse {

    private long total;

    private long page;

    private long size;

    private long pages;

    private List<ConsultationItemResponse> consultations;
}