(ns western-music.ingest.bio.wikidata
  (:require [org.httpkit.client :as http]
            [clojure.string :as str]
            [cheshire.core :as json]))

(declare lookup-nation lookup-city lookup-year)

(def ^:const base-url "https://www.wikidata.org/w/api.php")

(def ^:const properties
  {:place-of-birth "P19"
   :date-of-birth "P569"
   :title "P373" ;; technically "Commons Category". Regardless, seems to be a
   ;; good way to cut to the chase and get an identifier
   })

(defmacro defasync [name doc args & body]
  `(defn ~name ~doc
     ([~@(drop-last args)]
        (println "start of single-arity async")
        (let [p# (promise)]
          (println "init'ed promise for single-arity async")
          (~name ~@(drop-last args) (fn [val#] (p# val#) val#))
          (println "returning promise for single-arity async")
          p#))
     ([~@args] ~@body)))

(def id-print #(do (println %) %))

(defasync base-query
  "GET query to wikidata with the given parameters"
  [params callback]
  (println "aboot to query" params)
  (http/get base-url {:query-params (merge {:language "en"
                                            :format "json"}
                                           params)}
            (comp callback id-print)))

(defn body [http-response]
  (-> http-response (:body) (json/decode true)))

(def search-id-value
  "Get the id \"Q-whatever\" from a wikidata response
   TODO extend w/ id-search specific error handling/conflict resolution"
  (comp :id first :search))

(defasync id-search
  "Given a 'title' (a composer in current cases), return
   the wikidata entity id (example \"Q9695\")"
  [title callback]
  (base-query {:action "wbsearchentities" :search title}
              (comp callback search-id-value body)))

(defasync properties
  "Given a wikidata id, return the map of associated properties
   TODO: figure out why promise is super broken, just use callback for now"
  [id callback]
  (base-query {:action "wbgetclaims" :entity id}
              (comp callback :claims id-print body)))
