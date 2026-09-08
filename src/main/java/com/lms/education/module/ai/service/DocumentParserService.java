package com.lms.education.module.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class DocumentParserService {

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Cannot extract text from an empty file.");
            return "";
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return "";
        }

        try (InputStream inputStream = file.getInputStream()) {
            return extractText(inputStream, fileName);
        } catch (Exception e) {
            log.error("Failed to read file stream: {}", fileName, e);
            return "";
        }
    }

    public String extractText(InputStream inputStream, String fileName) {
        if (inputStream == null || fileName == null) {
            return "";
        }
        String extension = getFileExtension(fileName).toLowerCase();

        try {
            switch (extension) {
                case "pdf":
                    return extractFromPdf(inputStream);
                case "docx":
                    return extractFromDocx(inputStream);
                case "txt":
                    return extractFromTxt(inputStream);
                default:
                    log.info("Unsupported file extension for text extraction: {}", extension);
                    return "";
            }
        } catch (Exception e) {
            log.error("Failed to extract text from file: {}", fileName, e);
            return "";
        }
    }

    private String extractFromPdf(InputStream inputStream) throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("pdf-ingest-", ".tmp");
        try {
            java.nio.file.Files.copy(inputStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            try (PDDocument document = Loader.loadPDF(tempFile)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } finally {
            tempFile.delete();
        }
    }

    private String extractFromDocx(InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractFromTxt(InputStream inputStream) throws Exception {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
}
