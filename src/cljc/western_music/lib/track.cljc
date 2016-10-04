(ns western-music.lib.track
  "Current dumping ground for method extensions (youtube), should probably be moved to a more YT 
   centric namespace at some point"
  (:require [clojure.spec :as s]
            [western-music.spec :as spec]))

(s/def :track/youtube-id string?)

(defmethod spec/track-spec :track/youtube
  [_]
  (s/keys :reqs [:track/type
                 :track/artist
                 :track/title
                 :track/youtube-id
                 :track/id]))

(defn minimal
  "Returns a minimal (non-spec passing) map of a track"
  [artist composition-name]
  #:track{:artist artist
          :title composition-name})
