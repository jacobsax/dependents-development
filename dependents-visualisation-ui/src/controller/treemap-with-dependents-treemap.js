let renderTreemapWithDependentsTreemap = async (_, renderTreemap, group, repo, api_url, comparisonUrl) => {

    // from https://gist.github.com/hyamamoto/fd435505d29ebfa3d9716fd2be8d42f0
    function hashString(inString) {
        let hash = 0;

        for(let i = 0; i < inString.length; i++) {
        hash = Math.imul(31, hash) + inString.charCodeAt(i) | 0;
        }

        return hash;
    }

    // from https://gist.github.com/hyamamoto/fd435505d29ebfa3d9716fd2be8d42f0
    function hexColourFromString(inString) {
        return "#"+ ('000000' + ((hashString(inString))>>>0).toString(16)).slice(-6);
        // return pastelHslFromString(inString);
    }

    // generates a pastel HSL formatted colour from a string input, based on the hash of the string
    function pastelHslFromString(inString) {
      let hash = Math.abs(hashString(inString));
      let hue = hash % 360;
      let saturation = 50 + (hash % 40);
      let lightness = 65 + (hash % 30);
      return `hsl(${hue}, ${saturation}%, ${lightness}%)`;
    }

    // defines the options used by the spinner
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
        .attr("class", "tooltip")
        .attr("id", "tooltip")	
        .style("opacity", 0);

    let tooltipContents = {html: ""};

    // configuration for the dependent projects treemap
    let dependentOpts = {
        width: document.getElementById("dependent-col").offsetWidth,
        height: 600,
        clickable: true,
        focusedNode: "",
        tooltip: tooltip,
        tooltipContents: tooltipContents,
        svg: '#dependent-svg',
        column: "dependent-col"
    };

    // management of on click events for the dependent projects treemap
    let dependentTreemapOnClick = (data, rootNodeId, opts, onClick, d, i) => {
      let dependentGroup = d.data.id.split("/")[0];
      let dependentRepo = d.data.id.split("/")[1];

      window.open(`${comparisonUrl}?group=${group}&repo=${repo}&dependent_group=${dependentGroup}&dependent_repo=${dependentRepo}`,'_blank');    
    };
    
    // retrieveDependentData retrieves information about dependent projects, and formats
    // this information for rendering through the d3 hierarchy render method
    let retrieveDependentData = (group, repo, nodeLabel, nodeId) => {
        return new Promise((resolve, reject) => {
        let url =  `${api_url}/project/${group}/${repo}/retrieve/dependents`;
    
        if (nodeLabel != null && nodeId != null) {
            url =  `${api_url}/project/${group}/${repo}/retrieve/dependents?label=${nodeLabel}&id=${nodeId}`;
        } 
    
        return fetch(url).then(response => {
            if (response.status >= 200 && response.status < 300) {
            return Promise.resolve(response)
            } else {
            return Promise.reject(new Error(response.statusText))
            }
        }).then(response => {
            response.json().then(responseJson => {
            responseJson.data.forEach(child => {
                child.colour = pastelHslFromString(child.id);
            });
    
            return resolve({
                "name": `Dependent Projects`,
                "id": `DependentProjects`,
                "colour": "#3182bd",
                "children": responseJson.data,
            });
            });
        }).catch(err => {
            document.write("Could not fetch visualisation data for dependents.");
            return reject(err);
        })
        });
    };

    document.getElementById(dependentOpts.column).style.width = window.screen.width/2;
    document.getElementById(dependentOpts.column).style.height = window.screen.height;
    
    document.getElementById('dependent-svg').style.width = dependentOpts.width;
    document.getElementById('dependent-svg').style.height = dependentOpts.height;
    
    let projectData = {
            "name": `Project: ${group}/${repo}`,
            "id": `project.${group}.${repo}`,
            "children": [],
            "retrieve_children_url": `${api_url}/project/${group}/${repo}/retrieve/children`,
            "colour": "#3182bd"
            }
    
    let projectOpts = {
        width: document.getElementById("project-col").offsetWidth,
        height: 600,
        clickable: true,
        focusedNode: "",
        tooltip: tooltip,
        tooltipContents: tooltipContents,
        svg: '#project-svg',
        column: "project-col"
    };

    document.addEventListener('mousemove', (e) => {
        let x = e.clientX,
            y = e.clientY;

        projectOpts.tooltip.html(projectOpts.tooltipContents.html).style("top", (y + $(document).scrollTop() - 10) + 'px');
        projectOpts.tooltip.html(projectOpts.tooltipContents.html).style("left", (x + $(document).scrollLeft() + 10) + 'px');
    });

    
    document.getElementById("project-col").style.width = window.screen.width/2;
    document.getElementById("project-col").style.height = window.screen.height;
    
    document.getElementById("project-svg").style.width = projectOpts.width;
    document.getElementById("project-svg").style.height = projectOpts.height;
    
    let dependentsData = await retrieveDependentData(group, repo, null, null);
    renderTreemap(dependentsData, "DependentProjects", dependentOpts, dependentTreemapOnClick);

    document.getElementById("dependent-on").innerHTML = `Projects Dependent on: <i>Project: ${group}/${repo}</i>`;

    renderTreemap(projectData, `project.${group}.${repo}`, projectOpts, 
      (data, rootNodeId, opts, onClick, d, i) => {
        if (opts.clickable) {  
          let node = fetchNode(data, d.data.id);
          let spinner = new Spinner(spinnerOpts).spin(document.getElementById(opts.column));
          let dependentsSpinner = new Spinner(spinnerOpts).spin(document.getElementById(dependentOpts.column));
  
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
              return retrieveDependentData(group, repo, d.data.label, d.data.id).then(dependentsData => {
                renderTreemap(dependentsData, "DependentProjects", dependentOpts, dependentTreemapOnClick);
                return resolve();
              }).catch(err => {
                return reject(err);
              })
            })
          ]).then(() => {
            spinner.stop();
            dependentsSpinner.stop();
            document.getElementById("dependent-on").innerHTML = `Projects Dependent on: <i>${d.data.name}</i>`;
            opts.clickable = true;
          })
        }
      });
  }