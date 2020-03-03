package net.cirou.tool.bean;

public class Comment {

	private int commentCount;
	private String commentID;
	private String commentText;
	private String commentAction;
	private String commentUser;
	private int commentPage;
	private int parentCount;

	public int getParentCount() {
		return parentCount;
	}

	public void setParentCount(int parentCount) {
		this.parentCount = parentCount;
	}

	public String getCommentID() {
		return commentID;
	}

	public void setCommentID(String commentID) {
		this.commentID = commentID;
	}

	public String getCommentUser() {
		return commentUser;
	}

	public void setCommentUser(String commentUser) {
		this.commentUser = commentUser;
	}

	public int getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(int commentCount) {
		this.commentCount = commentCount;
	}

	public String getCommentAction() {
		return commentAction;
	}

	public void setCommentAction(String commentAction) {
		this.commentAction = commentAction;
	}

	public String getCommentText() {
		return commentText;
	}

	public void setCommentText(String commentText) {
		this.commentText = commentText;
	}

	public int getCommentPage() {
		return commentPage;
	}

	public void setCommentPage(int commentPage) {
		this.commentPage = commentPage;
	}

}
