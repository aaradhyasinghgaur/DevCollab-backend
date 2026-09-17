package com.example.backend.dto;

import com.example.backend.ot.TextOperation;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OTSubmitMessage {
    private int revision;
    private String clientId;
    private java.util.UUID userId;

    // Direct atomic delta fields
    private String type; // 'INSERT', 'DELETE', 'REPLACE'
    private Integer position;
    private String text;
    private Integer length;
    private RangeDto range;

    @JsonAlias({"content", "code", "fullText"})
    private String fullContent;

    // Structured OT components (optional)
    private TextOperation operation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RangeDto {
        private Integer startLineNumber;
        private Integer startColumn;
        private Integer endLineNumber;
        private Integer endColumn;
    }
}
