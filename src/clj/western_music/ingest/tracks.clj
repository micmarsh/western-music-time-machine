(ns western-music.ingest.tracks
  "August 1st, 2016: add tracks to existing compositions, add ids to both tracks
   and compositions"
  (:require [clojure.spec :as s]
            [western-music.spec :as spec]
            [western-music.lib.composition :as composition]))

(def new-id
  (let [ids (atom 0)]
    (fn [] (swap! ids inc))))

(defn assoc-id [m id-key]
  (assoc m id-key (new-id)))

(defn composition->track
  [composition]
  #:track{:type :track/no-player
          :artist (composition/composer-name composition)
          :title (composition/name composition)})

(defn complete-composition [composition]
  (let [track (assoc-id (composition->track composition) :track/id)
        c (-> composition
              (assoc-id :composition/id)
              (assoc :composition/tracks [track]))
        spec ::spec/composition]
    (if (s/valid? spec c)
      c
      (throw (ex-info (str "spec check failed: " (s/explain-str spec c))
                      (s/explain-data spec c))))))

(comment
  (require 'western-music.data)

  (def new-data  
    (map western-music.ingest.tracks/complete-composition western-music.data/initial-data))
  
)

