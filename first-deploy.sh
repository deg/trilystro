mkdir ../trilystro-website
pushd ../trilystro-website

git init

cat > CNAME <<EOF
trilystro.vuagain.com
EOF

git add .
git commit -m "Initial deploy to GitHub Pages"
git push --force --quiet "git@github.com:deg/trilystro.git" master:gh-pages

git branch gh-pages
git checkout gh-pages

popd
