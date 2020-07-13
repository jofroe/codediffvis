let input_source = document.getElementById('input-source');
let slider_mode = document.getElementById('slider-mode');
let slider_nonjava = document.getElementById('slider-nonjava');
let slider_generated = document.getElementById('slider-generated');
let slider_methods = document.getElementById('slider-methods');

let label_slider_mode = document.getElementById('label-slider-mode');
let label_slider_nonjava = document.getElementById('label-slider-nonjava');
let label_slider_generated = document.getElementById('label-slider-generated');
let label_slider_methods = document.getElementById('label-slider-methods');

var version = document.getElementById('manifest-version');
version.innerHTML = 'v'+chrome.app.getDetails().version;

input_source.onchange = event => {
  chrome.storage.local.set({ 'input-source': input_source.value });
}

slider_mode.onchange = event => {
  chrome.storage.local.set({ 'slider-mode': slider_mode.checked });
  updateSliderNames();
}

slider_nonjava.onchange = event => {
  chrome.storage.local.set({ 'slider-nonjava': slider_nonjava.checked });
  updateSliderNames();
}

slider_generated.onchange = event => {
  chrome.storage.local.set({ 'slider-generated': slider_generated.checked });
  updateSliderNames();
}

slider_methods.onchange = event => {
  chrome.storage.local.set({ 'slider-methods': slider_methods.checked });
  updateSliderNames();
}

chrome.storage.local.get(storage => {
  input_source.value = storage['input-source'];
  slider_mode.checked = storage['slider-mode'];
  slider_nonjava.checked = storage['slider-nonjava'];
  slider_generated.checked = storage['slider-generated'];
  slider_methods.checked = storage['slider-methods'];
  updateSliderNames();
});

function updateSliderNames () {
  label_slider_mode.innerHTML = slider_mode.checked ? 'Change-based colors' : 'Package-based colors';
  label_slider_nonjava.innerHTML = slider_nonjava.checked ? 'Show non-java nodes' : 'Hide non-java nodes';
  label_slider_generated.innerHTML = slider_generated.checked ? 'Show generated nodes' : 'Hide generated nodes';
  label_slider_methods.innerHTML = slider_methods.checked ? 'Show methods initially' : 'Hide methods initially';
}
