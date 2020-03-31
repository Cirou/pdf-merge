package net.cirou.tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import net.cirou.tool.bean.Comment;

public class HistoryGenerator {

	public static void writeLogFile(File pdfFile, List<Comment> comments) throws InvalidFormatException, IOException {

		// Loads the excel template file
		File file = new File("./res/Action_Log_Template.xlsx");

		// Initiates the workbook and sheet
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		Sheet sheet = workbook.getSheetAt(0);

		// Create a Font for styling header cells
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 12);
		headerFont.setColor(IndexedColors.BLACK.getIndex());

		// Create a CellStyle with the font
		CellStyle headerCellStyle = workbook.createCellStyle();
		headerCellStyle.setFont(headerFont);

		// Get the Excel Row 1
		Row headerRow = sheet.getRow(0);

		// Set the document name into A2
		Cell cell = headerRow.createCell(1);
		cell.setCellValue(pdfFile.getName());
		cell.setCellStyle(headerCellStyle);

		int currentRowIdx = 4;

		writeCommentRows(comments, sheet, currentRowIdx);

		FileOutputStream fileOut = new FileOutputStream("./res/out/Action_Log.xlsx");
		workbook.write(fileOut);
		workbook.close();
		fileOut.close();

	}

	private static int writeCommentRows(List<Comment> comments, Sheet sheet, int currentRowIdx) {

		for (Comment comment : comments) {

			Cell currentCell = null;
			Row currentRow = null;

			currentRow = getOrCreateRow(sheet, currentRowIdx);

			// Comments count for 1st columun
			currentCell = getOrCreateCell(currentRow, 0);
			currentCell.setCellValue(comment.getCommentCount());

			// File revision for 2nd columun
			currentCell = getOrCreateCell(currentRow, 1);
			currentCell.setCellValue("A01");

			// Comment author for 3rd columun
			currentCell = getOrCreateCell(currentRow, 2);
			currentCell.setCellValue(comment.getCommentUser());

			// Comment content for 4th columun
			currentCell = getOrCreateCell(currentRow, 3);
			currentCell.setCellValue(comment.getCommentAction());

			// Comments action for 5th columun
			currentCell = getOrCreateCell(currentRow, 4);
			currentCell.setCellValue(comment.getCommentText());

			currentRowIdx++;
		}

		return currentRowIdx;
	}

	private static Row getOrCreateRow(Sheet sheet, int currentRowIdx) {
		Row currentRow;
		currentRow = sheet.getRow(currentRowIdx);
		if (currentRow == null) {
			currentRow = sheet.createRow(currentRowIdx);
		}
		return currentRow;
	}

	private static Cell getOrCreateCell(Row currentRow, int cellId) {
		Cell currentCell;
		currentCell = currentRow.getCell(cellId);
		if (currentCell == null) {
			currentCell = currentRow.createCell(cellId);
		}
		return currentCell;
	}

	private static Comment retrieveAssociacion(List<Comment> associations, String id) {
		for (Comment association : associations) {
			if (association.getCommentID() != null && association.getCommentID().contentEquals(id)) {
				return association;
			}
		}
		return null;
	}

}
