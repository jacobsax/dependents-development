package org.jwu.javadepend.resources.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class ProjectInfo {

    @JsonProperty("github_repo_name")
    public String repo_name;

    @JsonProperty("git_url")
    public String gitUrl;

    @JsonProperty("pom")
    public List<PomInfo> pomInfo;

    public ProjectInfo (String repo_name, String gitUrl, PomInfo pomInfo) {
        this.repo_name = repo_name;
        this.gitUrl = gitUrl;
        this.pomInfo = new ArrayList<PomInfo>();
        this.addPom(pomInfo);
    }

    public ProjectInfo (String repo_name, String gitUrl) {
        this.repo_name = repo_name;
        this.gitUrl = gitUrl;
        this.pomInfo = new ArrayList<PomInfo>();
    }

    public void addPom(PomInfo pom) {
        this.pomInfo.add(pom);
    }

}
