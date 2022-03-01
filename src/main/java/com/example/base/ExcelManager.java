package com.example.base;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.ITestResult;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ExcelManager {

    private static final Logger LOGGER = LogManager.getLogger(ExcelManager.class);
    private static final List<Map<String, String>> excelRow;

    static {
        excelRow = getExcelRow();
    }

    public static synchronized List<Map<String, String>> getControllerRowsList() {
        return excelRow;
    }

    private static synchronized List<Map<String, String>> getExcelRow() {
        FileInputStream fileInputStream;
        List<Map<String, String>> rowMapList;
        try {
            fileInputStream = new FileInputStream(Constants.RUN_MANAGER_WORKBOOK.toFile());
            Workbook workbook = new XSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheet(Constants.CONTROLLER_SHEET_NAME);
            List<String> headerList = new LinkedList<>();
            for (int i = 0; i < sheet.getRow(0).getPhysicalNumberOfCells(); i++) {
                headerList.add(sheet.getRow(0).getCell(i).getStringCellValue());
            }
            LOGGER.debug("Headers list [{}]", headerList);
            rowMapList = new LinkedList<>();
            for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int j = 0; j < row.getPhysicalNumberOfCells(); j++) {
                    Cell cell = row.getCell(j);
                    if (StringUtils.isNotBlank(getCellValue(cell)) && !getCellValue(cell).equals("P_Key")) {
                        rowMap.put(headerList.get(j), getCellValue(cell));
                        LOGGER.debug("Added Key : [{}] | Value [{}] to row map", headerList.get(j), getCellValue(cell));
                    } else {
                        break;
                    }
                }
                rowMapList.add(rowMap);
            }
            fileInputStream.close();
        } catch (IOException e) {
            LOGGER.error(e);
            throw new RuntimeException(e);
        }
        return rowMapList.stream().filter(map -> map.size() > 0).collect(Collectors.toList());
    }

    public static synchronized List<Map<String, String>> getExcelRowsAsListOfMap(String excelWorkbookName, String excelSheetName,
                                                                                 String testMethodName) {
        FileInputStream fileInputStream;
        List<Map<String, String>> rowMapList = new LinkedList<>();
        try {
            fileInputStream = new FileInputStream(excelWorkbookName);
            Workbook workbook = new XSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheet(excelSheetName);
            List<String> headerList = new LinkedList<>();
            for (int i = 0; i < sheet.getRow(0).getPhysicalNumberOfCells(); i++) {
                headerList.add(sheet.getRow(0).getCell(i).getStringCellValue());
            }
            LOGGER.debug("Headers list [{}]", headerList);
            for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int j = 1; j < row.getPhysicalNumberOfCells(); j++) {
                    Cell cell = row.getCell(j);
                    if (StringUtils.isNotBlank(getCellValue(cell)) && getCellValue(sheet.getRow(i).getCell(1)).equals(testMethodName)) {
                        rowMap.put(headerList.get(j), getCellValue(cell));
                        LOGGER.debug("Added Key : [{}] | Value [{}] to row map", headerList.get(j), getCellValue(cell));
                    } else {
                        break;
                    }
                }
                if (rowMap.size() > 0) {
                    rowMapList.add(rowMap);
                }
            }
            fileInputStream.close();
        } catch (IOException e) {
            LOGGER.error(e);
            throw new RuntimeException(e);
        }
        return rowMapList;
    }

    protected static synchronized void writeTestStatusToExcel(ITestResult result) {
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(Constants.RUN_MANAGER_WORKBOOK.toFile());
            Workbook workbook = new XSSFWorkbook(fileInputStream);
            Sheet sheet = workbook.getSheet(Constants.CONTROLLER_SHEET_NAME);
            Map<String, Integer> headersMap = new LinkedHashMap<>();
            for (int i = 0; i < sheet.getRow(0).getPhysicalNumberOfCells(); i++) {
                headersMap.put(sheet.getRow(0).getCell(i).getStringCellValue(), i);
            }
            for (int columnIndex = 1; columnIndex < sheet.getPhysicalNumberOfRows(); columnIndex++) {
                if (StringUtils.isNotBlank(sheet.getRow(columnIndex).getCell(headersMap.get("TestMethodName")).getStringCellValue())) {
                    String testMethodName = sheet.getRow(columnIndex).getCell(headersMap.get("TestMethodName")).getStringCellValue();
                    String executeFlag = sheet.getRow(columnIndex).getCell(headersMap.get("Execute")).getStringCellValue();
                    if (executeFlag.equalsIgnoreCase("yes") && result.getMethod().getMethodName().equals(testMethodName)) {
                        String status = result.getStatus() == ITestResult.FAILURE ? "Failed" : "Passed";
                        sheet.getRow(columnIndex).getCell(headersMap.get("Status")).setCellValue(status);
                    }
                }
            }
            fileInputStream.close();
            FileOutputStream fileOutputStream = new FileOutputStream(Constants.RUN_MANAGER_WORKBOOK.toFile());
            workbook.write(fileOutputStream);
            fileOutputStream.close();
        } catch (Exception e) {
            LOGGER.error(e);
            throw new RuntimeException(e);
        }
        LOGGER.debug("Successfully wrote back test result to TestRunner sheet");
    }

    protected static Map<String, String> getControllerRowMapByTestMethodName(String testMethodName) {
        return excelRow.stream().filter(map -> map.get("TestMethodName").equals(testMethodName)).collect(Collectors.toList()).get(0);
    }

    private static String getCellValue(Cell cell) {
        return cell.getCellType().equals(CellType.NUMERIC) ? String.valueOf((int) cell.getNumericCellValue()) : cell.getStringCellValue();
    }

    // My Code
    public static Sheet getSheet(Path excelPath, String sheetName) throws Exception {
        FileInputStream fileInputStream;
        fileInputStream = new FileInputStream(excelPath.toFile());
        Workbook workbook = new XSSFWorkbook(fileInputStream);
        return workbook.getSheet(sheetName);
    }

    public static int getRowIndex(String reference, Path excelPath, String sheetName, int rowNumber) throws Exception {
        XSSFRow row;
        Iterator<Row> rows = ExcelManager.getSheet(excelPath, sheetName).rowIterator();
        while (rows.hasNext()) {
            row = (XSSFRow) rows.next();
            if (row.getCell(rowNumber).toString().trim().equals(reference.trim())) {
                return row.getRowNum();
            }
        }
        LOGGER.error("No Such Reference Found. Reference -> " + reference);
        throw new Exception("No Such Reference Found. Reference -> " + reference);
    }

    public static String getCellValue(XSSFCell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        } else if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            return value + "";
        } else if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.BOOLEAN || cell.getCellType() == CellType.ERROR
                || cell.getCellType() == CellType.FORMULA) {
            throw new RuntimeException("Cell Type is not supported ");
        }
        return "";
    }

    public static Map<String, String> getRowValue(int rowNumber, Path excelPath, String sheetName) throws Exception {
        Map<String, String> rowValue = new HashMap<>();
        Sheet sheet = ExcelManager.getSheet(excelPath, sheetName);
        Iterator<Cell> keyCells = sheet.getRow(0).cellIterator();
        XSSFRow valueRow = (XSSFRow) sheet.getRow(rowNumber);
        int i = 0;
        while (keyCells.hasNext()) {
            String key = keyCells.next().toString().trim();
            String value;
            try {
                value = getCellValue(valueRow.getCell(i)).trim();
            } catch (NoSuchElementException nse) {
                value = "";
            }
            i++;
            rowValue.put(key, value);
        }
        return rowValue;
    }

    public static Map<String, String> getRowValue(int rowNumber, int headerRowNumber, Path excelPath, String sheetName) throws Exception {
        Map<String, String> rowValue = new HashMap<>();
        Sheet sheet = ExcelManager.getSheet(excelPath, sheetName);
        Iterator<Cell> keyCells = sheet.getRow(headerRowNumber).cellIterator();
        XSSFRow valueRow = (XSSFRow) sheet.getRow(rowNumber);
        int i = 0;
        while (keyCells.hasNext()) {
            String key = keyCells.next().toString().trim();
            String value;
            try {
                value = getCellValue(valueRow.getCell(i)).trim();
            } catch (NoSuchElementException nse) {
                value = "";
            }
            i++;
            rowValue.put(key, value);
        }
        return rowValue;
    }

    public static Map<String, String> getRowValue(String reference, Path excelPath, String sheetName) throws Exception {
        return getRowValue(getRowIndex(reference, excelPath, sheetName, 0), excelPath, sheetName);
    }

    private static String getMobileSettingsReference() throws Exception {
        FileInputStream fileInputStream = new FileInputStream(Constants.RUN_MANAGER_WORKBOOK.toFile());
        Workbook workbook = new XSSFWorkbook(fileInputStream);
        Sheet sheet = workbook.getSheet(Constants.SETTINGS_SHEET_NAME);
        return sheet.getRow(2).getCell(1).toString();
    }

    private static String getWebSettingsReference() throws Exception {
        FileInputStream fileInputStream = new FileInputStream(Constants.RUN_MANAGER_WORKBOOK.toFile());
        Workbook workbook = new XSSFWorkbook(fileInputStream);
        Sheet sheet = workbook.getSheet(Constants.SETTINGS_SHEET_NAME);
        return sheet.getRow(1).getCell(1).toString();
    }

    private static Map<String, String> getWebSettingsDetailsAsMap() throws Exception {
        return getRowValue(getRowIndex(ExcelManager.getMobileSettingsReference(), Constants.RUN_MANAGER_WORKBOOK, Constants.SETTINGS_SHEET_NAME, 0),
                getRowIndex("WebConfiguration", Constants.RUN_MANAGER_WORKBOOK, Constants.SETTINGS_SHEET_NAME, 0),
                Constants.RUN_MANAGER_WORKBOOK, Constants.SETTINGS_SHEET_NAME);
    }

    public static Map<String, String> getMobileSettingsDetailsAsMap() throws Exception {
        return getRowValue(getRowIndex(ExcelManager.getMobileSettingsReference(), Constants.RUN_MANAGER_WORKBOOK, Constants.SETTINGS_SHEET_NAME, 0),
                getRowIndex("MobileConfiguration", Constants.RUN_MANAGER_WORKBOOK, Constants.SETTINGS_SHEET_NAME, 0),
                Constants.RUN_MANAGER_WORKBOOK, Constants.SETTINGS_SHEET_NAME);
    }

}
