package org.jwu.javadepend.resources.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PackageInfo {
    @JsonProperty("group")
    public String groupId;

    @JsonProperty("artifact")
    public String artifactId;

    @JsonProperty("version")
    public String version;

    public PackageInfo (String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }
}
