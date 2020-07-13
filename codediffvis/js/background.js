'use strict';

chrome.runtime.onInstalled.addListener(function () {
  chrome.declarativeContent.onPageChanged.removeRules(undefined, function () {
    chrome.declarativeContent.onPageChanged.addRules([{
      conditions: [new chrome.declarativeContent.PageStateMatcher({ pageUrl: { urlMatches: '(merge_requests\/[0-9]+\/diffs)' } })],
      actions: [new chrome.declarativeContent.ShowPageAction()]
    }
    ]);
  });
});

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.url != undefined) {
    d3.json(request.url)
      .then(data => {
        sendResponse({ msg: new Message(data, isCacheUpdated(data)) });
      })
      .catch(error => {
        sendResponse({ error: error });
      });
  }
  return true;
});

function isCacheUpdated (data) {
  chrome.storage.local.get('json-hash', hash => {
    const newHash = hashCode(data);
    if (hash != newHash) {
      chrome.storage.local.set({ 'json-hash': newHash });
      return true;
    }
    return false;
  });
}

function hashCode (json) {
  var hash = 0;
  for (var i = 0; i < json.length; i++) {
    var character = json.charCodeAt(i);
    hash = ((hash << 5) - hash) + character;
    hash = hash & hash;
  }
  return hash;
}

class Message {
  constructor(json, isCacheUpdated) {
      this.json = json;
      this.isCacheUpdated = isCacheUpdated;
  }
}

