#!/bin/bash

# npm install -g minifier

rm resources/public/javascripts/map.js
rm resources/public/css/style.css

cat ../europe/simple/js/jquery.js >> resources/public/javascripts/map.js
echo >> resources/public/javascripts/map.js
cat ../europe/simple/js/raphael.min.js >> resources/public/javascripts/map.js
echo >> resources/public/javascripts/map.js
cat ../europe/simple/js/scale.raphael.js >> resources/public/javascripts/map.js
echo >> resources/public/javascripts/map.js
cat ../europe/simple/js/paths.js >> resources/public/javascripts/map.js
echo >> resources/public/javascripts/map.js
cat ../europe/simple/js/init.js >> resources/public/javascripts/map.js
echo >> resources/public/javascripts/map.js

cat ../europe/simple/css/reset.css >> resources/public/css/style.css
echo >> resources/public/css/style.css
cat ../europe/simple/css/fonts.css >> resources/public/css/style.css
echo >> resources/public/css/style.css
cat ../europe/simple/css/style.css >> resources/public/css/style.css
echo >> resources/public/css/style.css
cat ../europe/simple/css/map.css >> resources/public/css/style.css
echo >> resources/public/css/style.css

cp ../europe/simple/xml/settings.xml resources/public/xml/settings.xml

perl -p -i -e "s/\r//g" resources/public/javascripts/map.js 
perl -p -i -e "s/\r//g" resources/public/css/style.css 
perl -p -i -e "s/\r//g" resources/public/xml/settings.xml 

# minify resources/public/javascripts/map.js > resources/public/javascripts/map.min.js
# minify resources/public/css/style.css > resources/public/css/map.min.css
