let renderTreemapWithDependentsIcicle = async (_, renderTreemap, group, repo, dependentGroup, dependentRepo) => {
  
  // hashString function from https://gist.github.com/hyamamoto/fd435505d29ebfa3d9716fd2be8d42f0
  function hashString(inString) {
    let hash = 0;

    for (let i = 0; i < inString.length; i++) {
      hash = Math.imul(31, hash) + inString.charCodeAt(i) | 0;
    }

    return hash;
  }

  function hexColourFromString(inString) {
    return "#" + ('000000' + ((hashString(inString)) >>> 0).toString(16)).slice(-6);
  }

  function pastelHslFromString(inString) {
    let hash = Math.abs(hashString(inString));
    let hue = hash % 360;
    let saturation = 50 + (hash % 40);
    let lightness = 65 + (hash % 30);
    return `hsl(${hue}, ${saturation}%, ${lightness}%)`;
  }

  // let dependentGroup = "TestSmells";
  // let dependentRepo = "TestSmellDetector";

  let spinnerOpts = {
    lines: 9, // The number of lines to draw
    length: 9, // The length of each line
    width: 5, // The line thickness
    radius: 14, // The radius of the inner circle
    color: '#EE3124', // #rgb or #rrggbb or array of colors
    speed: 1.9, // Rounds per second
    trail: 40, // Afterglow percentage
    className: 'spinner', // The CSS class to assign to the spinner
  };

  // Define the div for the tooltip
  let tooltip = d3.select("body").append("div")
    .attr("class", "tooltipIcicle")
    .attr("id", "tooltipIcicle")
    .style("opacity", 0);

  let tooltipContents = {
    html: ""
  };

  let retrieveDependentIcicleData = (projectGroup, projectRepo, dependentGroup, dependentRepo, nodeLabel, nodeId) => {

    return new Promise((resolve, reject) => {
      let url = `${api_url}/ast/${projectGroup}/${projectRepo}/dependent?group=${dependentGroup}&repo=${dependentRepo}`;

      if (nodeLabel != null && nodeId != null) {
        url = `${api_url}/ast/${projectGroup}/${projectRepo}/dependent?group=${dependentGroup}&repo=${dependentRepo}&label=${nodeLabel}&id=${nodeId}`;
      }

      return fetch(url).then(response => {
        if (response.status >= 200 && response.status < 300) {
          return Promise.resolve(response)
        } else {
          return Promise.reject(new Error(response.statusText))
        }
      }).then(response => {
        response.json().then(responseJson => {
          return resolve(responseJson.ast[0]);
        });
      }).catch(err => {
        document.write("Could not fetch visualisation data for dependents.");
        return reject(err);
      })
    });
  };

  let projectData = {
    "name": `Project: ${group}/${repo}`,
    "id": `project.${group}.${repo}`,
    "children": [],
    "retrieve_children_url": `${api_url}/project/${group}/${repo}/retrieve/children?dependent_group=${dependentGroup}&dependent_repo=${dependentRepo}`,
    "colour": "#3182bd"
  }

  let projectOpts = {
    width: document.getElementById("project-icicle-col").offsetWidth,
    height: 600,
    clickable: true,
    focusedNode: "",
    tooltip: tooltip,
    tooltipContents: tooltipContents,
    svg: '#project-icicle-svg',
    column: "project-icicle-col"
  };

  let icicleOpts = {
    width: document.getElementById("icicle-div").offsetWidth,
    height: 600,
    tooltip: tooltip,
    tooltipContents: tooltipContents,
    treemap: projectOpts,    
    column: "icicle-div"
  };

  document.addEventListener('mousemove', (e) => {
    let x = e.clientX,
      y = e.clientY;

    projectOpts.tooltip.html(projectOpts.tooltipContents.html).style("top", (y + $(document).scrollTop() - 10) + 'px');
    projectOpts.tooltip.html(projectOpts.tooltipContents.html).style("left", (x + $(document).scrollLeft() + 10) + 'px');
  });

  document.getElementById("dependent-on-icicle").innerHTML = `Components of <i>${dependentGroup}/${dependentRepo}</i> dependent on: <i>Project: ${group}/${repo}</i>`;

  document.getElementById('project-icicle-svg').style.width = projectOpts.width;
  document.getElementById('project-icicle-svg').style.height = projectOpts.height;

  retrieveDependentIcicleData(group, repo, dependentGroup, dependentRepo).then(dependentsData => {
    renderIcicle(icicleOpts, dependentsData);
  });

  renderTreemap(projectData, `project.${group}.${repo}`, projectOpts,
    (data, rootNodeId, opts, onClick, d, i) => {
      if (opts.clickable) {

        let node = fetchNode(data, d.data.id);
        let spinner = new Spinner(spinnerOpts).spin(document.getElementById(opts.column));
        let dependentsSpinner = new Spinner(spinnerOpts).spin(document.getElementById(icicleOpts.column));

        return Promise.all([
          new Promise((resolve, reject) => {
            opts.clickable = false;
            if (node.children.length == 0) {
              return fetch(d.data.retrieve_children_url).then(response => {
                if (response.status >= 200 && response.status < 300) {
                  return Promise.resolve(response)
                } else {
                  return Promise.reject(new Error(response.statusText))
                }
              }).then(response => {
                response.json().then(responseJson => {
                  // update data with new children

                  responseJson.data.forEach(child => {
                    if (child.label == "Method") {
                      child.colour = "#ff6347";
                    } else if (child.label == "ClassOrInterface") {
                      child.colour = "#ffd700";
                    } else if (child.label == "Package") {
                      child.colour = '#8470ff';
                    } else {
                      child.colour = pastelHslFromString(child.label);
                    }
                  });

                  node.children = responseJson.data;
                  renderTreemap(data, d.data.id, opts, onClick);

                  return resolve();
                });
              }).catch(err => {
                document.write("Could not fetch visualisation.");

                return reject(err);
              })
            } else {
              renderTreemap(data, d.data.id, opts, onClick);

              return resolve();
            }
          }),

          new Promise((resolve, reject) => {
            return retrieveDependentIcicleData(group, repo, dependentGroup, dependentRepo, d.data.label, d.data.id).then(dependentsData => {
              renderIcicle(icicleOpts, dependentsData);
              return resolve();
            }).catch(err => {
              return reject(err);
            })
          })
        ]).then(() => {
          spinner.stop();
          dependentsSpinner.stop();
          document.getElementById("dependent-on-icicle").innerHTML = `Components of <i>${dependentGroup}/${dependentRepo}</i> dependent on: <i>${d.data.name}</i>`;
          opts.clickable = true;
        })
      }
    });

  // renderIcicle(icicleOpts);
}
