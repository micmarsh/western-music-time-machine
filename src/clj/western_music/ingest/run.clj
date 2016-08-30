(ns western-music.ingest.run
  (:require [western-music.ingest.html :as html]
            [clojure.tools.cli :refer [parse-opts]]))

(def ^:const ingest-urls
  ["http://archive.is/e9wyZ"
   "http://archive.is/nXMZC"
   "http://archive.is/AKGJc"
   "http://archive.is/f1WiX"])

(def ^:const output-file
  ["-f" "--file FILE" "Output file location"
   :validate [#(.endsWith % ".edn") "Should be an edn file"]])

(def ^:const input-file
  ["-i" "--input FILE" "Input file location"
   :validate [#(.endsWith % ".edn") "Should be an edn file"]])

(def ingest-url (comp html/initial-data html/fetch-url))

(defn initial-ingest [& args]
  (let [parsed-args (parse-opts args [output-file])]
    (if-let [location (:file (:options parsed-args))]
      (let [all-data (into [] (mapcat ingest-url) ingest-urls)]
        (spit location (pr-str all-data))
        (println "Succesfully wrote initial data, check" location))
      (do (println (:summary parsed-args))
          (System/exit 1)))))
