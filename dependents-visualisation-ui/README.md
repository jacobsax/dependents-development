# Dependents Visualisation and UI


This service provides the front end to the analysis system. It includes a homepage (`index.html`) where a user can initiate analysis, an analysis progress page `parse-waiting-loop.html` and multiple visualisation pages.

The UI has been written in HTML and Javascript, using Bootstrap for formatting, and D3.js to render visualisations.

This visualisation can be built into a Docker container, to be served with an Nginx server. To do this, run `./build.sh`.

