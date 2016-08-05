(ns western-music.build.map)

(def ^:const css
  ["reset.css"
   "fonts.css"
   "style.css"
   "map.css"])

(def ^:const js
  ["jquery.js"
   "raphael.min.js"
   "scale.raphael.js"
   "paths.js"
   "init.js"])

(comment
  (print (clojure.string/join "\n" (map (partial format "cat ../europe/simple/js/%s >> resources/public/javascripts/map.js") js)))

  (print (clojure.string/join "\n" (map (partial format "cat ../europe/simple/css/%s >> resources/public/css/map.css") css)))

  )
