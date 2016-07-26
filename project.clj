(defproject western-music "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/clojurescript "1.9.89"]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.4-7"]]

  :source-paths ["src/clj" "src/cljc"]

  :profiles {:dev {:dependencies [[enlive "1.1.6"]
                                  [org.clojure/tools.cli "0.3.5"]
                                  [clj-http-lite "0.3.0"]
                                  [clj-time "0.5.0"]
                                  [cheshire "5.6.3"]
                                  [re-frame "0.8.0-alpha2"]]
                   :cljsbuild {:builds {:client {:source-paths ["devsrc"]
                                                 :compiler     {:main "western-music.dev"
                                                                :asset-path "js"
                                                                :optimizations :none
                                                                :source-map true
                                                                :source-map-timestamp true}}}}
                   
                   :aliases {"initial-ingest" ["run" "-m" "western-music.ingest.run/initial-ingest"]}}}

  
  :figwheel {:server-port 3450
             :repl true}

  
  :clean-targets ^{:protect false} ["resources/public/js" "target"]
  
  :cljsbuild {:builds {:client {:source-paths ["src/cljs" "src/cljc"] 
                                :compiler     {:output-dir "resources/public/js"
                                               :output-to "resources/public/js/client.js"}}}})
