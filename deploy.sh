#!/bin/bash
# Adapted from https://github.com/timothypratley/whip/blob/master/deploy.sh
# See also https://stackoverflow.com/questions/37667931/how-do-i-deploy-a-single-page-app-written-in-clojurescript-figwheel-to-a-stat
set -e

RED='\033[0;31m'
NOCOLOR='\033[0m'

function die(){
  echo -e ${RED}"$1"${NOCOLOR}
  exit 1
}

if [ -n "$(git status --untracked-files=no --porcelain)" ]; then
  die "Aborting deploy. There are uncommited changes.";
fi


lein clean
lein cljsbuild once min || die "Lein cljsbuild failed!"

pushd ../trilystro-website

cp -r ../trilystro/resources/public/* .
git describe --always               > ../trilystro-website/git-describe.txt
git log -1 --format=%cd --date=iso >> ../trilystro-website/git-describe.txt
git add .

git commit -m "Deploy to GitHub Pages"
git push "git@github.com:deg/trilystro.git" gh-pages:gh-pages

popd
