(defproject western-music "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha5"]]
  :profiles {:dev {:dependencies [[enlive "1.1.6"]
                                  [org.clojure/tools.cli "0.3.5"]]
                   :aliases {"initial-ingest" ["run" "-m" "western-music.ingest.run/initial-ingest"]}}})
