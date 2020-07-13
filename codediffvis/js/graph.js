const stylesRect = {
    'rx': d => NodeType.isTypeMethod(d.type) ? '1rem' : 0,
    'fill': d => ColorHelper.getColor(d, settings['slider-mode']),
    'stroke-width': d => isNodeReviewed(d) ? "1px" : "2px",
    'stroke': '#000',
    'stroke-width': '1px',
    'height': '2rem'
};
const stylesRect2 = {
    'rx': d => NodeType.isTypeMethod(d.type) ? '1rem' : 0,
    'stroke': 'white',
    'fill': 'white',
    'height': '2rem'
};
const stylesLine = {
    'stroke-dasharray': d => d.status != NodeStatus.UNCHANGED && d.relation == NodeRelation.METHOD_CALL ? ('3, 3') : NodeRelation.isTypeMethod(d.relation) ? ('2, 2') : 'none',
    'stroke': d => settings['slider-mode'] ? ChangeBasedColor[d.status] : 'rgb(192, 192, 192)',
    'stroke-width': d => d.status != NodeStatus.UNCHANGED && d.relation == NodeRelation.METHOD_CALL ? 3 : 2
};
const stylesText = {
    'font-weight': d => isNodeReviewed(d) ? "normal" : "bold",
    'fill': d => ColorHelper.getTextColor(d, settings['slider-mode']),
    'font-family': 'sans-serif',
    'font-size': '1em',
    'pointer-events': 'none'
}
const stylesTextParent = {
    'fill': d => 'black',
    'font-family': 'sans-serif',
    'font-size': '0.75em',
    'pointer-events': 'none'
}
const stylesCircle = {
    'fill': d => ColorHelper.getColor(d, settings['slider-mode']),
    'opacity': 0.25,
    'stroke': d => d.type == NodeType.TYPE_REFERENCE || d.type == NodeType.METHOD_REFERENCE ? 'black' : 'none'
}
const stylesTextType = {
    'fill': d => ColorHelper.getTextColor(d, settings['slider-mode']),
    'font-family': 'sans-serif',
    'font-size': '0.75em',
    'pointer-events': 'none',
    'font-style': 'italic'
}

var draggingNode = false;
class Graph {

    constructor(data, svg) {
        this.data = data;
        this.tree = new NodeTree(data);
        this.svg = svg;
        this.nodes = this.tree.nodes;
        this.links = this.tree.links;
        this._updateTree();

        this.simulation = d3.forceSimulation(this.nodes)
            .force('link', d3.forceLink(this.links).id(d => d.id).distance(50).strength(0.1))
            .force('charge', d3.forceManyBody().strength(-500))
            .force('forceX', d3.forceX(windowWidth / 2).strength(0.05))
            .force('forceY', d3.forceY(windowHeight / 2).strength(0.05))
            .force('center', d3.forceCenter(windowWidth / 2, windowHeight / 2))
            .force('collision', collisionForce)
            .nodes(this.nodes)

        this.container = this.svg.append('g').attr('class', 'container');
        this.simulation.on('tick', () => this._tick());
        this.simulation.velocityDecay(0.75);

        this.zoom = d3.zoom().on('zoom', () => {
            var nodeX = this.nodes.map(n => n.x);
            var nodeY = this.nodes.map(n => n.y);
            var minX = Math.min(...nodeX),
                minY = Math.min(...nodeY),
                maxX = Math.max(...nodeX),
                maxY = Math.max(...nodeY);
            var distX = 200 + 0.1 * (maxX - minX) / 2,
                distY = 200 + 0.1 * (maxY - minY) / 2;
            this.zoom.translateExtent([
                [minX - distX, minY - distY],
                [maxX + distX, maxY + distY]
            ])
            return this.container.attr('transform', d3.event.transform)
        }).scaleExtent([0, 2])
        this.svg.call(this.zoom)
            .on('dblclick.zoom', null);
    }

    start = () => {
        this._draw();
    }

