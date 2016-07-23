(ns western-music.subscriptions
  (:require [re-frame.core :refer [def-sub]]))

(def nation-id
  ;; TODO this goes somewhere else, as well as
  ;; * some kind of UUID, more solid than name string
  ;; * spec'ed input b/c this relies on a whole mess of nested data
  ;; already
  ;; * (related to last) spec'd in general for validating big 'ol data flow
  (comp :nation :birth :composer))

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
         (filter (comp #{nation} nation-id))
         (map (comp :name :composer))
         (distinct)
         (sort))))

(def-sub
  :selected-composer
  :<- [:ui-state]
  (fn [ui _]
    (:composer ui)))

(def-sub
  :selected-compositions
  :<- [:selected-composer]
  :<- [:raw-data]
  (fn [[composer data] _]
    (into []
          (comp (filter (comp #{composer} :name :composer))
                (map :name))
          data)))

(def-sub
  :all-nations
  :<- [:raw-data]
  (fn [data _]
    (->> data
         (map nation-id)
         (distinct)
         (sort))))

(def-sub :player :<- [:ui-state]
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
