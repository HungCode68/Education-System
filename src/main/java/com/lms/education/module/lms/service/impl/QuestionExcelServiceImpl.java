package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.module.lms.dto.QuestionDto;
import com.lms.education.module.lms.dto.QuestionImportErrorDto;
import com.lms.education.module.lms.dto.QuestionImportResultDto;
import com.lms.education.module.lms.dto.QuestionOptionDto;
import com.lms.education.module.lms.service.QuestionExcelService;
import com.lms.education.module.lms.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionExcelServiceImpl implements QuestionExcelService {

    private final QuestionService questionService;

    @Override
    public byte[] generateQuestionImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Mẫu import câu hỏi");

            String[] headers = {
                    "STT",
                    "Nội dung câu hỏi (*)",
                    "Loại câu hỏi (*)",
                    "Đáp án A (*)",
                    "Đáp án B (*)",
                    "Đáp án C",
                    "Đáp án D",
                    "Đáp án E",
                    "Đáp án F",
                    "Đáp án đúng (*)",
                    "Đoạn văn đọc hiểu / Giải thích",
                    "URL Hình ảnh/Media"
            };

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample Row 1: MULTIPLE_CHOICE (4 options A-D)
            Row sample1 = sheet.createRow(1);
            sample1.createCell(0).setCellValue("1");
            sample1.createCell(1).setCellValue("Thủ đô của Việt Nam là thành phố nào?");
            sample1.createCell(2).setCellValue("MULTIPLE_CHOICE");
            sample1.createCell(3).setCellValue("Hà Nội");
            sample1.createCell(4).setCellValue("TP. Hồ Chí Minh");
            sample1.createCell(5).setCellValue("Đà Nẵng");
            sample1.createCell(6).setCellValue("Huế");
            sample1.createCell(7).setCellValue("");
            sample1.createCell(8).setCellValue("");
            sample1.createCell(9).setCellValue("A");
            sample1.createCell(10).setCellValue("Hà Nội là thủ đô của nước CHXHCN Việt Nam.");
            sample1.createCell(11).setCellValue("");

            // Sample Row 2: TRUE_FALSE (2 options A-B)
            Row sample2 = sheet.createRow(2);
            sample2.createCell(0).setCellValue("2");
            sample2.createCell(1).setCellValue("Trái Đất có hình vuông đúng hay sai?");
            sample2.createCell(2).setCellValue("TRUE_FALSE");
            sample2.createCell(3).setCellValue("Đúng");
            sample2.createCell(4).setCellValue("Sai");
            sample2.createCell(5).setCellValue("");
            sample2.createCell(6).setCellValue("");
            sample2.createCell(7).setCellValue("");
            sample2.createCell(8).setCellValue("");
            sample2.createCell(9).setCellValue("B");
            sample2.createCell(10).setCellValue("Trái Đất có dạng hình cầu hơi dẹt ở hai cực.");
            sample2.createCell(11).setCellValue("");

            // Sample Row 3: MULTIPLE_CHOICE (6 options A-F, multiple correct answers)
            Row sample3 = sheet.createRow(3);
            sample3.createCell(0).setCellValue("3");
            sample3.createCell(1).setCellValue("Chọn các ngôn ngữ lập trình chạy trên máy ảo JVM?");
            sample3.createCell(2).setCellValue("MULTIPLE_CHOICE");
            sample3.createCell(3).setCellValue("Java");
            sample3.createCell(4).setCellValue("Python");
            sample3.createCell(5).setCellValue("Kotlin");
            sample3.createCell(6).setCellValue("C++");
            sample3.createCell(7).setCellValue("Scala");
            sample3.createCell(8).setCellValue("PHP");
            sample3.createCell(9).setCellValue("A,C,E");
            sample3.createCell(10).setCellValue("Java, Kotlin và Scala đều biên dịch ra bytecode chạy trên JVM.");
            sample3.createCell(11).setCellValue("");

            // Add Data Validation Dropdown for Question Type (Column C / Index 2)
            addDataValidation((XSSFSheet) sheet);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Extra padding
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1500, 20000));
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Lỗi khi tạo file mẫu Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo file mẫu import câu hỏi: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public QuestionImportResultDto importQuestionsFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new OperationNotPermittedException("Vui lòng tải lên file Excel không được để trống!");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xlsx")) {
            throw new OperationNotPermittedException("Hệ thống chỉ hỗ trợ định dạng Excel chuẩn XSLX (.xlsx)!");
        }

        List<QuestionImportErrorDto> errors = new ArrayList<>();
        List<QuestionDto> validQuestions = new ArrayList<>();
        int totalRows = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Detect column index of "Đáp án đúng (*)" or default to 9
            int correctColIdx = detectCorrectAnswerColumnIndex(sheet.getRow(0));

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                totalRows++;

                String content = getCellValue(row.getCell(1));
                String questionType = getCellValue(row.getCell(2));
                String correctAnswerStr = getCellValue(row.getCell(correctColIdx));
                String readingPassage = getCellValue(row.getCell(correctColIdx + 1));
                String mediaUrl = getCellValue(row.getCell(correctColIdx + 2));

                // Validate required fields
                if (content.isEmpty()) {
                    errors.add(new QuestionImportErrorDto(rowIndex + 1, "Nội dung câu hỏi", "Nội dung câu hỏi không được để trống"));
                }
                if (questionType.isEmpty()) {
                    questionType = "MULTIPLE_CHOICE"; // Default if empty
                } else if (!questionType.equalsIgnoreCase("MULTIPLE_CHOICE") && !questionType.equalsIgnoreCase("TRUE_FALSE")) {
                    errors.add(new QuestionImportErrorDto(rowIndex + 1, "Loại câu hỏi", "Loại câu hỏi phải là MULTIPLE_CHOICE hoặc TRUE_FALSE"));
                }
                if (correctAnswerStr.isEmpty()) {
                    errors.add(new QuestionImportErrorDto(rowIndex + 1, "Đáp án đúng", "Chưa nhập đáp án đúng (ví dụ: A hoặc A,B)"));
                }

                // Dynamically scan option columns from index 3 to correctColIdx - 1
                List<String> optionTexts = new ArrayList<>();
                for (int col = 3; col < correctColIdx; col++) {
                    String optVal = getCellValue(row.getCell(col));
                    if (!optVal.isEmpty()) {
                        optionTexts.add(optVal);
                    }
                }

                if (optionTexts.size() < 2) {
                    errors.add(new QuestionImportErrorDto(rowIndex + 1, "Đáp án lựa chọn",
                            "Câu hỏi phải có ít nhất 2 đáp án (hiện tại có " + optionTexts.size() + " đáp án)"));
                    continue;
                }

                // Parse correct answer letters (A, B, C, D, E, F...) -> indices
                Set<Integer> correctIndices = new HashSet<>();
                String[] tokens = correctAnswerStr.split("[,;\\s]+");
                for (String token : tokens) {
                    String clean = token.trim().toUpperCase();
                    int idx = letterToIndex(clean);
                    if (idx == -1) {
                        errors.add(new QuestionImportErrorDto(rowIndex + 1, "Đáp án đúng",
                                "Ký tự đáp án đúng '" + token + "' không hợp lệ (chỉ chấp nhận A, B, C, D, E, F...)"));
                    } else if (idx >= optionTexts.size()) {
                        errors.add(new QuestionImportErrorDto(rowIndex + 1, "Đáp án đúng",
                                "Đáp án đúng '" + token + "' vượt quá số lượng đáp án đã nhập (" + optionTexts.size() + " đáp án)"));
                    } else {
                        correctIndices.add(idx);
                    }
                }

                if (correctIndices.isEmpty()) {
                    errors.add(new QuestionImportErrorDto(rowIndex + 1, "Đáp án đúng", "Không tìm thấy ký tự đáp án đúng hợp lệ"));
                }

                if (errors.isEmpty()) {
                    // Build option DTOs
                    List<QuestionOptionDto> optionDtos = new ArrayList<>();
                    for (int i = 0; i < optionTexts.size(); i++) {
                        optionDtos.add(QuestionOptionDto.builder()
                                .optionContent(optionTexts.get(i))
                                .isCorrect(correctIndices.contains(i))
                                .build());
                    }

                    QuestionDto dto = QuestionDto.builder()
                            .questionType(questionType.toUpperCase())
                            .content(content)
                            .readingPassage(readingPassage.isEmpty() ? null : readingPassage)
                            .mediaUrl(mediaUrl.isEmpty() ? null : mediaUrl)
                            .options(optionDtos)
                            .build();

                    validQuestions.add(dto);
                }
            }

            if (!errors.isEmpty()) {
                log.warn("Import Excel bị từ chối do có {} lỗi trên tổng số {} dòng", errors.size(), totalRows);
                return QuestionImportResultDto.builder()
                        .success(false)
                        .totalRows(totalRows)
                        .successCount(0)
                        .errorCount(errors.size())
                        .errors(errors)
                        .build();
            }

            // Save all questions atomically
            List<QuestionDto> savedList = new ArrayList<>();
            for (QuestionDto q : validQuestions) {
                savedList.add(questionService.create(q, null));
            }


            log.info("Đã import thành công {} câu hỏi từ file Excel", savedList.size());
            return QuestionImportResultDto.builder()
                    .success(true)
                    .totalRows(totalRows)
                    .successCount(savedList.size())
                    .errorCount(0)
                    .importedQuestions(savedList)
                    .build();

        } catch (IOException e) {
            log.error("Lỗi khi đọc file Excel import câu hỏi: {}", e.getMessage(), e);
            throw new OperationNotPermittedException("Lỗi đọc file Excel: " + e.getMessage());
        }
    }

    private int detectCorrectAnswerColumnIndex(Row headerRow) {
        if (headerRow != null) {
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String val = getCellValue(headerRow.getCell(i)).toLowerCase();
                if (val.contains("đáp án đúng") || val.contains("correct answer") || val.contains("answer key") || val.equals("correct")) {
                    return i;
                }
            }
        }
        return 9; // Default Column J / index 9
    }


    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValue(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private int letterToIndex(String letter) {
        if (letter == null || letter.isEmpty()) {
            return -1;
        }
        char ch = letter.toUpperCase().charAt(0);
        if (ch >= 'A' && ch <= 'Z') {
            return ch - 'A';
        }
        return -1;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void addDataValidation(XSSFSheet sheet) {
        DataValidationHelper validationHelper = new XSSFDataValidationHelper(sheet);
        CellRangeAddressList addressList = new CellRangeAddressList(1, 500, 2, 2);
        DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(
                new String[]{"MULTIPLE_CHOICE", "TRUE_FALSE"});
        DataValidation dataValidation = validationHelper.createValidation(constraint, addressList);
        dataValidation.setShowErrorBox(true);
        sheet.addValidationData(dataValidation);
    }
}
