#!/bin/bash
# Adapted from https://github.com/timothypratley/whip/blob/master/deploy.sh
# See also https://stackoverflow.com/questions/37667931/how-do-i-deploy-a-single-page-app-written-in-clojurescript-figwheel-to-a-stat
set -e

deploydir=../trilystro-website

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

pushd $deploydir
rm -rf *
cp -r ../trilystro/resources/public/* .
cat > CNAME <<EOF
trilystro.vuagain.com
EOF
popd

git describe --always               > $deploydir/git-describe.txt
git log -1 --format=%cd --date=iso >> $deploydir/git-describe.txt

pushd $deploydir
git add .
git commit -m "Deploy to GitHub Pages"
git push "git@github.com:deg/trilystro.git" gh-pages:gh-pages

popd
