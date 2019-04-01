package org.jwu.javadepend.resources.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class PomInfo {


    @JsonProperty("packages_identified")
    public boolean packagesIdentified;

    @JsonProperty("dependencies_identified")
    public boolean dependenciesIdentified;

    @JsonProperty("artifacts")
    public List<PackageInfo> packageInfo;

    @JsonProperty("dependencies")
    public List<PackageInfo> dependencies;

    public PomInfo() {

        this.packagesIdentified = false;
        this.dependenciesIdentified = false;
        this.packageInfo = new ArrayList();
        this.dependencies = new ArrayList();
    }

    public void addPackage(PackageInfo singlePackage){
        this.packageInfo.add(singlePackage);
        this.packagesIdentified = true;
    }

    public void addDependency(PackageInfo singlePackage){
        this.dependencies.add(singlePackage);
        this.dependenciesIdentified = true;
    }

}
