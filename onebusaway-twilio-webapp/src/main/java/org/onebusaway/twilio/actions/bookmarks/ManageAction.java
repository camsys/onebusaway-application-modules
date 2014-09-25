package org.onebusaway.twilio.actions.bookmarks;

import java.util.List;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.presentation.model.BookmarkWithStopsBean;
import org.onebusaway.presentation.services.BookmarkPresentationService;
import org.onebusaway.twilio.actions.Messages;
import org.onebusaway.twilio.actions.TwilioSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.ValueStack;

@Results({
  @Result(name="delete-bookmark", type="chain",
      params={"namespace", "/bookmarks", "actionName", "delete-bookmark"}),
      @Result(name="home", type="redirectAction",
      params={"namespace", "/", "actionName", "index"})
})
public class ManageAction extends AbstractBookmarkAction {
  
  private static Logger _log = LoggerFactory.getLogger(ManageAction.class);

  @Override
  public String execute() throws Exception {
    _log.debug("in execute! with input=" + getInput());
    setNextAction("bookmarks/manage");
    if (getInput() != null) {
      
      if (PREVIOUS_MENU_ITEM.equals(getInput())) {
        clearNextAction();
        return "home";
      } else if (REPEAT_MENU_ITEM.equals(getInput())) {
        listBookmarks();
        return INPUT;
      }
      
      setSelection();
      if (isValidSelection()) {
        clearNextAction();
        return "delete-bookmark";
      }

			// validation failed, retry
			listBookmarks();
      return INPUT;
    }

    // no input, list bookmarks to delete
    listBookmarks();
    return INPUT;
  }


  private void listBookmarks() {
    setNextAction("bookmarks/manage");
    List<BookmarkWithStopsBean> bookmarks = _bookmarkPresentationService.getBookmarksWithStops(_currentUser.getBookmarks());

    if (bookmarks == null || bookmarks.isEmpty()) {
      addMessage(Messages.BOOKMARKS_EMPTY);
      addMessage(Messages.HOW_TO_GO_BACK);
      addMessage(Messages.TO_REPEAT);
    } else {
      populateBookmarks(bookmarks, Messages.BOOKMARKS_TO_DELETE_THE_BOOKMARK_FOR);
    }

  }


}
