#!/bin/bash
# Adapted from https://github.com/timothypratley/whip/blob/master/deploy.sh
# See also https://stackoverflow.com/questions/37667931/how-do-i-deploy-a-single-page-app-written-in-clojurescript-figwheel-to-a-stat
set -e
lein clean
lein cljsbuild once min
# lein uberjar
pushd resources/public
git init
git add .
git commit -m "Deploy to GitHub Pages"
git push --force --quiet "git@github.com:deg/trilystro.git" master:gh-pages
popd
rm -fr resources/public/.git
