# trilystro

[![Dependencies Status](https://versions.deps.co/deg/trilystro/status.svg)](https://versions.deps.co/deg/trilystro)

Trilystro is a [re-frame](https://github.com/Day8/re-frame) application written for two purposes:

1) It is a testbed for me to play with ideas for
   [Sodium](https://github.com/deg/sodium), [Iron](https://github.com/deg/iron),
   [re-frame-firebase](https://github.com/deg/re-frame-firebase), and my other utility
   projects. Also, SPA deployment to github, linked projects, chrome extensions, etc.

2) It is an exploration of ways to curate information for personal use, and play with
   ideas I've been thinking about for a while.

### Usage

Currently, Trilystro is a tagged note-taking app. It lets your record a note about a URL
and mark it with tags. These notes can be private or public to the world.

It shows off:

- Using Sodium's Semantic-UI wrapper for a GUI that is pleasant on both desktop and mobile.
- A Firebase backend, wrapped by re-frame-firebase
- Chrome extensions, working with a re-frame-based project
- Google-based authentication, powered by Firebase
- Google ads, wrapped by Semantic-UI and Sodium
- Using augmented finite state machines (FSMs) to manage state
- Dividing a project into libraries

A copy of Trilystro is usually running at http://trilystro.vuagain.com


Subdirectories of this hold several independently compiled front-ends and libraries. For
now, each is compiled independently and has its own README.md file.
