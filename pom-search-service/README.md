<!--
This work is Copyright 2019 Jacob Unwin, and released under the MIT license.

Original based on HelloWorld Grizzly2 Application Copyright 2016 Janus Friis Nielsen, from https://github.com/janusdn/jersey-server-grizzly2

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# POM Search Service

This is a horizontally scalable Dependent Finder Service is responsible for the searching of open source repositories for dependents. It provides a REST API that can handle requests to parse pom.xml files to identify dependencies and produced artifacts; as well a search Github or a MySQL cache to identify Maven repositories which specify dependencies on a specified Artifact.

This component is written in Java, and uses the Grizzly2 package to serve a REST API, and the MavenXpp3Reader package to parse pom.xml files.

To build and run this component for development purposes, use Maven - run 'maven build'. A HTTP server will be started on localhost:8082. This server requires a number of configuration variables - specified in environment variables. To set up these environment variables for development purposes, run 'cp config.sh.template config.sh' and then modify config.sh to populate the missing variables. Finally, run 'source config.sh'. The API for this server is documented as a [Postman collection](./pom-search-service.postman_collection.json).

To build this component into a docker container, run './build.sh'. The configuration variables defined in 'config.sh' must be injected into this container.

This service is capable of searching for dependent repositories in a MySQL Cache of pom.xml files. This is a MySQL database which contains a data dump of all pom.xml files hosted on GitHub, and metadata regarding their respective repositories. MySQL syntax can be used to search for strings inside the pom.xml files. As this database is read only, it can be replicated to improve performance. The reasoning for this database is further discussed in the report associated with this project.
 