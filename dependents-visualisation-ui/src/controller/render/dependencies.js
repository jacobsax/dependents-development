function renderDependenciesTable(api_url, owner, repo, tableId) {
    let tableHeader = 
            `<thead>
                <tr>
                    <th scope="col">Group ID</th>
                    <th scope="col">Artifact ID</th>
                </tr>
            </thead>
            <tbody>`;

    let tableBody = "";

    tableBody = "";

    let tableFooter = `</tbody>`;

    let url = `${api_url}/project/${owner}/${repo}/dependencies`;

    return fetch(url).then(response => {
        if (response.status >= 200 && response.status < 300) {
          return Promise.resolve(response)
        } else {
          return Promise.reject(new Error(response.statusText))
        }
      }).then(response => {
          response.json().then(responseJson => {

            let artifacts = responseJson.artifacts;
            artifacts.forEach(artifact => {
                tableBody += 
                    `<tr>
                        <td>${artifact.group}</td>
                        <td>${artifact.artifact}</td>
                    </tr>`;
            });


          document.getElementById(tableId).innerHTML = tableHeader + tableBody + tableFooter;

          return resolve();
        });
      }).catch(err => {
        document.getElementById(tableId).innerHTML = "<p>Error: Could not fetch artifacts for project.</p>"

        return reject(err);
      })
}