    restart = changedKey => {
        this._update(changedKey);
        this._redraw();
    }

    updateForceCenter = () => {
        this.simulation.force('center', d3.forceCenter(windowWidth / 2, windowHeight / 2));
        this.simulation.alphaTarget(0.001).restart()
    }

    focus = node => {
        this.svg.transition().duration(500)
            .call(this.zoom.translateTo, node.x, node.y)
    }

    _filterAndUpdateNodes = changedKey => {
        if (changedKey == 'slider-nonjava') {
            if (settings[changedKey]) {
                var nodesNonJava = this.tree.nodes.filter(d => d.type == NodeType.NONJAVA);
                this._pushNodes(this.nodes, nodesNonJava);
            } else {
                this.nodes = this.nodes.filter(d => d.type != NodeType.NONJAVA);
            }
        } else if (changedKey == 'slider-generated') {
            if (settings[changedKey]) {
                var nodesGenerated = this.tree.nodes.filter(d => d.status != NodeStatus.UNCHANGED && d.isGenerated);
                this._pushNodes(this.nodes, nodesGenerated);
            } else {
                this.nodes = this.nodes.filter(d => !d.isGenerated);
            }
        } else if (changedKey == 'slider-methods') {
            if (settings[changedKey]) {
                var nodesMethodsUnchanged = this.tree.nodes.filter(d => d.type == NodeType.METHOD && d.status != NodeStatus.UNCHANGED)
                this._pushNodes(this.nodes, nodesMethodsUnchanged);
            } else {
                this._filterNodesStart();
            }
        }
    }

    _filterNodesStart = () => {
        var filter = [NodeType.CLASS, NodeType.INTERFACE, NodeType.ABSTRACT_CLASS];
        if (settings['slider-nonjava']) {
            filter.push(NodeType.NONJAVA);
        }
        if (settings['slider-methods']) {
            filter.push(NodeType.METHOD)
        }
        this.nodes = this.tree.nodes.filter(
            n => {
                var showGeneratedFiles = true;
                if (!settings['slider-generated']) {
                    showGeneratedFiles = !n.isGenerated;
                }
                return n.status != NodeStatus.UNCHANGED && filter.includes(n.type) && showGeneratedFiles;
            }
        );
    }

    _positionNodesInitial = () => {
        //position main nodes
        var radius = 200 + 20 * this.nodes.length;
        var mainNodes = this.nodes.filter(n => n.classNodeId == undefined);
        mainNodes.forEach((n, i) => {
            var angle = 2 * Math.PI / mainNodes.length * i;
            n.x = radius * Math.sin(angle);
            n.y = radius * Math.cos(angle);
        });
        //position non main nodes that haven't been placed yet
        radius = 500;
        var nonMainNodes = this.nodes.filter(n => n.classNodeId != undefined && !NodeType.isTypeMethod(n.type) && n.x == undefined && n.y == undefined);
        nonMainNodes.forEach((n, i) => {
            var angle = 2 * Math.PI / nonMainNodes.length * i;
            var mainNode = mainNodes.find(d => d.id == n.classNodeId);
            n.x = radius * Math.sin(angle) + (mainNode == undefined ? 0 : mainNode.x);
            n.y = radius * Math.cos(angle) + (mainNode == undefined ? 0 : mainNode.y);
        });
        //position methods
        radius = 100;
        var methodNodes = this.nodes.filter(n => n.classNodeId != undefined && NodeType.isTypeMethod(n.type));
        methodNodes.forEach((n, i) => {
            var angle = 2 * Math.PI / methodNodes.length * i;
            var parentNode = mainNodes.find(d => d.id == n.parentNodeId);
            n.x = radius * Math.sin(angle) + (parentNode == undefined ? 0 : parentNode.x);
            n.y = radius * Math.cos(angle) + (parentNode == undefined ? 0 : parentNode.y);
        });
    }

    _filterAndUpdateLinks = () => {
        this.links = this.tree.links.filter(l =>
            this.nodes.find(n => n.id == l.source || n == l.source)
            && this.nodes.find(n => n.id == l.target || n == l.target));
    }

