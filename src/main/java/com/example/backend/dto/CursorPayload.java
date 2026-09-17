package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPayload {
    private UUID userId;
    private String username;
    private String displayName;
    private String color;

    @JsonProperty("userName")
    public String getUserName() {
        return displayName != null ? displayName : (username != null ? username : "Peer");
    }

    @JsonAlias({"line", "lineNumber"})
    private Integer lineNumber;

    @JsonAlias({"column", "columnNumber", "col"})
    private Integer column;

    public int getLineNumber() {
        return lineNumber != null ? lineNumber : 1;
    }

    public int getColumn() {
        return column != null ? column : 1;
    }

    @JsonProperty("columnNumber")
    public int getColumnNumber() {
        return getColumn();
    }
}
