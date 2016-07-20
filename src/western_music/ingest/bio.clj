(ns western-music.ingest.bio
  (:require [western-music.ingest.bio.wikidata :as wiki]))

(def biography-spec
  {:nation wiki/lookup-nation
   :city wiki/lookup-city
   :year wiki/lookup-year})

(comment
  (def stuff
    [(throw (ex-info "Add real data here" {}))])

  (require '[western-music.ingest
             [fetch :as fetch]
             [bio :as bio]])

  (defn get-stuff [stuff]
    (fetch/apply-spec stuff {:composer {:birth bio/biography-spec}}))

  (get-stuff (first stuff))
  
  (doall (take 4 (map #(fetch/apply-spec % {:composer {:birth bio/biography-spec}}) stuff)))
  ;; TODO this reveals an exception, it appears to by in lookup-city.
  ;; Most likely is id-search just not working out, null propogating

  ;; Also getting a NPE in `wiki/properties call (when working
  ;; backwards), so that's an interesting development
  )