    _update = (changedKey) => {
        this._filterAndUpdateNodes(changedKey);
        this._filterAndUpdateLinks();
        ColorHelper.updatePackageColor(this.nodes.map(n => n.packageName));
    }

    _updateTree = () => {
        this._filterNodesStart();
        this._positionNodesInitial();
        this._filterAndUpdateLinks();
        ColorHelper.updatePackageColor(this.nodes.map(n => n.packageName));
    }

    _tick = () => {
        this.container.selectAll('circle.circle')
            .attr('cx', d => d.x)
            .attr('cy', d => d.y)
            .attr('r', d => d.radius);

        this.container.selectAll('rect')
            .attr('x', d => d.x - d.width2)
            .attr('y', d => d.y - remToPixel(1))
            .attr('width', d => d.width);

        this.container.selectAll('text.name')
            .attr('x', d => d.x - d.width2 + remToPixel(1.25))
            .attr('y', d => d.y + remToPixel(0.3));

        this.container.selectAll('text.parent')
            .attr('x', d => d.x - d.widthParent2 + remToPixel(1))
            .attr('y', d => d.y - remToPixel(1.2));

        this.container.selectAll('line')
            .attr('x1', d => d.source.x)
            .attr('y1', d => d.source.y)
            .attr('x2', d => d.target.x)
            .attr('y2', d => d.target.y);

        this.container.selectAll('text.textType')
            .attr('x', d => d.x - d.width2 + remToPixel(0.5))
            .attr('y', d => d.y + remToPixel(0.25));
    }

    _redraw = () => {
        this.simulation.nodes(this.nodes);
        this.simulation.force('link')
            .links(this.links)
            .initialize(this.nodes);

        var windowBody = d3.select(graphWindow.document.body);
        windowBody.selectAll('g.node').remove();
        windowBody.selectAll('g.node').data(this.nodes).join();
        windowBody.selectAll("line").data(this.links).join();
        windowBody.selectAll("undefined").remove();

        this._draw();
        this.simulation.alphaTarget(0.001).restart()
    }

    _draw = () => {
        var nodeIds = this.nodes.map(n => n.id);
        var treeNodesMap = this.tree.nodesMap;

        this.container.selectAll('line')
            .data(this.links)
            .join('line').attr('class', 'links')
            .styles(stylesLine);

        this.container.selectAll('g.node').raise()
            .data(this.nodes)
            .join("g").attr("class", 'node')
            .each(function (d, i) {
                d3.select(this).append("rect").attr('class', 'rect2').styles(stylesRect2);
                d3.select(this).append("rect").attr('class', 'rect').styles(stylesRect);
                var text = d3.select(this).append("text").attr('class', 'name').text(d => d.name).styles(stylesText);
                d.width = text.filter(t => t.index == d.index).node().getBBox().width + remToPixel(2);
                d.width2 = d.width / 2;
                d.height = remToPixel(2);

                if (!NodeType.isTypeMethod(d.type) && d.classNodeId != null) {
                    var textParent = d3.select(this).append("text").attr('class', 'parent').text(d => treeNodesMap.get(d.classNodeId).name).styles(stylesTextParent);
                    d.widthParent = textParent.filter(t => t.index == d.index).node().getBBox().width + remToPixel(2);
                    d.widthParent2 = d.widthParent / 2;
                }

                if (!NodeType.isTypeMethod(d.type)) {
                    var methods = d.children.filter(c => NodeType.isTypeMethod(c.type) && nodeIds.includes(c.id)).length
                    d.radius = methods > 0 ? d.width2 + remToPixel(1) + methods * remToPixel(1) + 2 : 0;
                    d3.select(this).append("circle").attr('class', 'circle').styles(stylesCircle).lower();
                }

                if (NodeType.isDeclaredType(d.type) && d.type != NodeType.METHOD) {
                    d3.select(this).append('text').attr('class', 'textType').text(d => d.type[0]).styles(stylesTextType);
                }
            })
            .on('mouseover', (d) => this._onNodeHover(d))
            .on('mouseout', () => this._onHoverExit())
            .on('click', d => this._onNodeClick(d))
            .on('contextmenu', d => this._onNodeContextMenu(d))
            .call(drag(this.simulation));
    }

