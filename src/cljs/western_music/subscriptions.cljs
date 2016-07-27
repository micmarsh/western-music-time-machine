(ns western-music.subscriptions
  (:require [re-frame.core :refer [def-sub]]
            [western-music.lib.composition :as composition]
            [western-music.util :as util]
            [western-music.protocols :as p]))

(def-sub
  :raw-data
  (fn [data _] (:raw data)))

(def-sub
  :ui-state
  (fn [data _] (:ui data)))

(def-sub
  :selected-nation
  :<- [:ui-state]
  (fn [ui _]
    (let [{:keys [mouse-on selected]} (:nation ui)]
      (or selected mouse-on))))

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
  (fn [ui _] (:composer ui)))

(def-sub
  :selected-tracks
  :<- [:ui-state]
  (fn [ui _] 
    (mapv
     (fn [t]
       (reify p/DisplayData
         (display [_] 
           (str (:track/artist t) " - " (:track/title t)))
         (id [_] (:track/id t))))
     (-> ui :player :track-list))))

(def-sub
  :all-nations
  :<- [:raw-data]
  (fn [data _]
    (->> data
         (map composition/nation-id)
         (distinct)
         (sort))))

(def-sub
  :player
  :<- [:ui-state]
  (fn [ui _] (:player ui)))

(def-sub
  :track-queue
  :<- [:player]
  (fn [player _]
    (:queue player)))

(def-sub
  :current-track
  :<- [:player]
  (fn [player _]
    (:playing player)))

(def-sub
  :paused?
  :<- [:player]
  (fn [player _]
    (:paused player)))
