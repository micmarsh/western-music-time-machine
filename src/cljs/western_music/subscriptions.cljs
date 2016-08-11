(ns western-music.subscriptions
  (:require [re-frame.core :refer [def-sub]]
            [western-music.lib.composition :as composition]
            [western-music.lib.ui :as ui]
            [western-music.util :as util]
            [western-music.protocols :as p]))

(def-sub
  :raw-data
  (fn [data _] (:data/raw data)))

(def-sub
  :ui-state
  (fn [data _] (:data/ui data)))

(def-sub
  :selected-nation
  :<- [:ui-state]
  (fn [ui _] (ui/selected-nation ui)))

(def-sub
  :all-nations
  :<- [:raw-data]
  (fn [data _]
    (->> data
         (map composition/nation-id)
         (distinct)
         (sort))))

(defn ->already-queued? [queue]
  (comp (set (map :track/id queue)) :track/id))

(def-sub
  :selected-composers
  :<- [:selected-nation]
  :<- [:raw-data]
  (fn [[nation data] _]
    (->> data
         (filter (comp (partial util/string= nation) composition/nation-id))
         (map composition/composer-name)
         (distinct)
         (sort))))

(def-sub
  :selected-composer
  :<- [:ui-state]
  (fn [ui _] (ui/composer ui)))

(defn display-track-no-composer 
  "TODO don't like keeping this here, but it's not really part of lib.ui
   either. Figure something out later"
  [track]
  (reify p/DisplayData
    (display [_] (:track/title track))
    (id [_] (:track/id track))))

(def-sub
  :selected-tracks
  :<- [:ui-state]
  (fn [ui _] 
    (let [queue (ui/player-queue (ui/player ui))
          already-queued? (->already-queued? queue)]
      (into [] (comp (remove already-queued?)
                     (map display-track-no-composer))
            (ui/track-list ui)))))

(def-sub
  :player
  :<- [:ui-state]
  (fn [ui _] (ui/player ui)))

(defn display-track 
  "TODO same deal as above"
  [track]
  (reify p/DisplayData
    (display [_] 
      (str (:track/artist track) " - " (:track/title track)))
    (id [_] (:track/id track))))

(def-sub
  :track-queue
  :<- [:player]
  (fn [player _]
    (mapv display-track (ui/player-queue player))))

(def-sub
  :current-track
  :<- [:player]
  (fn [player _]
    (display-track (ui/player-playing player))))

(def-sub
  :paused?
  :<- [:player]
  (fn [player _]
    (ui/player-paused? player)))
