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
  - Add symlinks inside it to the iron, sodium and re-frame-firebase projects.
  - (See https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md#checkout-dependencies
    for details).
    
I have the following symlinks:
```
iron              -> ../../../iron/
re-frame-firebase -> ../../../re-frame-firebase/
sodium            -> ../../../sodium/

```

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
