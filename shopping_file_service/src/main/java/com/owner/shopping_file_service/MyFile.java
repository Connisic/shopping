package com.owner.shopping_file_service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyFile implements Serializable {
    private String fileName;
    private byte[] file;
}
