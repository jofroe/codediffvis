var windowWidth = 1280, windowHeight = 720;
var svg, errorDiv;
var settings
var graph;

// add a new window for the graph
const windowHtml = `
<html>
<head><title>Graph</title></head>
<body style="
margin: 0;
display: flex;
justify-content: center;
align-items: center;
"></body>
</html>
`
var graphWindow = window.open("", "_blank", "innerWidth=" + windowWidth + ",innerHeight=" + windowHeight);
// handle possible already opened window
graphWindow.document.body.innerHTML = '';
graphWindow.document.write(windowHtml);
// update width and height to window with since it might already be open
if (graphWindow.innerWidth > 0 && graphWindow.innerHeight > 0) {
  windowWidth = graphWindow.innerWidth;
  windowHeight = graphWindow.innerHeight;
}
updateSettings();

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.data == undefined) {
    console.log('error retrieving data');
  } else {
    updateSettings();
  }
  return true;
});

chrome.storage.onChanged.addListener((item, area) => {
  var changedKey = Object.keys(item).filter(key => settings[key] != item[key].newValue).map(key => key)[0];
  settings[changedKey] = item[changedKey].newValue;
  if (changedKey == 'input-source') {
    getDataAndRedrawGraph();
    redrawSVG();
  } else {
    graph.restart(changedKey);
  }
});

function updateSettings () {
  chrome.storage.local.get(
    storage => {
      settings = storage;
      getDataAndRedrawGraph();
      redrawSVG()
    });
}

function getDataAndRedrawGraph () {
  var jsonUrl = settings['input-source'];
  if (!jsonUrl.endsWith('.json')) {
    if (!jsonUrl.endsWith('/')) {
      jsonUrl += '/';
    }
    var splitted = location.href.split('/');
    jsonUrl += splitted[splitted.indexOf('merge_requests') + 1] + '.json';
  }
  chrome.runtime.sendMessage({ url: jsonUrl },
    response => {
      if (response.msg != undefined && response.msg.json != undefined) {
        redrawGraph(response.msg.json, response.msg.isCacheUpdated);
      } else {
        var errorDivHeight = 50;
        errorDiv = document.createElement('div')
        errorDiv.setAttribute('class', 'error-div border-bottom');
        errorDiv.setAttribute('style', 'width:' + windowWidth + 'px;height:' + errorDivHeight + 'px;color:darkorange;text-align:center;');
        errorDiv.innerText = 'Error loading JSON.\nPlease check the connection URL.';
        svg.remove();
        graphWindow.document.body.appendChild(errorDiv)
      }
    });
}

function redrawGraph (json, isCacheUpdated) {
  if (isCacheUpdated == undefined || graph == undefined) {
    graph = new Graph(json, svg);
    graph.start();
  }
}

function redrawSVG () {
  if (errorDiv != undefined) {
    errorDiv.remove();
  }
  if (svg != undefined) {
    svg.remove();
  }
  svg = d3.select(graphWindow.document.body)
    .append('svg')
    .attr('id', 'graph')
    .attr('width', windowWidth).attr('height', windowHeight)
    .style('cursor', 'move');
  addResizeHandler();
}

function addResizeHandler () {
  graphWindow.addEventListener("resize", e => windowResize(e));
}

function windowResize (event) {
  event.preventDefault();
  windowWidth = graphWindow.innerWidth;
  windowHeight = graphWindow.innerHeight;
  svg.attr('width', windowWidth);
  svg.attr('height', windowHeight);
  if (graph != undefined) {
    graph.updateForceCenter();
  }
}
