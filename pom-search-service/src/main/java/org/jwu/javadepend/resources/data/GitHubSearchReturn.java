package org.jwu.javadepend.resources.data;

import javax.xml.bind.annotation.*;

import java.util.Collection;

/**
 * Encodes some of the information returned from
 * Github's /searc
 */
@XmlRootElement
public class GitHubSearchReturn {
    public int total_count;
    public boolean incomplete_results;

    public Collection<SearchItem> items;

    public class SearchItem {
        public String name;
        public String path;
        public String gitUrl;

        public Repository repository;
    }

    public class Repository {
        public String name;
        public String html_url;

        public Owner owner;
    }

    public class Owner {
        public String login;
    }
}

