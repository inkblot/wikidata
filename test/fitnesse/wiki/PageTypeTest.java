package fitnesse.wiki;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static fitnesse.wiki.PageType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PageTypeTest extends WikiBaseTestCase {

    @Test
    public void fromString() {
        assertEquals(SUITE, PageType.fromString("Suite"));
        assertEquals(TEST, PageType.fromString(TEST.toString()));
        assertEquals(NORMAL, PageType.fromString("Normal"));

        try {
            PageType.fromString("unknown");
            fail("should have thrown an exception");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void fromSuiteWikiPage() throws Exception {
        WikiPage page = createSuitePage();
        assertEquals(SUITE, PageType.fromWikiPage(page));
    }

    private WikiPage createSuitePage() throws Exception {
        WikiPage page = createDefaultPage();
        setPageTypeAttribute(page, SUITE);
        return page;
    }

    private void setPageTypeAttribute(WikiPage page, PageType attribute) throws Exception {
        PageData pageData = page.getData();
        pageData.setAttribute(attribute.toString(), "true");
        page.commit(pageData);
    }

    private WikiPage createDefaultPage() throws Exception {
        return InMemoryPage.makeRoot("RooT", injector);
    }

    @Test
    public void fromTestWikiPage() throws Exception {
        WikiPage page = createTestPage();
        assertEquals(PageType.TEST, PageType.fromWikiPage(page));
    }

    private WikiPage createTestPage() throws Exception {
        WikiPage page = createDefaultPage();
        setPageTypeAttribute(page, TEST);
        return page;
    }

    @Test
    public void fromNormalWikiPage() throws Exception {
        WikiPage page = createDefaultPage();
        assertEquals(NORMAL, PageType.fromWikiPage(page));
    }

    private Collection<Object[]> pageTypeFromPageNameData() {
        List<Object[]> values = new ArrayList<Object[]>();

        addTestData(values, SUITE, "SuitePage");
        addTestData(values, SUITE, "PageSuite");
        addTestData(values, SUITE, "PageExamples");

        addTestData(values, TEST, "TestPage");
        addTestData(values, TEST, "PageTest");
        addTestData(values, TEST, "ExamplePage");
        addTestData(values, TEST, "PageExample");

        addTestData(values, NORMAL, "NormalPage");

        addTestData(values, NORMAL, "SuiteSetUp");
        addTestData(values, NORMAL, "SetUp");

        addTestData(values, NORMAL, "SuiteTearDown");
        addTestData(values, NORMAL, "TearDown");

        addTestData(values, NORMAL, "ExamplesNormal");

        return values;
    }

    private static void addTestData(List<Object[]> values, PageType test,
                                    String string) {
        values.add(new Object[]{test, string});
    }

    @Test
    public void pageTypeFromPageName() {
        Collection<Object[]> testData = pageTypeFromPageNameData();

        for (Object[] testItem : testData) {
            PageType pageType = (PageType) testItem[0];
            String pageName = (String) testItem[1];
            assertEquals(pageType, getPageTypeForPageName(pageName));
        }
    }

}
