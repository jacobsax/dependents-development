
/**
 * parseASTOfProject triggers the Abstract Syntax tree parsing for 
 * call graph generation of a project.
 * @param {*} repoUrl 
 */
function parseASTOfProject(repoUrl) {
  // post a request to initiate the parsing of the project
  fetch(`${api_url}/init/ast-search/java`, {
    method: "POST", // *GET, POST, PUT, DELETE, etc.
    mode: "cors", // no-cors, cors, *same-origin
    cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
    credentials: "same-origin", // include, *same-origin, omit
    headers: {
        "Content-Type": "application/json",
    },
    redirect: "follow", // manual, *follow, error
    referrer: "no-referrer", // no-referrer, *client
    body: JSON.stringify({ "github_short_url": repoUrl, "parsing_type": "all"}), // body data type must match "Content-Type" header
  }).then(() => {
    window.location.reload(1);
  });
}

/**
 * retrieveProjectASTState retrieves the state of Call Graph Generation (AST parsing)
 * for newly visible table rows, and then updates the innerHtml for those rows to describe
 * the current parsing state
 * @param {*} tableCell 
 * @param {*} repoUrl 
 */
function retrieveProjectASTState(tableCell, repoUrl) {
  let url = `${api_url}/ast/${repoUrl}/state`;

  // fetch the current state of the project
  fetch(url).then(response => {
    if (response.status >= 200 && response.status < 300) {
      return Promise.resolve(response)
    } else {
      return Promise.reject(new Error(response.statusText))
    }
  }).then(response => {
      response.json().then(responseJson => {
        // response state is the state of the AST tree parsing for this project. It 
        // can be 'in-progress', 'all', 'packageclassonly', 'queued' and 'not-parsed'
        if (responseJson['state'] == 'packageclassonly' || responseJson['state'] == 'all') {
          tableCell.innerHTML = "Generated";
        } else if (responseJson['state'] == 'queued') {
          tableCell.innerHTML = "Queued";
        } else if (responseJson['state'] == 'in-progress') {
          tableCell.innerHTML = "In Progress";
        } else if (responseJson['state'] == 'not-parsed'){
          // if the project hasn't been parsed, add a button for the user to click to initiate parsing
          tableCell.innerHTML = `<a href="#" onclick="parseASTOfProject('${repoUrl}');">Initiate Generation</a>`;
        } else if (responseJson['state'] == 'failed'){
          // if parsing has failed, add a button for the user to click to re-initiate parsing
          tableCell.innerHTML = `Failed. <a href="#" onclick="parseASTOfProject('${repoUrl}');">Retry</a>`;
        }
      });
  }).catch(err => {
    document.getElementById(tableId).innerHTML = "<p>Error: Could not fetch dependents of project.</p>"
  });
}

function retrieveTransitiveDependentStats(tableCell, repoUrl) {
  let url = `${api_url}/project/${repoUrl}/dependents`;
  fetch(url).then(response => {
    if (response.status >= 200 && response.status < 300) {
      return Promise.resolve(response)
    } else {
      return Promise.reject(new Error(response.statusText))
    }
  }).then(response => {
      response.json().then(responseJson => {
        if (responseJson != undefined && responseJson != null) {
          tableCell.innerHTML = `${responseJson['projects-search']['count']}`;
        }
      });
  });
}

/**
 * dependentsTableScroll is intended to be used with the dependents table, to be executed on scroll of the table (this is set up in the html template). 
 * It iterates over every row in the table, and determines whether the row is currently visible to the user. When a row has become visible, 
 * this information is stored to localstorage. If the row is visible, and has never been visible to the user before, the missing fields for 
 * the row (dependent count, AST tree parsing info) are loaded. 
 * 
 * 
 * @param {string} tableId 
 */
function dependentsTableScroll(tableId) {
  // determine the bounding co-ordinates for the table of dependents
  let rect = document.getElementById("dependents-table-wrapper").getBoundingClientRect();

  // iterate over every row in the table and determine whether the row is currently visible.
  for (let i = 1; i < document.getElementById(tableId).rows.length; i++){
    // determine the bounding co-ordinates for the current row
    let rowRect = document.getElementById(tableId).rows.item(i).getBoundingClientRect();

    // check if the current row is visible. If it is, retrieve the AST parsing state for the project
    if (rowRect.top <= rect.bottom && rowRect.bottom >= rect.top) {
      let rowId = `dependents-table.row-${i}` // generate an id to store information about the row being viewed to localstorage

      // if the row has never entered the view field before, fetch its missing fields
      if (!(rowId in localStorage)) {

        let cell = document.getElementById(tableId).rows.item(i).cells.namedItem("call-graph-row");
        let repoUrl = document.getElementById(tableId).rows.item(i).cells.namedItem("short-url").innerHTML;
        let dependentsCell = document.getElementById(tableId).rows.item(i).cells.namedItem("dependents-count-row");

        // retrieve the AST parsing state for the project
        retrieveProjectASTState(cell, repoUrl);
        retrieveTransitiveDependentStats(dependentsCell, repoUrl);
      }

      // store to localstorage that the row being viewed has become visible at least once. This
      // prevents the rows fields being fetched multiple times un-necessairily
      localStorage.setItem(rowId, "loaded");
    }
  }
}

/**
 * renderDependentsTable renders a table of dependent projects
 */
function renderDependentsTable(api_url, project_page_url, owner, repo, tableId) {
    let tableHeader = 
            `<thead>
                <tr>
                    <th scope="col">Project Name</th>
                    <th scope="col">Total Dependents</th>
                    <th scope="col">Call Graph</th>
                    <th scope="col">Explore Project</th>
                </tr>
            </thead>
            <tbody>`;

    let tableBody = "";

    tableBody = "";

    let tableFooter = `</tbody>`;

    // fetch the list of dependents to this project
    let url = `${api_url}/project/${owner}/${repo}/dependents`;
    return fetch(url).then(response => {
        if (response.status >= 200 && response.status < 300) {
          return Promise.resolve(response)
        } else {
          return Promise.reject(new Error(response.statusText))
        }
      }).then(response => {
          response.json().then(responseJson => {

            let projects = responseJson['projects-search'].projects;
            let count = 0; // count maintains a count of the number of dependents found

            // for each project, add a row to the table
            projects.forEach(project => {
                // the github short url is in the form group/repo. Separate the components
                let group = project.github_short_url.split("/")[0];
                let repo = project.github_short_url.split("/")[1];
                tableBody += 
                    `<tr id="dependents-row-${count}">
                        <td><a href="https://github.com/${project.github_short_url}" target="_blank">${project.github_short_url}</a></td>
                        <td id="dependents-count-row"></td>
                        <td id="call-graph-row"></td>
                        <td><a href="${project_page_url}?group=${group}&repo=${repo}" target="_blank">Explore Project</a></td>
                        <td style="display:none;" id="short-url">${project.github_short_url}</td> 
                    </tr>`; // store the github short url in a hidden column. This is used by the dependentsTableScroll function to identify the repository for this row

                count += 1;
            });


          document.getElementById(tableId).innerHTML = tableHeader + tableBody + tableFooter;

          // call the dependents scroll to populate all currently visible rows with their AST results
          dependentsTableScroll(tableId);

          return resolve();
        });
      }).catch(err => {
        document.getElementById(tableId).innerHTML = "<p>Error: Could not fetch dependents of project.</p>"

        return reject(err);
      })
}