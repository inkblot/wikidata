// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.html;

import com.google.inject.Singleton;

@Singleton
public class HtmlPageFactory {
    public HtmlPage newPage() {
        return new HtmlPage();
    }

    public String toString() {
        return getClass().getName();
    }
}