    _onNodeHover = node => {
        if (draggingNode) {
            return;
        }
        isHovering = true;
        var isNodeClickable = isClickable(this._getClickableElement(node));
        if (isNodeClickable) {
            this.svg.style('cursor', 'grab');
        }
        this.container.selectAll('text.name').style('text-decoration', o => o == node && isNodeClickable ? 'underline' : 'none');
        if (isHighlightLocked) {
            return;
        }
        var connectedNodes = this._getConnectedNodes(node);
        var connectedLinks = this.links.filter(l => connectedNodes.includes(l.source) && connectedNodes.includes(l.target));

        this.container.selectAll('text').filter(o => !connectedNodes.includes(o))
            .style('color', 'black').style('opacity', 0.1)
        this.container.selectAll('rect.rect,line,circle')
            .filter(o => !connectedNodes.includes(o) && !connectedLinks.includes(o))
            .style('opacity', 0.1)
        this.container.selectAll('line').filter(o => connectedLinks.includes(o)).raise()
        this.container.selectAll('g.node').filter(o => connectedNodes.includes(o)).raise()
        this.container.selectAll('g.node').filter(o => connectedNodes.includes(o) && NodeType.isTypeMethod(o.type)).raise()
    }

    _onHoverExit = () => {
        if (draggingNode) {
            return;
        }
        isHovering = false;
        if (isHighlightLocked) {
            return;
        }
        this.svg.style('cursor', 'move');
        this.container.selectAll('text.name').styles(stylesText).style('opacity', 1).style('text-decoration', 'none');
        this.container.selectAll('text.parent').styles(stylesTextParent).style('opacity', 1).style('text-decoration', 'none');
        this.container.selectAll('rect.rect,line').style('opacity', 1)
        this.container.selectAll('circle.circle').style('opacity', stylesCircle['opacity'])
        this.container.selectAll('text.textType').styles(stylesTextType).style('opacity', 1);
        this.container.selectAll('g.node').raise();
        this.container.selectAll('g.node').filter(d => NodeType.isTypeMethod(d.type)).raise();
    }

    _getConnectedNodes = node => {
        var connectedNodes = [node];
        if (node.type == NodeType.METHOD) {
            var methodCallNodes = this._visibleChildren(node).filter(child => child != node
                && NodeType.isTypeMethod(child.type)
                && this._visibleChildren(child).find(b => !NodeType.isTypeMethod(b.type)));
            this._pushNodes(methodCallNodes, methodCallNodes.flatMap(m => this._visibleChildren(m).filter(o => !NodeType.isTypeMethod(o.type))));
            this._pushNodes(methodCallNodes, this._visibleChildren(node).filter(c => !NodeType.isTypeMethod(c.type)));
            this._pushNodes(connectedNodes, methodCallNodes);
        } else {
            this._pushNodes(connectedNodes, this._visibleChildren(node));
        }
        return connectedNodes;
    }

    _visibleChildren = node => {
        return node.children.filter(child => this.nodes.includes(child) && node.links.find(l => l == child.id));
    }

    _onNodeClickExpand = node => {
        var dataChanged = false;
        var methodCallNodes;
        if (node.type == NodeType.METHOD && node.status != NodeStatus.UNCHANGED) {
            methodCallNodes = node.children.filter(child => child != node && (NodeType.isTypeMethod(child.type)) && child.children.find(grandChild => !NodeType.isTypeMethod(grandChild.type)));
            this._pushNodes(methodCallNodes, [...new Set(methodCallNodes.flatMap(method => method.children.filter(child => !NodeType.isTypeMethod(child.type))))]);
            dataChanged = this._pushNodes(this.nodes, methodCallNodes, node);
        } else if (NodeType.isDeclaredType(node.type)) {
            methodCallNodes = node.children.filter(grandChild => grandChild.status != NodeStatus.UNCHANGED)
            dataChanged = this._pushNodes(this.nodes, methodCallNodes, node);
        }
        if (dataChanged) {
            node.methodCallNodes = methodCallNodes;
            this._filterAndUpdateLinks();
            this._redraw();
        }
    }

