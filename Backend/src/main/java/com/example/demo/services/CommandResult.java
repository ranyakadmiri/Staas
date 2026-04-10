package com.example.demo.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandResult {
    private int exitCode;
    private String stdout;
    private String stderr;
}