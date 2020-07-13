var highlightedNode;
var isHighlightLocked = false;
var isHovering = false;
var isClickExpand = false;

d3.select(graphWindow.document.body)
    .on("keydown", event => {
        if (d3.event.key == 'Control') {
            isHighlightLocked = isHovering && !isHighlightLocked ? true : false;
            if (!isHighlightLocked && !isHovering) {
                graph._onHoverExit();
            }
        } else if (d3.event.key == 'Shift') {
            isClickExpand = true;
        }
    })
    .on('keyup', event => {
        if (d3.event.key == 'Shift') {
            isClickExpand = false;
        }
    });


window.addEventListener('load', () => {
    checkForFileContent();
}, false);


var filesLengthOld = 0;
function checkForFileContent () {
    var files = Array.from(document.getElementsByClassName("file-holder"));
    if (files.length == 0 || filesLengthOld < files.length) {
        filesLengthOld = files.length;
        // wait 2 sec repeatedly until element creation
        return setTimeout(() => checkForFileContent(), 2000);
    } else {
        files.forEach(e => e.addEventListener('mouseover', mouseOverCode));
        files.forEach(e => e.addEventListener('mouseleave', mouseLeaveCode));
    }
}

class HighlightedNode {
    constructor(filePath, lineNumber, isOldLineHighlighted) {
        this.filePath = filePath;
        this.classNodes = graph.nodes.filter(n => filePath.endsWith(n.classNodeId));
        this.candidateNodes = this.classNodes.filter(n => this._positionMatches(n, lineNumber, isOldLineHighlighted));

        if (this.classNodes.length == 0 || this.candidateNodes.length == 0) {
            this.node = graph.nodes.find(n => filePath.endsWith(n.id.replace(/\//g, '.')));
        } else {
            this.maxLineNumber = Math.max(...this.candidateNodes.map(n => this._position(n, isOldLineHighlighted)[0]));
            this.node = this.candidateNodes.find(n => this._position(n, isOldLineHighlighted)[0] == this.maxLineNumber);
        }
    }

    matches = (lineNumber, isOldLine) => {
        var candidateNodesNew = [];
        if (this.classNodes.length != 0) {
            candidateNodesNew = this.classNodes.filter(n => this._positionMatches(n, lineNumber, isOldLine));
        }
        if (candidateNodesNew.length == 0) {
            if (this.node == graph.nodes.find(n => this.filePath.endsWith(n.id.replace(/\//g, '.')))) {
                return true;
            }
            return false;
        }
        if (this.node == undefined) { return false; }
        var maxLineNumberNew = Math.max(...candidateNodesNew.map(n => this._position(n, isOldLine)[0]));
        var nodeNew = candidateNodesNew.find(n => this._position(n, isOldLine)[0] == maxLineNumberNew);
        return nodeNew.id == this.node.id;
    }

    _position = (node, isOldLine) => {
        return isOldLine && node.positionOld != null ? node.positionOld : node.position;
    }

    _positionMatches = (node, lineNumber, isOldLine) => {
        var position = this._position(node, isOldLine);
        return position[0] <= lineNumber && position[1] >= lineNumber;
    }
}

function mouseOverCode (event) {
    if (graph == undefined) {
        return;
    }
    var lineHolder = event.toElement.closest('.line_holder');
    if (lineHolder != undefined) {
        if (event.toElement.classList.contains('empty-cell')) {
            return; // empty cell hovered
        }
        var containsDefinition = event.toElement.classList.contains('line_content') || event.toElement.classList.contains('diff-line-num')
        var classes;
        if (containsDefinition) {
            classes = event.toElement.classList
        } else {
            var candidate = event.toElement.closest('.line_content');
            if (candidate == undefined) {
                candidate = event.toElement.closest('.diff-line-num');
            }
            if (candidate == null) {
                // error finding candidate
                return;
            }
            classes = candidate.classList;
        }
        const isOldLine = classes.contains('old_line') || classes.contains('left-side') || classes.contains('old');
        var line = lineHolder.getElementsByClassName(isOldLine ? 'old_line' : 'new_line')[0];
        if (line == undefined) {
            line = lineHolder.getElementsByClassName(!isOldLine ? 'old_line' : 'new_line')[0];
        }
        const lineNumberLink = line.getElementsByTagName('a')[0];
        var filePath = event.currentTarget.getElementsByClassName("file-title-name")[0].attributes['data-original-title'];
        filePath = filePath == undefined ? event.currentTarget.getElementsByClassName("file-title-name")[0].attributes['title'] : filePath;
        if (lineNumberLink == undefined || filePath == undefined) {
            return; // no link found
        }
        const lineNumber = lineNumberLink.attributes['data-linenumber'].value;
        filePath = filePath.value.replace(' deleted', '');
        filePath = '.' + filePath.replace(/\//g, '.').replace(/.java$/, '');
        if (highlightedNode != undefined && highlightedNode.classNodePath == filePath && highlightedNode.matches(lineNumber, isOldLine)) {
            return; // same highlight
        }
        graph._onHoverExit();
        graph._tick();

        highlightedNode = new HighlightedNode(filePath, lineNumber, isOldLine);
        if (highlightedNode.node != undefined) {
            d3.select(graphWindow.document.body).selectAll('g.node')
                .filter(n => n.id == highlightedNode.node.id)
                .each(n => {
                    graph.focus(n);
                    graph._onNodeHover(n);
                });
        }
    }
}

function mouseLeaveCode (event) {
    if (graph == undefined) {
        return;
    }
    highlightedNode = undefined;
    graph._onHoverExit();
    graph._tick();
}

function getClickableElement (filePath, lineNumber) {
    if (filePath == null) {
        return null;
    }
    var filePath = filePath.split('\\').join('/');
    const fileElement = Array.prototype.find.call(
        document.getElementsByClassName("file-title-name"),
        fileTitle => {
            var title = fileTitle.attributes["data-original-title"];
            title = title == undefined ? fileTitle.attributes['title'] : title;
            return title != undefined && filePath.endsWith(title.value.replace(' deleted', ''));
        });
    if (fileElement == undefined) {
        return null;
    }
    return getCorrespondingLineNumber(fileElement, lineNumber);
}

function getCorrespondingLineNumber (fileElement, lineNumber) {
    if (lineNumber == null) {
        return fileElement.closest('.file-header-content').getElementsByTagName('a')[0];
    }
    var clickableElement = Array.prototype.flatMap.call(
        fileElement.closest(".diff-file").getElementsByClassName("diff-line-num"),
        line => Array.prototype.filter.call(
            line.getElementsByTagName("a"),
            lineNumberLink => lineNumber == lineNumberLink.attributes["data-linenumber"].value
        )
    ).find(element => element != undefined);
    if (clickableElement != undefined) {
        return clickableElement;
    }
    return fileElement.parentElement;
}

isClickable = e => {
    return e != null && ((e.getAttribute('onclick') != null) || (e.getAttribute('href') != null));
}
