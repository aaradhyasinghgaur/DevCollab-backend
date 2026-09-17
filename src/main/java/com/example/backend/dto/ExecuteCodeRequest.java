package com.example.backend.dto;

import com.example.backend.entity.ProgrammingLanguage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {
    private String code;
    private ProgrammingLanguage language;
    private String stdin;
}
