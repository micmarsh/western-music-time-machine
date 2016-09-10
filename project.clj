(defproject western-music "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.4-7"]]

  :source-paths ["src/clj" "src/cljc"]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-MERGE-FIX"]
                                  [re-frame "0.8.0-alpha2"]
                                  [cljs-ajax "0.5.8"]

                                  [org.clojure/test.check "0.9.0"]
                                  [enlive "1.1.6"]
                                  [org.clojure/tools.cli "0.3.5"]
                                  [clj-http-lite "0.3.0"]
                                  [clj-time "0.5.0"]
                                  [cheshire "5.6.3"]]
                   :source-paths ["test/cljc"]
                   :cljsbuild {:builds {:client {:source-paths ["devsrc"]
                                                 :compiler     {:main "western-music.dev"
                                                                :asset-path "js"
                                                                :optimizations :none
                                                                :source-map true
                                                                :source-map-timestamp true}}}}
                   
                   :aliases {"initial-ingest" ["run" "-m" "western-music.ingest.run/initial-ingest"]}}
             
             :compile-client {:dependencies [[org.clojure/clojurescript "0.0-MERGE-FIX"]
                                             [re-frame "0.8.0-alpha2"]
                                             [cljs-ajax "0.5.8"]]
                              
                              :cljsbuild {:builds {:client {:compiler {:main "western-music.core"
                                                                       :asset-path "js"
                                                                       :externs ["resources/public/externs.js"]
                                                                       :optimizations :advanced}}}}}}

  
  :figwheel {:server-port 3450
             :repl true}

  
  :clean-targets ^{:protect false} ["resources/public/js" "target"]
  
  :cljsbuild {:builds {:client {:source-paths ["src/cljs" "src/cljc"] 
                                :compiler     {:output-dir "resources/public/js"
                                               :output-to "resources/public/js/client.js"}}}})
