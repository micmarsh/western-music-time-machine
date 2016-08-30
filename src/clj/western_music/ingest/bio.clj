(ns western-music.ingest.bio
  (:require [western-music.ingest.bio.wikidata :as wiki]))

(def fetch-spec
  {:place/nation wiki/lookup-nation
   :place/city wiki/lookup-city
   :time/year wiki/lookup-year})

(comment
  (def dont-use #{"Thomas Tallis" "Antonio Martin Y Col"
                  "Alonso de Mudarra";; doesn't
                  ;; have country of birth atrribute
                  "Anonymous" ;; obviously not a person
                  "Dietrich Buxtehude" ;; DOB string is apparently not ISO
                  })
 
  )
