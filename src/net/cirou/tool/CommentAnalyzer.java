package net.cirou.tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
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

	private static List<PDAnnotation> comments = new ArrayList<>();

	public static void main(String[] args) throws IOException, InvalidFormatException {

		// takes the output file
		File file = new File("./res/out/doc_merge.pdf");
		PDDocument doc = PDDocument.load(file);
		getDocComments(doc);
		List<Comment> associations = generateHistoryFile(doc);
		fillExcelFile(file, associations);

	}

	private static void getDocComments(PDDocument doc) throws IOException {
		if (doc != null) {
			int numberOfPages = doc.getNumberOfPages();
			for (int i = 0; i < numberOfPages; i++) {
				PDPage page = doc.getPage(i);
				if (page != null && page.getAnnotations() != null) {
					comments.addAll(page.getAnnotations());
				}
			}
		}
	}

	private static List<Comment> generateHistoryFile(PDDocument doc) {

		List<Comment> associations = new ArrayList<>();

		try {
			// Iterates through the comments

			int count = 1;
			for (PDAnnotation pdannotation : comments) {

				COSDictionary dict = pdannotation.getCOSObject();

				if (dict.containsKey(COSName.CONTENTS)) {

					Comment comment = new Comment();

					COSDictionary parent = (COSDictionary) dict.getDictionaryObject("IRT");

					int commentPage = getCommentPage(doc, pdannotation);

					comment.setCommentCount(count);
					comment.setCommentID(dict.getNameAsString(COSName.NM));
					comment.setCommentText(pdannotation.getContents());
					comment.setCommentUser(dict.getString(COSName.T));
					comment.setCommentAction("Added a comment at page " + commentPage);

					manageParentComment(associations, dict, comment, parent);

					associations.add(comment);
					count++;

				}

			}

			return associations;

		} catch (Exception e) {
			System.err.println(e.getMessage());
			return new ArrayList<>();
		}

	}

	private static int getCommentPage(PDDocument doc, PDAnnotation pdannotation) {

		try {
			for (int p = 0; p < doc.getNumberOfPages(); ++p) {
				if (pdannotation.getPage() != null) {
					List<PDAnnotation> annotations = pdannotation.getPage().getAnnotations();
					for (PDAnnotation ann : annotations) {
						if (ann.getCOSObject() == pdannotation.getCOSObject()) {
							return p + 1;
						}
					}
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return 0;
		}

		return 0;
	}

	private static void manageParentComment(List<Comment> associations, COSDictionary dict, Comment comment,
			COSDictionary parent) {
		if (parent != null) {

			Comment parentComment = retrieveAssociacion(associations, parent.getNameAsString(COSName.NM));

			if (parentComment != null) {

				String parentAction = "";

				if (dict.getNameAsString(COSName.STATE) != null) {

					COSDictionary parentParent = (COSDictionary) parent.getDictionaryObject("IRT");

					if (parentParent != null) {
						parentAction = "Updated the status set on action " + parentComment.getCommentCount();
					} else {
						parentAction = "Set the status on comment " + parentComment.getCommentCount();
					}

				} else {
					parentAction = "Replied to " + parentComment.getCommentUser() + " on comment "
							+ parentComment.getCommentCount();
				}

				comment.setCommentAction(parentAction);
			}
		}
	}

	private static void fillExcelFile(File pdfFile, List<Comment> comments) throws InvalidFormatException, IOException {

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
