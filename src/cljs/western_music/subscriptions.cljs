(ns western-music.subscriptions
  (:require [re-frame.core :refer [reg-sub]]
            [western-music.lib.composition :as composition]
            [western-music.lib.ui :as ui]
            [western-music.util :as util]
            [western-music.protocols :as p]))

(reg-sub
  :raw-data
  (fn [data _] (:data/raw data)))

(reg-sub
  :ui-state
  (fn [data _] (:data/ui data)))

(reg-sub
  :selected-nation
  :<- [:ui-state]
  (fn [ui _] (ui/selected-nation ui)))

(reg-sub
  :all-nations
  :<- [:raw-data]
  (fn [data _]
    (->> data
         (map composition/nation-id)
         (distinct)
         (sort))))

(defn ->already-queued? [queue]
  (comp (set (map :track/id queue)) :track/id))

(reg-sub
  :selected-composers
  :<- [:selected-nation]
  :<- [:raw-data]
  :<- [:player]
  (fn [[nation data p] _]
    (let [already-queued? (->already-queued? (ui/player-queue p))]
     (sort (into [] (comp (filter (composition/->nation? nation))
                          (remove (comp already-queued? composition/track))
                          (map composition/composer-name)
                          (distinct))
                 data)))))

(reg-sub
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

(reg-sub
  :selected-tracks
  :<- [:ui-state]
  (fn [ui _] 
    (let [queue (ui/player-queue (ui/player ui))
          already-queued? (->already-queued? queue)]
      (into [] (comp (remove already-queued?)
                     (map display-track-no-composer))
            (ui/track-list ui)))))

(reg-sub
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

(reg-sub
  :track-queue
  :<- [:player]
  (fn [player _]
    (mapv display-track (ui/player-queue player))))

(reg-sub
  :current-track
  :<- [:player]
  (fn [player _]
    (display-track (ui/player-playing player))))

(reg-sub
  :paused?
  :<- [:player]
  (fn [player _]
    (ui/player-paused? player)))