    _onNodeContextMenu = node => {
        d3.event.preventDefault();
        if (Array.isArray(node.methodCallNodes) && node.methodCallNodes.length) {
            this.nodes = this.nodes.filter(o => !node.methodCallNodes.includes(o));
            node.methodCallNodes = [];
        }
        this.nodes = this.nodes.filter(o => o != node);
        this._filterAndUpdateLinks();
        this._redraw();
    }

    _onNodeClick = node => {
        d3.event.preventDefault();
        if (isClickExpand) {
            this._onNodeClickExpand(node);
            return;
        }
        var element = this._getClickableElement(node);
        if (isClickable(element)) {
            element.click();
            node.clicked = true;
            node.width = this.container.selectAll('text.name')
                .style('font-weight', text => isNodeReviewed(text) ? "normal" : "bold")
                .filter(t => t.index == node.index).node().getBBox().width + remToPixel(2);
            node.width2 = node.width / 2;
            this.container.selectAll('rect')
                .style('stroke-width', rect => isNodeReviewed(rect) ? "1px" : "2px");
            this._tick(); // force a tick
        }
    }

    _getClickableElement = d => {
        if (NodeType.isReferencedType(d.type)) {
            return null;
        }
        return getClickableElement(d.filePath, d.declaringClassesName != null ? d.position[0] : null)
    }

    _pushNodes = (nodes, newNodes, reference) => {
        newNodes = newNodes.filter(d => !nodes.includes(d));
        if (newNodes.length == 0) {
            return false;
        }
        newNodes.forEach(d => {
            if (d.x != undefined || d.y != undefined) {
                return;
            }
            if (reference == undefined) {
                d.x = windowWidth / 2;
                d.y = windowHeight / 2;
            } else {
                var a = Math.floor(Math.random() * Math.floor(360));
                d.x = reference.x + Math.cos(a) * reference.radius;
                d.y = reference.y + Math.sin(a) * reference.radius;
            }
        });
        nodes.push(...newNodes);
        ColorHelper.updatePackageColor(this.nodes.map(n => n.packageName));
        return true;
    }
}

drag = simulation => {
    var isLockedBeforeDragging = false;

    dragStart = node => {
        isLockedBeforeDragging = isHighlightLocked;
        isHighlightLocked = true;
        if (!d3.event.active) {
            simulation.alphaTarget(0.01).restart();
        }
        node.fx = node.x;
        node.fy = node.y;
        draggingNode = true;
    }

    dragging = node => {
        node.fx = d3.event.x;
        node.fy = d3.event.y;
    }

    dragEnd = node => {
        node.fx = null;
        node.fy = null;
        if (!isLockedBeforeDragging) {
            isHighlightLocked = false;
        }
        draggingNode = false;
        setTimeout(() => simulation.stop(), 100);
    }

    return d3.drag()
        .on('start', dragStart)
        .on('drag', dragging)
        .on('end', dragEnd);
}

