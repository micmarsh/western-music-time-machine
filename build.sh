#!/bin/bash

# npm install -g minifier

rm -r javascripts js css img xml

lein with-profile compile-client do clean,  cljsbuild once

mkdir javascripts js css img xml


cp resources/public/img/* img/
cp resources/public/js/client.js js/client.js
cp resources/public/xml/settings.xml xml/settings.xml
cp resources/public/index.html index.html

sed -i '/<script src="javascripts\/utils.js"><\/script>/d' index.html

minify resources/public/javascripts/utils.js resources/public/javascripts/map.js -o javascripts/map.js 
minify resources/public/css/style.css -o css/style.css
