package fitnesse.wikitext.test;

import fitnesse.wiki.*;
import fitnesse.wikitext.parser.SourcePage;
import fitnesse.wikitext.parser.WikiSourcePage;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WikiSourcePageTest extends WikiBaseTestCase {

    @Test
    public void getsChildren() throws Exception {
        TestRoot root = new TestRoot(InMemoryPage.makeRoot("root", injector));
        WikiPage page = root.makePage("PageOne");
        root.makePage(page, "PageTwo");
        root.makePage(page, "PageThree");
        WikiSourcePage source = new WikiSourcePage(page);
        ArrayList<String> names = new ArrayList<String>();
        for (SourcePage child : source.getChildren()) names.add(child.getName());

        assertEquals(2, names.size());
        assertTrue(names.contains("PageTwo"));
        assertTrue(names.contains("PageThree"));
    }

    @Test
    public void getsUrlForPage() throws Exception {
        WikiPage test = new TestRoot(InMemoryPage.makeRoot("root", injector)).makePage("MyPage");
        assertEquals("WikiPath", new WikiSourcePage(test).makeUrl("WikiPath"));
    }
}
