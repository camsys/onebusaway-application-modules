package org.onebusaway.twilio.actions.bookmarks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onebusaway.collections.MappingLibrary;
import org.onebusaway.presentation.model.BookmarkWithStopsBean;
import org.onebusaway.twilio.actions.Messages;
import org.onebusaway.twilio.actions.TwilioSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.ValueStack;

public class IndexAction extends TwilioSupport {

  private static Logger _log = LoggerFactory.getLogger(IndexAction.class);
  
  @Override
  public String execute() throws Exception {
    _log.debug("in execute! with input=" + getInput());
    
    
    if ("3".equals(getInput())) {
      clearNextAction();
      // TODO
      return INPUT;
    }
    
    // no input, looks for book marks
    ActionContext context = ActionContext.getContext();
    ValueStack stack = context.getValueStack();
    List<BookmarkWithStopsBean> bookmarks = (List<BookmarkWithStopsBean>) stack.findValue("bookmarks");

    if (bookmarks == null || bookmarks.isEmpty()) {
      _log.debug("no bookmarks found");
      addMessage(Messages.BOOKMARKS_EMPTY);
      addMessage(Messages.HOW_TO_GO_BACK);
      addMessage(Messages.TO_REPEAT);

    } else {
      populateBookmarks(bookmarks);
    }
    
    return INPUT;
  }

  private void populateBookmarks(List<BookmarkWithStopsBean> bookmarks) {
    int index = 1;
    for (BookmarkWithStopsBean bookmark : bookmarks) {
      _log.debug("found bookmark=" + bookmark);
      String toPress = Integer.toString(index);

      addMessage(Messages.FOR);
// TODO
//      AgiActionName stopAction = addAction(toPress,
//          "/stop/arrivalsAndDeparturesForStopId");

      List<String> stopIds = MappingLibrary.map(bookmark.getStops(), "id");
      Set<String> routeIds = new HashSet<String>(MappingLibrary.map(
          bookmark.getRoutes(), "id", String.class));
// TODO
//      stopAction.putParam("stopIds", stopIds);
//      stopAction.putParam("routeIds", routeIds);
//
//      addBookmarkDescription(bookmark);

      addMessage(Messages.PLEASE_PRESS);
      addText(toPress);

      index++;
    }

    
  }
}
