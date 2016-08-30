(ns western-music.ingest.track
  (:require [western-music.ingest.html :as title]
            ;; ^ TODO as hinted at by alias, bad name/place for these fns
            [western-music.ingest.bio :as bio]
            [western-music.ingest.fetch :refer [apply-spec]]
            [western-music.ingest.youtube.search :as yt]
            [western-music.lib.composition :as comp]
            [western-music.spec :as spec]))

(def formatted-title
  (partial format "X. %s - %s"))

(def ^:const bio-fetch-spec
  {::spec/composer #:composer{:birth bio/fetch-spec}})

(defn complete-biography [minimal-data]
  (-> minimal-data
      (apply-spec bio-fetch-spec)
      (assoc-in [::spec/composer :composer/birth :place/type] :place/city)
      (assoc-in [::spec/composer :composer/birth :time/type] :time/year)))

(def minimal-data (comp title/parse-title formatted-title))

(defn full-composition [{:keys [yt-api-key
                                new-composition-id
                                new-track-id]
                         :or {new-track-id -1
                              new-composition-id -1}}
                        artist composition-name]
  (assert (not (nil? yt-api-key)) "Need YouTube API Key")
  (->> (minimal-data artist composition-name)
       (complete-biography)
       (#(comp/add-track % (yt/youtube-track yt-api-key new-track-id
                                             #:track{:artist artist
                                                     :title composition-name})))
       (#(assoc % :composition/id new-composition-id))
       (spec/verify ::spec/composition) ))

