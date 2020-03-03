package net.cirou.tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

public class AutoMerge {

	public static void main(String[] args) throws IOException {

		// Prepare 2 files to test the code
		File file = new File("./res/doc_a.pdf");
		File file2 = new File("./res/doc_b.pdf");
		File file3 = new File("./res/doc_c.pdf");

		// Assuming the 2 files are different revisions of the same document
		PDDocument doc = PDDocument.load(file);
		PDDocument doc2 = PDDocument.load(file2);
		PDDocument doc3 = PDDocument.load(file3);

		// This is just an example, the doc list in input should be dynamic
		List<PDDocument> docs = new ArrayList<>();
		docs.add(doc);
		docs.add(doc2);
		docs.add(doc3);
		mergeDocComments(docs);
	}

	private static void mergeDocComments(List<PDDocument> docs) throws IOException {
		if (!docs.isEmpty()) {

			// we assume that every document has the same number of pages
			// otherwise we can't auto-merge comments
			int numberOfPages = docs.get(0).getNumberOfPages();

			// we collect the comments of every revision grouped by the page index
			Map<Integer, List<PDAnnotation>> annotationsPerPage = new HashMap<>();

			for (PDDocument doc : docs) {
				getAllComments(annotationsPerPage, numberOfPages, doc);
			}

			// it’s not important what document we take, because the only difference between
			// documents is the comments in them
			PDDocument doc = docs.get(0);
			for (int i = 0; i < numberOfPages; i++) {
				doc.getPage(i).setAnnotations(annotationsPerPage.get(i));
			}

			// we save the new file with all the comments merged
			doc.save("./res/out/doc_merge.pdf");
		}
	}

	private static void getAllComments(Map<Integer, List<PDAnnotation>> commentsPerPage, int numberOfPages,
			PDDocument doc) throws IOException {
		// for each document we cycle the pages to collect the comments
		for (int i = 0; i < numberOfPages; i++) {
			PDPage page = doc.getPage(i);
			if (page != null && page.getAnnotations() != null) {
				List<PDAnnotation> comments = commentsPerPage.get(i);
				if (comments == null) {
					comments = new ArrayList<>();
				}
				
				// filtrare le firme "FT: Sig"
				comments.addAll(page.getAnnotations());
				
				commentsPerPage.put(i, comments);
			}
		}
	}

}
