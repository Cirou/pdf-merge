package net.cirou.tool;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import net.cirou.tool.bean.Comment;

public class AutoMerge {

	public static void main(String[] args) throws IOException, InvalidFormatException {

		// Prepare files to test the code
		File file = new File("./res/doc_signed_2.pdf");
		File file2 = new File("./res/doc_signed_3.pdf");
//		File file3 = new File("./res/doc_c.pdf");

		// Assuming the files are different revisions of the same document
		PDDocument doc = PDDocument.load(file);
		PDDocument doc2 = PDDocument.load(file2);
//		PDDocument doc3 = PDDocument.load(file3);

		// This is just an example, the doc list in input should be dynamic
		List<PDDocument> docs = new ArrayList<>();
		List<Comment> associations = new ArrayList<>();
		docs.add(doc);
		docs.add(doc2);
//		docs.add(doc3);
		mergeDocComments(docs, associations);
		HistoryGenerator.writeLogFile(new File("./res/out/doc_merge.pdf"), associations);
		
	}

	private static void mergeDocComments(List<PDDocument> docs, List<Comment> associations) throws IOException {
		if (!docs.isEmpty()) {

			// we assume that every document has the same number of pages
			// otherwise we can't auto-merge comments
			int numberOfPages = docs.get(0).getNumberOfPages();

			// we collect the comments of every revision grouped by the page index
			Map<Integer, List<PDAnnotation>> annotationsPerPage = new HashMap<>();

			for (PDDocument doc : docs) {
				if(doc.getNumberOfPages() == numberOfPages) {
					getAllComments(annotationsPerPage, numberOfPages, doc, associations);
				} else {
					discardDocument(associations, doc);
				}
			}

			// it’s not important what document we take, because the only difference between
			// documents is the comments in them. doc[0] will be original file without any comment
			PDDocument doc = docs.get(0);
			for (int i = 0; i < numberOfPages; i++) {
				doc.getPage(i).setAnnotations(annotationsPerPage.get(i));
			}

			// we save the new file with all the comments merged
			doc.save("./res/out/doc_merge.pdf");
			
		}
	}

	private static void discardDocument(List<Comment> associations, PDDocument doc) {
		Comment comment = new Comment();
		comment.setCommentCount(0);
		comment.setCommentID("00");
		comment.setCommentText("");
		comment.setCommentUser("Comments-Merge Tool");
		comment.setCommentAction("Document reviewed by " + doc.getDocumentInformation().getAuthor() + " discarded: different number of pages.");
		associations.add(comment);
	}

	private static void getAllComments(Map<Integer, List<PDAnnotation>> commentsPerPage, int numberOfPages,
			PDDocument doc, List<Comment> associations) throws IOException {
		// for each document we cycle the pages to collect the comments
		for (int i = 0; i < numberOfPages; i++) {
			PDPage page = doc.getPage(i);
			if (page != null && page.getAnnotations() != null) {
				managePageComments(commentsPerPage, doc, associations, i, page);
			}
		}

	}

	private static void managePageComments(Map<Integer, List<PDAnnotation>> commentsPerPage, PDDocument doc,
			List<Comment> associations, int i, PDPage page) throws IOException {
		List<PDAnnotation> comments = commentsPerPage.get(i);
		if (comments == null) {
			comments = new ArrayList<>();
		}

		int count = 1;
		for (PDAnnotation pdannotation : page.getAnnotations()) {

			COSDictionary dict = pdannotation.getCOSObject();

			if (dict.containsKey(COSName.CONTENTS)) {
				
				
				extractTextFromCommentedArea(page, pdannotation);
	            
				//if comment has content, it must be kept
				comments.add(pdannotation);
				commentsPerPage.put(i, comments);
				manageCommentWithText(doc, associations, count, pdannotation, dict);
			} else {
				if (isSignature(pdannotation)) {
					manageElectronicSignratures(doc, associations, count, pdannotation, dict);
				} else { //if it's not a signature, the comment must be kept
					comments.add(pdannotation);
					commentsPerPage.put(i, comments);
				}
			}
			count++;
			
		}
	}

	private static void extractTextFromCommentedArea(PDPage page, PDAnnotation pdannotation) throws IOException {
		PDFTextStripperByArea stripper = new PDFTextStripperByArea();
		stripper.setSortByPosition(true);
		
		PDRectangle rect = pdannotation.getRectangle();
		float x = rect.getLowerLeftX();
		float y = rect.getUpperRightY();
		float width = rect.getWidth();
		float height = rect.getHeight();
		int rotation = page.getRotation();
		if (rotation == 0) {
		    PDRectangle pageSize = page.getMediaBox();
		    y = pageSize.getHeight() - y;
		}
		Rectangle2D.Float awtRect = new Rectangle2D.Float(x, y, width, height);
		stripper.addRegion(Integer.toString(0), awtRect);
		stripper.extractRegions(page);
		System.out.println("Getting text from region = " + awtRect + "\n");
		System.out.println(stripper.getTextForRegion(Integer.toString(0)));
        System.out.println("Getting text from comment = " + pdannotation.getContents());
	}

	private static void manageCommentWithText(PDDocument doc, List<Comment> associations, int count,
			PDAnnotation pdannotation, COSDictionary dict) {
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
	}

	private static void manageElectronicSignratures(PDDocument doc, List<Comment> associations, int count,
			PDAnnotation pdannotation, COSDictionary dict) {
		//if annotation is a signature, it must be discarded, but we need to keep track on the log file
		Comment comment = new Comment();
		int commentPage = getCommentPage(doc, pdannotation);
		comment.setCommentCount(count);
		comment.setCommentID(dict.getNameAsString(COSName.NM));
		comment.setCommentText(((COSName)dict.getDictionaryObject(COSName.FT)).getName());
		comment.setCommentUser("Comments-Merge Tool");
		comment.setCommentAction("Signature deleted at page " + commentPage);
		associations.add(comment);
	}

	private static boolean isSignature(PDAnnotation annot) {
		COSBase ft = annot.getCOSObject().getDictionaryObject(COSName.FT);
		return ft instanceof COSName;
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

	private static Comment retrieveAssociacion(List<Comment> associations, String id) {
		for (Comment association : associations) {
			if (association.getCommentID() != null && association.getCommentID().contentEquals(id)) {
				return association;
			}
		}
		return null;
	}
}
