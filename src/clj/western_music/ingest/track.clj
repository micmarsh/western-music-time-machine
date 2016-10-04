(ns western-music.ingest.track
  (:require [western-music.ingest.bio.wikidata :as wiki]
            [western-music.ingest.fetch :refer [apply-spec]]
            [western-music.ingest.youtube.search :as yt]
            [western-music.lib
             [composition :as comp]
             [track :as track]]
            [western-music.spec :as spec]
            [clojure.spec :as s]))

(def ^:const bio-fetch-spec
  {::spec/composer #:composer{:birth {:place/nation wiki/lookup-nation
                                      :place/city wiki/lookup-city
                                      :time/year wiki/lookup-year
                                      :place/type :place/city
                                      :time/type :time/year}}})

(defn full-composition [{:keys [yt-api-key
                                new-composition-id
                                new-track-id]
                         :or {new-track-id -1
                              new-composition-id -1}}
                        artist composition-name]
  (assert (not (nil? yt-api-key)) "Need YouTube API Key")
  (spec/verify
   ::spec/composition
   (-> (comp/minimal artist composition-name)
       (apply-spec bio-fetch-spec)
       (comp/add-track (yt/youtube-track yt-api-key new-track-id
                                         (comp/track artist composition-name)))
       (assoc :composition/id new-composition-id))))


(defn add-new-composition
  [existing yt-api-key artist composition-name]
  (spec/verify (s/coll-of ::spec/composition) existing)
  (let [options {:yt-api-key yt-api-key
                 :new-composition-id (inc (comp/max-id existing))
                 :new-track-id (inc (comp/max-track-id existing))}]
    (conj existing (full-composition options artist composition-name))))

(comment
  (require '[clojure.edn :as edn])

  (def ^:const compositions-path "resources/public/edn/compositions.edn")
  
  (defn add-track [yt-api-key artist title]
    (let [existing-data (edn/read-string (slurp compositions-path))
          new-data (add-new-composition existing-data yt-api-key artist title)]
      (spit compositions-path (pr-str new-data))))
  )
