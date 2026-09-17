package com.example.backend.dto;

// DTO :- data transfer objects used to carry data between layers.

import com.example.backend.entity.ProgrammingLanguage;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateRoomRequest {
     private String name ;
     private String description;
     private ProgrammingLanguage language;
}
