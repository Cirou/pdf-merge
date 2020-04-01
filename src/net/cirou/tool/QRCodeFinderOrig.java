package net.cirou.tool;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

public class QRCodeFinderOrig {

	static float x = 0;
	static float y = 0;

	static BufferedImage image;
	static AffineTransform flipAT;
	static AffineTransform rotateAT;
	static AffineTransform transAT;
	static String filename;
	static int SCALE = 4;
	static Graphics2D g2d;
	static TextPosition texts;

	public static void main(String[] args) throws IOException {

		// Prepare 2 files to test the code
		File file = new File("./res/qrcode/H88-MA-E-2430150_A.pdf");
		File file2 = new File("./res/qrcode/H88-MA-E-2430151_A.pdf");
		File file3 = new File("./res/qrcode/H88-MA-E-2430152_A.pdf");

		// Assuming the 2 files are different revisions of the same document
		PDDocument doc = PDDocument.load(file);
		PDDocument doc2 = PDDocument.load(file2);
		PDDocument doc3 = PDDocument.load(file3);

		// This is just an example, the doc list in input should be dynamic
		List<PDDocument> docs = new ArrayList<>();
		docs.add(doc);
		// docs.add(doc2);
		// docs.add(doc3);
		addRect();
	}

	private static void addRect() throws IOException {

		// Loading an existing document
		File file = new File("./res/qrcode/H88-MA-E-2430154_A.pdf");
		PDDocument document = PDDocument.load(file);

		// Retrieving a page of the PDF Document
		PDPage page = document.getPage(0);

		PDFTextStripper tStripper = new PDFTextStripper() {

			@Override
			protected void writeString(String string, List<TextPosition> textPositions) throws IOException {

				try {
					int fromIndex = 0;
					
					String search = "{QRCODE}";

					while ((fromIndex = string.indexOf(search, fromIndex)) != -1) {
						texts = textPositions.get(fromIndex);
						break;
					}
				} catch (NullPointerException e) {
					System.out.println("CAught");
				}

			}

		};

		tStripper.setStartPage(0);
		tStripper.setEndPage(1);
		tStripper.getText(document);

		// This retrieves the correct coordinates for the text
		PDRectangle cropBox = page.getCropBox();
        float x = texts.getTextMatrix().getTranslateX() + cropBox.getLowerLeftX();
        float y = texts.getTextMatrix().getTranslateY() + cropBox.getLowerLeftY();
		
        // This must be changed accordingly to the QRCode image size
		y = (float) (y - (30 / 2)) ;
		
		System.out.println("Placeholder coordinates X: " + x + ", Y :" + y);

		// Instantiating the PDPageContentStream class
		PDPageContentStream contentStream = new PDPageContentStream(document, page, true, true, true);

		// Setting the non stroking color
		contentStream.setNonStrokingColor(Color.DARK_GRAY);

		// Drawing a rectangle
		contentStream.addRect(x, y, 50, 50);

		// Drawing a rectangle
		contentStream.fill();

		System.out.println("rectangle added");

		// Closing the ContentStream object
		contentStream.close();

		// Saving the document
		File file1 = new File("./res/qrcode/H88-MA-E-2430154_A_2.pdf");
		document.save(file1);

		// Closing the document
		document.close();
	}

}
