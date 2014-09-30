package org.onebusaway.twilio.actions.bookmarks;

import java.util.List;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.presentation.model.BookmarkWithStopsBean;
import org.onebusaway.twilio.actions.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Results({
  @Result(name="back", type="redirectAction", params={"namespace", "/", "actionName", "index"}),
  @Result(name="arrival-and-departure-for-stop-id", type="chain",
      params={"namespace", "/stops", "actionName", "arrivals-and-departures-for-stop-id"})
})
public class IndexAction extends AbstractBookmarkAction {

  private static Logger _log = LoggerFactory.getLogger(IndexAction.class);
  
  @Override
  public String execute() throws Exception {
    _log.debug("in bookmark execute! with input=" + getInput());
    
    // Check for "back" action
    if (PREVIOUS_MENU_ITEM.equals(getInput())) {
      return "back";
    }

    
    // if we have input (other than "back"), assume its the index of the bookmark
    if (getInput() != null) {
      clearNextAction();
      setSelection();
      return "arrival-and-departure-for-stop-id";
    }
    
    // no input, look for bookmarks
    List<BookmarkWithStopsBean> bookmarks = _bookmarkPresentationService.getBookmarksWithStops(_currentUser.getBookmarks());
    logUserInteraction();


    if (bookmarks == null || bookmarks.isEmpty()) {
      addMessage(Messages.BOOKMARKS_EMPTY);
      addMessage(Messages.HOW_TO_GO_BACK);
      addMessage(Messages.TO_REPEAT);
    } else {
      populateBookmarks(bookmarks, Messages.FOR);
    }
    
    setNextAction("bookmarks/index");
    return INPUT;
  }

}
