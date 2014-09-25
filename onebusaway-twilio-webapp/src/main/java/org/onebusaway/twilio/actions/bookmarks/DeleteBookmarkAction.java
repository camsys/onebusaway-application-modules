package org.onebusaway.twilio.actions.bookmarks;

import java.util.Map;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.twilio.actions.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Results({
  @Result(name="index", type="redirectAction",
      params={"namespace", "/bookmarks", "actionName", "index"})
})
public class DeleteBookmarkAction extends AbstractBookmarkAction {
  
  private static Logger _log = LoggerFactory.getLogger(DeleteBookmarkAction.class);
  private int _index = -1; // default to an invalid index

  public void setIndex(int index) {
    _index = index;
  }

  @Override
  public String execute() throws Exception {
    
    Integer navState = (Integer)sessionMap.get("navState");
    if (navState == null) {
      navState = DISPLAY_DATA_NAV;
    }

    if (navState == DISPLAY_DATA_NAV) {
      
      int bookmarkSize = _currentUser.getBookmarks().size();
      int bookmarkId = _currentUser.getBookmarks().get(_index).getId();
      _log.info("deleting index=" + _index + " of " + bookmarkSize + " with id=" + bookmarkId);
      _currentUser.getBookmarks().remove(_index);
      _currentUserService.deleteStopBookmarks(bookmarkId);
      int updatedBookmarkSize = _currentUser.getBookmarks().size();
      _log.debug("updatedBookmarkSize=" + updatedBookmarkSize);
      if (bookmarkSize == updatedBookmarkSize) {
				// if this didn't throw an exception, there probably was nothing to delete
        addMessage(Messages.BOOKMARKS_EMPTY);
      } else {
        addMessage(Messages.BOOKMARK_DELETED);
      }
      sessionMap.put("navState", DO_ROUTING_NAV);
      setNextAction("bookmarks/delete-bookmark");
      return INPUT;
     }
    
    // any input or DO_ROUTING redirects us to the index
    _log.debug("redirecting to bookmark index");
    clearNextAction();
    clearNavState();
    return "index";
  }


}
