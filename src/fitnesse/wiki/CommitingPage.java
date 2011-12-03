// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.wiki;

import com.google.inject.Injector;

import java.io.IOException;

public abstract class CommitingPage extends BaseWikiPage {
    private static final long serialVersionUID = 1L;

    protected CommitingPage(String name, WikiPage parent, Injector injector) {
        super(name, parent, injector);
    }

    protected abstract VersionInfo makeVersion() throws IOException;

    protected abstract void doCommit(PageData data) throws IOException;

    public VersionInfo commit(PageData data) throws IOException {
        VersionInfo previousVersion = makeVersion();
        doCommit(data);
        return previousVersion;
    }

}