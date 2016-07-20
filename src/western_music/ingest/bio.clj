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
             [bio :as bio]]
           '[western-music.ingest.bio.wikidata :as bio.wiki]
           '[western-music.util :as util])

  (defn get-stuff [stuff]
    (fetch/apply-spec stuff {:composer {:birth bio/biography-spec}}))

  (get-stuff (first stuff))

  (def cache (atom {}))
  
  (def dont-use #{"Thomas Tallis" "Antonio Martin Y Col"
                  "Alonso de Mudarra";; doesn't
                  ;; have country of birth atrribute
                  "Anonymous" ;; obviously not a person
                  "Dietrich Buxtehude" ;; DOB string is apparently not ISO
                  })
  
  (binding [util/*global-cache* cache]
    (def full-results
      (into []  (comp (remove (comp dont-use :name :composer)) (map get-stuff)) stuff)))
  )