var collisionForce = rectCollide();
function rectCollide () {
    var nodes;

    force = () => {
        var node, parent, child;
        var xDelta, yDelta, xMinDist, yMinDist, xOverlap, yOverlap;
        var isTrappedInside = false, isKeepOutside = false;

        var tree = d3.quadtree(nodes, d => d.x, d => d.y);
        nodes.forEach(n => {
            node = n;
            tree.visit(apply);
        })

        function initCollisionParameters (data) {
            if (node.radius > data.radius) {
                parent = node;
                child = data;
            } else {
                parent = data;
                child = node;
            }

            xDelta = Math.abs(parent.x - child.x);
            yDelta = Math.abs(parent.y - child.y);
            xDeltaSign = Math.sign(parent.x - child.x);
            yDeltaSign = Math.sign(parent.y - child.y);

            xMinDist = parent.width2 + child.width2;
            yMinDist = (parent.height + child.height) / 2;
            xOverlap = xDelta - xMinDist;
            yOverlap = yDelta - yMinDist;
        }

        function isCollisionDetected () {
            return xOverlap < 0 && yOverlap < 0;
        }

        function isParentWithRadius () {
            return parent.radius > 0;
        }

        function handleCircularCollision () {
            if (isCollisionDetected() || !isParentWithRadius()) {
                // nodes are already overlapping or no radius is defined
                return;
            }

            var isChildMethodOfParent = NodeType.isTypeMethod(child.type) && parent.children.includes(child);
            var xMaxDist = 0, yMaxDist = 0;

            if (isChildMethodOfParent) {
                // trap child inside radius
                var dist = Math.sqrt(xDelta ** 2 + yDelta ** 2);
                if (dist > parent.radius) {
                    var scale = parent.radius / dist;
                    xMaxDist = scale * xDelta;
                    yMaxDist = scale * yDelta;
                    isTrappedInside = true;
                }
            } else {
                // keep child outside radius
                var xRadialDist = Math.abs(xDelta - child.width2);
                var yRadialDist = Math.abs(yDelta - child.height / 2);
                isKeepOutside = true;

                if (xDelta < child.width2 && yRadialDist < parent.radius) {
                    // special case where both edges are outside the circle but the child border intersects with the parent radius
                    xRadialDist = 0;
                    yMinDist = parent.radius;
                }
                var dist = Math.sqrt(xRadialDist ** 2 + yRadialDist ** 2);
                if (dist < parent.radius) {
                    var scale = parent.radius / dist;
                    xMinDist = scale * xDelta;
                    yMinDist = scale * yDelta;
                }
            }

            if (isTrappedInside) {
                xOverlap = xMaxDist - xDelta;
                yOverlap = yMaxDist - yDelta;

                // slow down if the nodes are too far apart
                xOverlap = xOverlap < -100 ? -(100 / xOverlap * (xOverlap + 100) + 100) : xOverlap;
                yOverlap = yOverlap < -100 ? -(100 / yOverlap * (yOverlap + 100) + 100) : yOverlap;
            } else {
                xOverlap = xDelta - xMinDist;
                yOverlap = yDelta - yMinDist;
            }
        }

        function isXCorrection () {
            // for circle collisions favor the bigger overlap as it results in a more natural behavior
            return isParentWithRadius() ? Math.abs(xOverlap) > Math.abs(yOverlap) && (isKeepOutside || isTrappedInside) : Math.abs(xOverlap) < Math.abs(yOverlap)
        }

        function apply (quad, x0, y0, x1, y1) {
            if (!quad.data || quad.data.index <= node.index) return false;

            initCollisionParameters(quad.data);
            handleCircularCollision();

            if (isCollisionDetected()) {
                // revert direction if it is a circle trap child inside collision
                if (isXCorrection()) {
                    var xChange = isTrappedInside ? -xDeltaSign * xOverlap : xDeltaSign * xOverlap;
                    parent.x -= isTrappedInside ? 0 : xChange / 2;
                    child.x += isTrappedInside ? xChange : xChange / 2;
                } else {
                    var yChange = isTrappedInside ? -yDeltaSign * yOverlap : yDeltaSign * yOverlap;
                    parent.y -= isTrappedInside ? 0 : yChange / 2;
                    child.y += isTrappedInside ? yChange : yChange / 2;
                }
            }

            isTrappedInside = false, isKeepOutside = false; //reset parameters
            return x0 > parent.x + xMinDist || y0 > parent.y + yMinDist
                || x1 < parent.x - xMinDist || y1 < parent.y - yMinDist;
        }
    }

    force.initialize = d => nodes = d;
    return force;
}
