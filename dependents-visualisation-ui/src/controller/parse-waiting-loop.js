
async function isProjectParsed(group, repo) {
    let url = `${api_url}/project/${group}/${repo}/dependents/state`;

    return fetch(url).then(response => {
        if (response.status >= 200 && response.status < 300) {
            return Promise.resolve(response)
        } else {
            return Promise.reject(new Error(response.statusText))
        }
    }).then(response => {
        return response.json().then(responseJson => {
            // if the project has been parsed
            if (responseJson.state == "True") {
                return Promise.resolve(true);
            } else {
                return Promise.resolve(false);
            }
        });
    }).catch(err => {
        return Promise.resolve(false);
    });
}

async function artifactsParsed(group, repo) {
    let url = `${api_url}/artifact/${group}/${repo}/dependents/state`;

    return fetch(url).then(response => {
        if (response.status >= 200 && response.status < 300) {
            return Promise.resolve(response)
        } else {
            return Promise.reject(new Error(response.statusText))
        }
    }).then(response => {
        return response.json().then(responseJson => {
            return Promise.resolve(responseJson);
        });
    }).catch(err => {
        return Promise.resolve(false);
    });
}

async function astParsed(group, repo) {
    let url = `${api_url}/ast/${group}/${repo}/state`;

    // fetch the current state of the project
    return fetch(url).then(response => {
      if (response.status >= 200 && response.status < 300) {
        return Promise.resolve(response)
      } else {
        return Promise.reject(new Error(response.statusText))
      }
    }).then(response => {
        return response.json().then(responseJson => {
            console.log(responseJson.state);

            return responseJson['state'];
        });
    }).catch(() => {
        return Promise.resolve(null);
    });
}

// fetchProgress is called to retrieve the current progress on parsing the project.
function fetchProgress(updateObjectId, group, repo) {
    isProjectParsed(group, repo).then(projectParsed => {
        if (!projectParsed) {
            document.getElementById(updateObjectId).innerHTML = "Waiting for project to be parsed."
        } else {
            document.getElementById(updateObjectId).innerHTML = "Searched base project."
            
            return artifactsParsed(group, repo).then(artifacts => {

                if (artifacts == undefined || artifacts['artifacts'] == undefined) {
                    document.getElementById(updateObjectId).innerHTML += " ";
                    document.getElementById(updateObjectId).innerHTML += `Searched 0 of 0 discovered Artifacts in Project.`;    
                    return Promise.resolve();
                }

                var completedCount = 0;

                var totalCount = artifacts['artifacts'].length;

                for (var i = 0; i < artifacts['artifacts'].length; i++) {
                    console.log(artifacts['artifacts'][i]);
                    if (artifacts['artifacts'][i]['search-state'] == "completed") {
                        completedCount += 1;
                    }
                }

                document.getElementById(updateObjectId).innerHTML += " ";
                document.getElementById(updateObjectId).innerHTML += `Searched ${completedCount} of ${totalCount} discovered Artifacts in Project.`;
       
                return astParsed(group, repo).then(astParsedState => {
                    console.log(astParsed);
                    document.getElementById(updateObjectId).innerHTML += " ";
                    
                    astParsed = false;
                    if (astParsedState == 'packageclassonly' || astParsedState == 'all') {
                        document.getElementById(updateObjectId).innerHTML += `Abstract Syntax Tree analysis completed.`;
                        astParsed = true;
                    } else if (astParsedState == 'failed') {
                        document.getElementById(updateObjectId).innerHTML += `Abstract Syntax Tree analysis failed. Some visualisations will not be available.`;
                    } else {
                        document.getElementById(updateObjectId).innerHTML += `Abstract Syntax Tree analysis in progress.`;
                    }

                    // if all artifacts have been parsed, and the ast tree
                    // of the project is parsed, redirect to the project visualisation page
                    if ((completedCount == totalCount) && astParsed) {
                        window.location.replace(`${project_page_url}?group=${group}&repo=${repo}`);
                    }
                });
            });
        }
    }).then(() => {
        setTimeout(function () {
            window.location.reload(1);
        }, 5000);
    })

   
}