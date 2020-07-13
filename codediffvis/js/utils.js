var remToPixelMap = new Map();

remToPixel = rem => {
    if (remToPixelMap.has(rem)) {
        return remToPixelMap.get(rem);
    }
    const pixel = Math.round(rem * parseFloat(getComputedStyle(document.documentElement).fontSize));
    remToPixelMap.set(rem, pixel);
    return pixel;
}
