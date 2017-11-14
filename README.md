# trilystro

[![Dependencies Status](https://versions.deps.co/deg/trilystro/status.svg)](https://versions.deps.co/deg/trilystro)

Trilystro is a [re-frame](https://github.com/Day8/re-frame) application written for two purposes:

1) It is a testbed for me to play with ideas for [Sodium](https://github.com/deg/sodium),
   [re-frame-firebase](https://github.com/deg/re-frame-firebase), and my other utility
   projects.

2) It is an exploration of ways to curate information for personal use, and play with
   ideas I've been thinking about for a while.

### Usage

Currently, Trilystro is a tagged note-taking app. It lets your record a note about a URL
and mark it with tags. These notes can be private or public to the world.

It shows off:

- Using Sodium's Semantic-UI wrapper for a GUI that is pleasant on both desktop and mobile.
- A Firebase backend, wrapped by re-frame-firebase
- Google-based authentication, powered by Firebase
- Google ads, wrapped by Semantic-UI and Sodium
- Using augmented finite state machines (FSMs) to manage state

A copy of Trilystro is usually running at http://trilystro.vuagain.com (It is hosted on
Heroku's free tier, which shuts it down upon idle, so please give about 30 seconds for
first startup).



## Development Mode

### Warning: Checkouts directory

This project is configured for my needs, and assumes that the developer is also working
on Sodium and re-frame-firebase.  To this end, it assume that those two projects are
symlinked in the checkouts dir.  Unfortunately, this results in problems with Figwheel.
For details, see https://github.com/deg/trilystro/issues/1#issuecomment-336803953.

If you want to run this project in development mode, you will need to do one of the
following:

1) Modify projects.cljs: Remove the four strings that start with `"checkouts"`

2) or join me developing all of the projects:

  - Checkout the Sodium and re-frame-firebase project to your local machine.
  - Create a checkouts subdirectory under Trilysto
  - Add symlinks inside it to the Sodium and re-frame-firebase projects.
  - (See https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md#checkout-dependencies
    for details).

3) or wait for a cleaner fix, once Figwheel fully handles checkouts. This is being
   discussed at https://github.com/bhauman/lein-figwheel/pull/517.



### Start Cider from Emacs:

Put this in your Emacs config file:

```
(setq cider-cljs-lein-repl "(do (use 'figwheel-sidecar.repl-api) (start-figwheel!) (cljs-repl))")
```

Navigate to a clojurescript file and start a figwheel REPL with `cider-jack-in-clojurescript` or (`C-c M-J`)

### Compile css:

Compile css file once.

```
lein garden once
```

Automatically recompile css file on change.

```
lein garden auto
```

### Run application:

```
lein clean
lein figwheel dev
```

Or, from Emacs with Cider, open one of the `.cljs` files and invoke `C-c M-J`
(`cider-jack-in-clojurescript`).

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

### Run tests:

```
lein clean
lein doo phantom test once
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).

## Production Build

```
lein clean
lein uberjar
java -jar target/trilystro.jar
```

That should compile the clojurescript code first, and then create the standalone jar.

When you run the jar you can set the port the ring server will use by setting the environment variable PORT.
If it's not set, it will run on port 3000 by default.

To deploy to heroku, first create your app:

```
heroku create
```

Then deploy the application:

Either to Heroku:

```
git push heroku master
```

Or to my github repo:

```
./deploy.sh
```


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```
