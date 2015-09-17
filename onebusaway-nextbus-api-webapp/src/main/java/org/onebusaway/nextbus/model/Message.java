package org.onebusaway.nextbus.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("message")
public class Message {
	
	@XStreamAsAttribute 
	private String id;
	
	@XStreamAsAttribute 
	private String creator;
	
	@XStreamAsAttribute 
	private boolean sendToBuses;
	
	@XStreamAsAttribute 
	private long  startBoundary;
	
	@XStreamAsAttribute 
	private long endBoundary;

	@XStreamAsAttribute 
	private String  startBoundaryStr;
	
	@XStreamAsAttribute 
	private String endBoundaryStr;
	
	private MessageInterval messageInterval;
	
	@XStreamAlias("phonemeText")
	private MessageText phoneMeText;
	
	@XStreamAlias("text")
	private MessageText messageText;
	
	@XStreamAlias("textSecondaryLanguage")
	private MessageText textSecondaryLanguage;

	public MessageInterval getMessageInterval() {
		return messageInterval;
	}

	public void setMessageInterval(MessageInterval messageInterval) {
		this.messageInterval = messageInterval;
	}

	public MessageText getPhoneMeText() {
		return phoneMeText;
	}

	public void setPhoneMeText(MessageText phoneMeText) {
		this.phoneMeText = phoneMeText;
	}

	public MessageText getTextSecondaryLanguage() {
		return textSecondaryLanguage;
	}

	public void setTextSecondaryLanguage(MessageText textSecondaryLanguage) {
		this.textSecondaryLanguage = textSecondaryLanguage;
	}

	public MessageText getMessageText() {
		return messageText;
	}

	public void setMessageText(MessageText messageText) {
		this.messageText = messageText;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public boolean isSendToBuses() {
		return sendToBuses;
	}

	public void setSendToBuses(boolean sendToBuses) {
		this.sendToBuses = sendToBuses;
	}

	public long getStartBoundary() {
		return startBoundary;
	}

	public void setStartBoundary(long startBoundary) {
		this.startBoundary = startBoundary;
	}

	public long getEndBoundary() {
		return endBoundary;
	}

	public void setEndBoundary(long endBoundary) {
		this.endBoundary = endBoundary;
	}

	public String getStartBoundaryStr() {
		return startBoundaryStr;
	}

	public void setStartBoundaryStr(String startBoundaryStr) {
		this.startBoundaryStr = startBoundaryStr;
	}

	public String getEndBoundaryStr() {
		return endBoundaryStr;
	}

	public void setEndBoundaryStr(String endBoundaryStr) {
		this.endBoundaryStr = endBoundaryStr;
	}

}
