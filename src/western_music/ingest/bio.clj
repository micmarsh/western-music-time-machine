(ns western-music.ingest.bio
  (:require [western-music.ingest.bio.wikidata :as wiki]))

(def biography-spec
  {:nation wiki/lookup-nation
   :city wiki/lookup-city
   :year wiki/lookup-year})
