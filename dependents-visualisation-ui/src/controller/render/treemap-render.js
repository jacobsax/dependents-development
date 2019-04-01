function fetchNode(data, id) {
    if (data.id == id) {
        return data;
        // dataToReturn = data;
    } else if (data.hasOwnProperty('children')){
        for (let childPos in data.children) {
            let dataFoundInChild = fetchNode(data.children[childPos], id);
            if (dataFoundInChild != null) {
                return dataFoundInChild;
            }
        }
    }

    return null;
}

function fetchToDepth(data, depth) {
    let children = [];
    let toReturn = _.cloneDeep(data);

    if (data.hasOwnProperty('children')) {
        if (depth > 0) {
            for (let childPos in data.children) {
                let element = fetchToDepth(_.cloneDeep(data.children[childPos]), depth - 1);
                children.push(_.cloneDeep(element));
            }
        } else {
            if (!data.hasOwnProperty('value')) {
                // TODO: calculate value from sub values
                toReturn.value = 200;
            }
        }
    } 

    toReturn.children = _.cloneDeep(children);
    return toReturn;
}

function retrieveDataFromRoot(data, rootNodeId) {
    dataToReturn = null;

    if (data.id == rootNodeId) {
        dataToReturn = fetchToDepth(data, 1);
        // dataToReturn = data;
    } else if (data.hasOwnProperty('children')){
        let toReturn = null;
        for (let childPos in data.children) {
            let dataFoundInChild = retrieveDataFromRoot(data.children[childPos], rootNodeId);
            if (dataFoundInChild != null) {
                toReturn = JSON.parse(JSON.stringify(data));
                toReturn.children = [dataFoundInChild];
                break;
            }
        }

        return toReturn;
    }
    return dataToReturn;
}

function renderTreemap (data, rootNodeId, opts, onClick) {
    opts.focusedNode = rootNodeId;

    if (rootNodeId == undefined) {
        finalData = data;
    } else {
        finalData = retrieveDataFromRoot(data, rootNodeId);
    }

    let rootNode = d3.hierarchy(finalData);

    let width = document.getElementById(opts.column).clientWidth;
    let height = document.getElementById(opts.column).clientHeight;

    let treemapLayout = d3.treemap()
        .size([opts.width, opts.height])
        .paddingOuter(16);


    d3.select(opts.svg)
        .selectAll('g').remove();

    rootNode.sum(function(d) {
        return d.value;
    });

    treemapLayout(rootNode);

    let projectView = d3.select(opts.svg);

    let nodes = projectView
        .selectAll('g')
        .data(rootNode.descendants())
        .enter()
        .append('g')
        .attr('transform', function(d) {return 'translate(' + [d.x0, d.y0] + ')'})

    nodes
        .append('rect')
        .attr('width', function(d) { 
            return d.x1 - d.x0; 
        })
        .attr('fill', function(d) {
        return d.data.colour;
        })
        .attr('height', function(d) { return d.y1 - d.y0; })
        .on("mouseover", function(d) {	
            opts.tooltip.transition()		
                .duration(200)		
                .style("opacity", 0.9);		
            opts.tooltipContents.html = d.data.name;
        })					
        .on("mouseout", function(d) {		
            opts.tooltip.transition()		
                .duration(500)		
                .style("opacity", 0);	
        }).on('click', function(d, i) {
            onClick(data, rootNodeId, opts, onClick, d, i);
        });

    nodes
        .append('text')
        .attr('dx', 4)
        .attr('dy', 14)
        .text(function(d) {
            return d.data.value;
        })
        .style("text-shadow", "0px 0px 20px black");
};