const ChangeBasedColor = Object.freeze({
    'ADDED': '#008000',
    'DELETED': '#8B0000',
    'CHANGED': '#FF8C00',
    'UNCHANGED': '#C0C0C0'
});

const ColorHelper = {
    packageMap: null,
    packageColor: null,
    
    getColorByStatus: node => {
        if (NodeType.isReferencedType(node.type)) {
            return '#FFFFFF';
        } else if (node.isGenerated) {
            return '#C0C0C0';
        } else if (node.type == NodeType.NONJAVA) {
            return '#80D0FF';
        }
        return ChangeBasedColor[node.status];
    },
    
    updatePackageColor: packages => {
        ColorHelper.packageMap = new Map([...new Set(packages)].sort().map((p, i) => [p, i]));
        ColorHelper.packageColor = d3.scaleSequential(d3.interpolateRainbow).domain([0, ColorHelper.packageMap.size]);
    },
    
    getColorByPackage: package => {
        return ColorHelper.packageColor(ColorHelper.packageMap.get(package));
    },
    
    getTextColor: (node, isChangeBasedMode) => {
        return ColorHelper.getTextColorByNodeColor(ColorHelper.getColor(node, isChangeBasedMode));
    },
    
    getColor: (node, isChangeBasedMode) => {
        return isChangeBasedMode ? ColorHelper.getColorByStatus(node) : ColorHelper.getColorByPackage(node.packageName);
    },
    
    getTextColorByNodeColor: nodeColor => {
        var r, g, b;
        if (nodeColor.length == 4) {
            r = "0x" + nodeColor[1] + nodeColor[1];
            g = "0x" + nodeColor[2] + nodeColor[2];
            b = "0x" + nodeColor[3] + nodeColor[3];
        } else if (nodeColor.length == 7) {
            r = "0x" + nodeColor[1] + nodeColor[2];
            g = "0x" + nodeColor[3] + nodeColor[4];
            b = "0x" + nodeColor[5] + nodeColor[6];
        } else if (nodeColor.startsWith('rgb')) {
            var rgb = nodeColor.match(/\d+/g);
            r = rgb[0];
            g = rgb[1];
            b = rgb[2];
        } else {
            return 'black';
        }
        var luminance = (Math.min(r, g, b) + Math.max(r, g, b)) / 2;
        return luminance < 127.5 ? 'white' : 'black';
    }
}
