// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.wiki;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static fitnesse.wiki.zip.ZipFileVersionsController.dateFormat;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class FileSystemPageZipFileVersioningTest extends WikiBaseTestCase {
    public FileSystemPage page;
    private VersionInfo firstVersion;
    private PageCrawler crawler;
    private WikiPage root;

    @Inject
    public void inject(@Named(WikiModule.ROOT_PAGE) WikiPage root) {
        this.root = root;
    }

    @Before
    public void setUp() throws Exception {
        PageVersionPruner.daysTillVersionsExpire = 1;
        assertThat(root, instanceOf(FileSystemPage.class));
        crawler = root.getPageCrawler();
        page = (FileSystemPage) crawler.addPage(root, PathParser.parse("PageOne"), "original content");

        PageData data = page.getData();
        firstVersion = page.commit(data);
    }

    @Override
    protected Properties getProperties() {
        Properties properties = super.getProperties();
        properties.remove(WikiModule.WIKI_PAGE_CLASS);
        return properties;
    }

    @Test
    public void testSave() throws Exception {
        String dirPath = page.getFileSystemPath();
        File dir = new File(dirPath);
        String[] filenames = dir.list();

        List<String> list = Arrays.asList(filenames);
        assertTrue(list.contains(firstVersion + ".zip"));
    }

    @Test
    public void testLoad() throws Exception {
        PageData data = page.getData();
        data.setContent("new content");
        VersionInfo version = page.commit(data);

        PageData loadedData = page.getDataVersion(version.getName());
        assertEquals("original content", loadedData.getContent());
    }

    @Test
    public void testGetVersions() throws Exception {
        Set<VersionInfo> versionNames = page.getData().getVersions();
        assertEquals(1, versionNames.size());
        assertTrue(versionNames.contains(firstVersion));
    }

    @Test
    public void testSubWikisDontInterfere() throws Exception {
        crawler.addPage(page, PathParser.parse("SubPage"), "sub page content");
        try {
            page.commit(page.getData());
        } catch (Exception e) {
            fail("this exception should not have been thrown: " + e.getMessage());
        }
    }

    @Test
    public void testTwoVersions() throws Exception {
        PageData data = page.getData();
        data.setContent("new content");
        VersionInfo secondVersion = page.commit(data);
        Set<VersionInfo> versionNames = page.getData().getVersions();
        assertEquals(2, versionNames.size());
        assertTrue(versionNames.contains(firstVersion));
        assertTrue(versionNames.contains(secondVersion));
    }

    @Test
    public void testVersionsExpire() throws Exception {
        PageVersionPruner.daysTillVersionsExpire = 3;
        PageData data = page.makePageData();
        Set<VersionInfo> versions = data.getVersions();
        for (VersionInfo version : versions)
            page.removeVersion(version.toString());

        data.getProperties().setLastModificationTime(dateFormat().parse("20031213000000"));
        page.makeVersion(data);
        data.getProperties().setLastModificationTime(dateFormat().parse("20031214000000"));
        page.makeVersion(data);
        data.getProperties().setLastModificationTime(dateFormat().parse("20031215000000"));
        page.makeVersion(data);
        data.getProperties().setLastModificationTime(dateFormat().parse("20031216000000"));
        page.makeVersion(data);

        versions = page.makePageData().getVersions();
        PageVersionPruner.pruneVersions(page, versions);
        versions = page.makePageData().getVersions();
        assertEquals(3, versions.size());

        List<VersionInfo> versionsList = new LinkedList<VersionInfo>(versions);
        Collections.sort(versionsList);
        assertTrue(versionsList.get(0).toString().endsWith("20031214000000"));
        assertTrue(versionsList.get(1).toString().endsWith("20031215000000"));
        assertTrue(versionsList.get(2).toString().endsWith("20031216000000"));
    }

    @Test
    public void testGetContent() throws Exception {
        WikiPagePath alpha = PathParser.parse("AlphaAlpha");
        WikiPage a = crawler.addPage(root, alpha, "a");

        PageData data = a.getData();
        assertEquals("a", data.getContent());
    }

    @Test
    public void testReplaceContent() throws Exception {
        WikiPagePath alpha = PathParser.parse("AlphaAlpha");
        WikiPage page = crawler.addPage(root, alpha, "a");

        PageData data = page.getData();
        data.setContent("b");
        page.commit(data);
        assertEquals("b", page.getData().getContent());
    }

    @Test
    public void testSetAttributes() throws Exception {
        PageData data = root.getData();
        data.setAttribute("Test", "true");
        data.setAttribute("Search", "true");
        root.commit(data);
        assertTrue(root.getData().hasAttribute("Test"));
        assertTrue(root.getData().hasAttribute("Search"));

        assertEquals("true", root.getData().getAttribute("Test"));
    }

    @Test
    public void testSimpleVersionTasks() throws Exception {
        WikiPagePath path = PathParser.parse("MyPageOne");
        WikiPage page = crawler.addPage(root, path, "old content");
        PageData data = page.getData();
        data.setContent("new content");
        VersionInfo previousVersion = page.commit(data);

        data = page.getData();
        Set<VersionInfo> versions = data.getVersions();
        assertEquals(1, versions.size());
        assertEquals(true, versions.contains(previousVersion));

        PageData loadedData = page.getDataVersion(previousVersion.getName());
        assertSame(page, loadedData.getWikiPage());
        assertEquals("old content", loadedData.getContent());
    }

    @Test
    public void testUserNameIsInVersionName() throws Exception {
        WikiPagePath testPagePath = PathParser.parse("TestPage");
        WikiPage testPage = crawler.addPage(root, testPagePath, "version1");

        PageData data = testPage.getData();
        data.setAttribute(PageData.LAST_MODIFYING_USER, "Aladdin");
        testPage.commit(data);

        data = testPage.getData();
        data.setAttribute(PageData.LAST_MODIFYING_USER, "Joe");
        VersionInfo record = testPage.commit(data);

        assertTrue(record.getName().startsWith("Aladdin"));
    }

    @Test
    public void testNoVersionException() throws Exception {
        WikiPagePath pageOnePath = PathParser.parse("PageOne");
        WikiPage page = crawler.addPage(root, pageOnePath, "old content");
        try {
            page.getDataVersion("abc");
            fail("a NoSuchVersionException should have been thrown");
        } catch (NoSuchVersionException e) {
            assertEquals("There is no version 'abc'", e.getMessage());
        }
    }

    @Test
    public void testUnicodeInVersions() throws Exception {
        WikiPage page = crawler.addPage(root, PathParser.parse("SomePage"), "\uba80\uba81\uba82\uba83");
        PageData data = page.getData();
        data.setContent("blah");
        VersionInfo info = page.commit(data);

        data = page.getDataVersion(info.getName());
        String expected = "\uba80\uba81\uba82\uba83";
        String actual = data.getContent();

        assertEquals(expected, actual);
    }

    @Test
    public void testVersionedPropertiedLoadedProperly() throws Exception {
        WikiPage page = crawler.addPage(root, PathParser.parse("TestPage"));
        PageData data = page.getData();
        WikiPageProperties oldProps = data.getProperties();
        WikiPageProperties props = new WikiPageProperties();
        props.set("MyProp", "my value");
        data.setProperties(props);
        page.commit(data);

        data.setProperties(oldProps);
        VersionInfo version = page.commit(data);

        PageData versionedData = page.getDataVersion(version.getName());
        WikiPageProperties versionedProps = versionedData.getProperties();

        assertTrue(versionedProps.has("MyProp"));
        assertEquals("my value", versionedProps.get("MyProp"));
    }

}
