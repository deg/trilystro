# Trilystro

Trilystro is an implementation of VuAgain written in ClojureScript and
[re-frame](https://github.com/Day8/re-frame), created with several goals in mind:

1) It is a testbed for me to play with implementation ideas and styles, exploring and
   enhancing:
   * Dividing re-frame projects into single-concern libraries
   * [Iron](https://github.com/deg/iron) - My utility library for re-frame components.
   * [Sodium](https://github.com/deg/sodium) - A wrapper around Semantic UI, to deliver
     a GUI that is pleasant on both desktop and mobile.
   * [re-frame-firebase](https://github.com/deg/re-frame-firebase) - a re-frame-friendly
     wrapper around Google's Firebase DB; using Firebase as the shared backend between
     an SPA and a Chrome Extension, as well as an interface to Gooogle (and soon
     Facebook) based authentication.
   * Using augmented state machines to manage app state.
   * Writing Chrome extensions in ClojureScript, using re-frame.
   * Deploying dynamic SPA applications on GitHub.
   * Including Google Ads
   * Etc.

2) Continuing the work I did in the original JavaScript prototype of VuAgain. This is my
   exploration of ways to curate information for personal use. I hope to continue to
   expand it and play with new ideas in these directions.


## Usage

Trilystro is a tagged note-taking app. It lets your record a note about a URL
and mark it with tags. These notes can be private or public to the world.

A copy of Trilystro is usually running at http://trilystro.degel.com

Trilysto also includes rudimentary
[Chrome Extension](https://chrome.google.com/webstore/detail/vuagain/dikihecbidmmfpjjkofpcfcpdfefnilf)
that:
* Gives direct access to notes associated with the current browser tab
* Enhances Google search results (and, soon, other pages) to hightlight URLs that have
  notes attached.


## Structure

Subdirectories of this hold several independently compiled front-ends and libraries. For
now, each is compiled independently and has its own README.md file.

* client - the SPA
* chromex - the chromex
* trilib - common utilities and support

## Build/release instructions (interim)

Until we add some more scripting automation, the steps are manual and tedious:

- Ensure that we are not using any -SNAPSHOT dependencies. Check, especially, the three
  support libraries we are currently using: iron, sodium, and re-frame-firebase. Also
  look at the three projects in here: client, chromex, and trilib.
- Upgrade version numbers of the three projects to match the release number
- Upgrade version numbers in the two manifest files, in chromex/resources/*/
- Upgrade version number in popup window (in chromex/src/popup/vuagain/chromex/popup)
- Check in all changes, and tag build _before_ building (so it is captured in about
  info)
- `lein install` Trilib
- build and release chromex and client, per their instructions

