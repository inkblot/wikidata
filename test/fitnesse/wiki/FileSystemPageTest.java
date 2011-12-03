// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.

package fitnesse.wiki;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.junit.Before;
import org.junit.Test;
import util.Clock;
import util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class FileSystemPageTest extends WikiBaseTestCase {
    private PageCrawler crawler;
    private static List<String> cmMethodCalls = new ArrayList<String>();

    private Clock clock;
    private WikiPage root;
    private WikiPageFactory wikiPageFactory;

    @Override
    protected Properties getProperties() {
        Properties properties = super.getProperties();
        properties.remove(WikiModule.WIKI_PAGE_CLASS);
        return properties;
    }

    @Inject
    public void inject(Clock clock, @Named(WikiModule.ROOT_PAGE) WikiPage root, WikiPageFactory wikiPageFactory) {
        this.clock = clock;
        this.root = root;
        this.wikiPageFactory = wikiPageFactory;
    }

    @Before
    public void setUp() throws Exception {
        cmMethodCalls.clear();
        assertThat(root, instanceOf(FileSystemPage.class));
        crawler = root.getPageCrawler();
    }

    @Test
    public void testCreateBase() throws Exception {
        FileSystemPage levelA = (FileSystemPage) crawler.addPage(root, PathParser.parse("PageA"), "");
        assertEquals(getRootPath() + "/RooT/PageA", levelA.getFileSystemPath());
        assertTrue(new File(getRootPath() + "/RooT/PageA").exists());
    }

    @Test
    public void testTwoLevel() throws Exception {
        WikiPage levelA = crawler.addPage(root, PathParser.parse("PageA"));
        crawler.addPage(levelA, PathParser.parse("PageB"));
        assertTrue(new File(getRootPath() + "/RooT/PageA/PageB").exists());
    }

    @Test
    public void testContent() throws Exception {
        WikiPagePath rootPath = PathParser.parse("root");
        assertEquals("", crawler.getPage(root, rootPath).getData().getContent());
        crawler.addPage(root, PathParser.parse("AaAa"), "A content");
        assertEquals("A content", root.getChildPage("AaAa").getData().getContent());
        WikiPagePath bPath = PathParser.parse("AaAa.BbBb");
        crawler.addPage(root, bPath, "B content");
        assertEquals("B content", crawler.getPage(root, bPath).getData().getContent());
    }

    @Test
    public void testBigContent() throws Exception {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < 1000; i++)
            buffer.append("abcdefghijklmnopqrstuvwxyz");
        crawler.addPage(root, PathParser.parse("BigPage"), buffer.toString());
        String content = root.getChildPage("BigPage").getData().getContent();
        assertTrue(buffer.toString().equals(content));
    }

    @Test
    public void testPageExists() throws Exception {
        crawler.addPage(root, PathParser.parse("AaAa"), "A content");
        assertTrue(root.hasChildPage("AaAa"));
    }

    @Test
    public void testGetChildren() throws Exception {
        crawler.addPage(root, PathParser.parse("AaAa"), "A content");
        crawler.addPage(root, PathParser.parse("BbBb"), "B content");
        crawler.addPage(root, PathParser.parse("CcCc"), "C content");
        new File(new File(getRootPath(), "root"), "someOtherDir").mkdir();
        List<WikiPage> children = root.getChildren();
        assertEquals(3, children.size());
        for (WikiPage child : children) {
            String name = child.getName();
            boolean isOk = "AaAa".equals(name) || "BbBb".equals(name) || "CcCc".equals(name);
            assertTrue("WikiPAge is not a valid one: " + name, isOk);
        }
    }

    @Test
    public void testRemovePage() throws Exception {
        WikiPage levelOne = crawler.addPage(root, PathParser.parse("LevelOne"));
        crawler.addPage(levelOne, PathParser.parse("LevelTwo"));
        levelOne.removeChildPage("LevelTwo");
        File fileOne = new File(getRootPath() + "/RooT/LevelOne");
        File fileTwo = new File(getRootPath() + "/RooT/LevelOne/LevelTwo");
        assertTrue(fileOne.exists());
        assertFalse(fileTwo.exists());
    }

    @Test
    public void testDelTree() throws Exception {
        WikiPage levelOne = crawler.addPage(root, PathParser.parse("LevelOne"));
        crawler.addPage(levelOne, PathParser.parse("LevelTwo"));
        File childOne = new File(getRootPath() + "/RooT/LevelOne");
        File childTwo = new File(getRootPath() + "/RooT/LevelOne/LevelTwo");
        assertTrue(childOne.exists());
        root.removeChildPage("LevelOne");
        assertFalse(childTwo.exists());
        assertFalse(childOne.exists());
    }

    @Test
    public void testDefaultAttributes() throws Exception {
        WikiPage page = crawler.addPage(root, PathParser.parse("PageOne"), "something");
        assertTrue(page.getData().hasAttribute("Edit"));
        assertTrue(page.getData().hasAttribute("Search"));
        assertFalse(page.getData().hasAttribute("Test"));
        assertFalse(page.getData().hasAttribute("TestSuite"));
    }

    @Test
    public void testPersistentAttributes() throws Exception {
        crawler.addPage(root, PathParser.parse("FrontPage"));
        WikiPage createdPage = root.getChildPage("FrontPage");
        PageData data = createdPage.getData();
        data.setAttribute("Test", "true");
        data.setAttribute("Search", "true");
        createdPage.commit(data);
        assertTrue(data.hasAttribute("Test"));
        assertTrue(data.hasAttribute("Search"));
        WikiPage page = root.getChildPage("FrontPage");
        assertTrue(page.getData().hasAttribute("Test"));
        assertTrue(page.getData().hasAttribute("Search"));
    }

    @Test
    public void testCachedInfo() throws Exception {
        WikiPage page1 = crawler.addPage(root, PathParser.parse("PageOne"), "page one");
        WikiPage child1 = crawler.addPage(page1, PathParser.parse("ChildOne"), "child one");
        WikiPage child = page1.getChildPage("ChildOne");
        assertSame(child1, child);
    }

    @Test
    public void testCanFindExistingPages() throws Exception {
        crawler.addPage(root, PathParser.parse("FrontPage"), "front page");
        WikiPage newRoot = wikiPageFactory.makeRootPage();
        assertThat(newRoot, instanceOf(FileSystemPage.class));
        assertNotNull(newRoot.getChildPage("FrontPage"));
    }

    @Test
    public void testGetPath() throws Exception {
        assertThat(root, instanceOf(FileSystemPage.class));
        FileSystemPage fsRoot = (FileSystemPage) root;
        assertEquals(getRootPath() + "/RooT", fsRoot.getFileSystemPath());
    }

    @Test
    public void testLastModifiedTime() throws Exception {
        WikiPage page = crawler.addPage(root, PathParser.parse("SomePage"), "some text");
        page.commit(page.getData());
        long now = clock.currentClockTimeInMillis();
        Date lastModified = page.getData().getProperties().getLastModificationTime();
        assertTrue(now - lastModified.getTime() <= 5000);
    }

    @Test
    public void testUnicodeCharacters() throws Exception {
        WikiPage page = crawler.addPage(root, PathParser.parse("SomePage"), "\uba80\uba81\uba82\uba83");
        PageData data = page.getData();
        assertEquals("\uba80\uba81\uba82\uba83", data.getContent());
    }

    @Test
    public void testLoadChildrenWhenPageIsDeletedManualy() throws Exception {
        WikiPage page = crawler.addPage(root, PathParser.parse("TestPage"));
        page.getChildren();
        FileUtil.deleteFileSystemDirectory(((FileSystemPage) page).getFileSystemPath());
        try {
            page.getChildren();
        } catch (Exception e) {
            fail("No Exception should be thrown");
        }
    }

    public static void cmUpdate(String file, String payload) {
        cmMethodCalls.add(String.format("update %s|%s", file, payload));
    }

    public static void cmEdit(String file, String payload) {
        cmMethodCalls.add(String.format("edit %s|%s", file, payload));
    }

    public static void cmDelete(String file, String payload) {
        cmMethodCalls.add(String.format("delete %s|%s", file, payload));
    }

    public static void cmPreDelete(String file, String payload) {
        cmMethodCalls.add(String.format("preDelete %s|%s", file, payload));
    }

    @Test
    public void cmPluginNotCalledIfBlank() throws Exception {
        WikiPage page = crawler.addPage(root, PathParser.parse("TestPage"), "!define CM_SYSTEM {}");
        cmMethodCalls.clear();
        page.addChildPage("CreatedPage");
        assertEquals(0, cmMethodCalls.size());
    }

    @Test
    public void cmPluginCalledForCreate() throws Exception {
        WikiPage page = crawler.addPage(root, PathParser.parse("TestPage"), "!define CM_SYSTEM {fitnesse.wiki.FileSystemPageTest xxx}");
        cmMethodCalls.clear();
        page.addChildPage("CreatedPage");
        assertEquals(1, cmMethodCalls.size());
        assertEquals("update " + getRootPath() + "/RooT/TestPage/CreatedPage|fitnesse.wiki.FileSystemPageTest xxx", cmMethodCalls.get(0));
    }

    @Test
    public void cmPluginCalledIfNoPayload() throws Exception {
        WikiPage page = crawler.addPage(root, PathParser.parse("TestPage"), "!define CM_SYSTEM {fitnesse.wiki.FileSystemPageTest}");
        cmMethodCalls.clear();
        page.addChildPage("CreatedPage");
        assertEquals("update " + getRootPath() + "/RooT/TestPage/CreatedPage|fitnesse.wiki.FileSystemPageTest", cmMethodCalls.get(0));
        assertEquals(1, cmMethodCalls.size());
    }

    @Test
    public void cmPluginEditAndUpdateCalledForReWrite() throws Exception {
        WikiPage page = crawler.addPage(root, PathParser.parse("TestPage"), "!define CM_SYSTEM {fitnesse.wiki.FileSystemPageTest xxx}");
        cmMethodCalls.clear();
        page.commit(page.getData());
        assertEquals(4, cmMethodCalls.size());
        assertEquals("edit " + getRootPath() + "/RooT/TestPage/content.txt|fitnesse.wiki.FileSystemPageTest xxx", cmMethodCalls.get(0));
        assertEquals("update " + getRootPath() + "/RooT/TestPage/content.txt|fitnesse.wiki.FileSystemPageTest xxx", cmMethodCalls.get(1));
        assertEquals("edit " + getRootPath() + "/RooT/TestPage/properties.xml|fitnesse.wiki.FileSystemPageTest xxx", cmMethodCalls.get(2));
        assertEquals("update " + getRootPath() + "/RooT/TestPage/properties.xml|fitnesse.wiki.FileSystemPageTest xxx", cmMethodCalls.get(3));
    }

    @Test
    public void cmPluginEditNotCalledIfNewPage() throws Exception {
        WikiPage page = crawler.addPage(root, PathParser.parse("TestPage"), "!define CM_SYSTEM {fitnesse.wiki.FileSystemPageTest xxx}");
        cmMethodCalls.clear();
        crawler.addPage(page, PathParser.parse("NewPage"), "raw content");
        assertEquals("update " + getRootPath() + "/RooT/TestPage/NewPage|fitnesse.wiki.FileSystemPageTest xxx", cmMethodCalls.get(0));
        assertEquals("update " + getRootPath() + "/RooT/TestPage/NewPage/content.txt|fitnesse.wiki.FileSystemPageTest xxx", cmMethodCalls.get(1));
        assertEquals("update " + getRootPath() + "/RooT/TestPage/NewPage/properties.xml|fitnesse.wiki.FileSystemPageTest xxx", cmMethodCalls.get(2));
        assertEquals(3, cmMethodCalls.size());
    }

    @Test
    public void cmPluginCalledForDelete() throws Exception {
        WikiPage page = crawler.addPage(root, PathParser.parse("TestPage"), "!define CM_SYSTEM {fitnesse.wiki.FileSystemPageTest xxx}");
        page.addChildPage("CreatedPage");
        cmMethodCalls.clear();
        page.removeChildPage("CreatedPage");
        assertEquals(2, cmMethodCalls.size());
        assertEquals("preDelete " + getRootPath() + "/RooT/TestPage/CreatedPage|fitnesse.wiki.FileSystemPageTest xxx", cmMethodCalls.get(0));
        assertEquals("delete " + getRootPath() + "/RooT/TestPage/CreatedPage|fitnesse.wiki.FileSystemPageTest xxx", cmMethodCalls.get(1));
    }
}
