package com.health.healthdiagnosis.dto.response;

/*
  @author WU,Rowan
 * @date 2026/3/6
 */
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageResponse {

    private MessageResponse userMessage;

    private MessageResponse assistantReply;
}
