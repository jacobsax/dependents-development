function dependentsSearchOnClick(formId, ownerInput, repositoryInput, validationResponse) {
    let ownerValue = document.getElementById(formId).elements.namedItem(ownerInput).value.toLowerCase();
    let repositoryValue = document.getElementById(formId).elements.namedItem(repositoryInput).value.toLowerCase();

    /**
     *  Validate inputs
     * */ 
    let validation = document.getElementById(validationResponse);

    // if the owner field is empty, flag it
    if (ownerValue == "") {
        validation.innerHTML ="Repository Owner must not be empty";
        return false;
    }

    // if the repository field is empty, flag it
    if (repositoryValue == "") {
        validation.innerHTML = "Repository Name field must not be empty";
        return false;
    }

    // check if the github url given exists, else flag the error 
    let url = `${api_url}/project/${ownerValue}/${repositoryValue}/validate`;
    var http = new XMLHttpRequest();
    http.open('HEAD', url, false);
    http.send();
    if (http.status != 200) {
        validation.innerHTML = "GitHub project entered is either private, does not exist, or is not a Java project built using Maven.";
        return false;
    }

    validation.innerHTML = "";

    // initiate searching for dependent repositories
    return fetch(`${api_url}/init/dependents-search/pom`, {
        method: "POST", 
        mode: "cors", 
        cache: "no-cache", 
        credentials: "same-origin", 
        headers: {
            "Content-Type": "application/json",
        },
        redirect: "follow", 
        referrer: "no-referrer", 
        body: JSON.stringify({ "github_short_url": `${ownerValue}/${repositoryValue}` })
    }).then(() => {
        // initiate parsing of the ast tree for this project. If it has already been parsed, it won't 
        // be parsed again in the back end
        return fetch(`${api_url}/ast/${ownerValue}/${repositoryValue}/state`).then(response => {
            if (response.status >= 200 && response.status < 300) {
            return Promise.resolve(response)
            } else {
            return Promise.reject(new Error(response.statusText))
            }
        }).then(response => {
            return response.json().then(responseJson => {
                console.log(responseJson);
                console.log(responseJson['state']);

                return responseJson['state'];
            });
        }).then(parseState => {
            console.log("parse state is " + parseState);
            // initiate parsing if it has not already completed
            if (parseState == "failed" || parseState == "not-parsed" || parseState == null) {
                console.log("initiating parsing ");

                return fetch(`${api_url}/init/ast-search/java`, {
                    method: "POST", 
                    mode: "cors", 
                    cache: "no-cache", 
                    credentials: "same-origin", 
                    headers: {
                        "Content-Type": "application/json",
                    },
                    redirect: "follow", 
                    referrer: "no-referrer", 
                    body: JSON.stringify({ "github_short_url": `${ownerValue}/${repositoryValue}`, "parsing_type": "packageclassonly"}),
                });
            } else {
                return Promise.resolve();
            }
        });
    }).then(() => {
        // redirect the user to the parse waiting loop page to retrieve analysis progress updates
        window.location.replace(`${site_url}/parse-waiting-loop.html?group=${ownerValue}&repo=${repositoryValue}`);
    });
}