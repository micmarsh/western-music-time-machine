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
  :selected-compositions
  :<- [:selected-nation]
  :<- [:raw-data]
  (fn [[nation data] _]
    (filter (comp #{nation} nation-id) data)))

(defn composition-title
  [composition]
  (let [composer-name (:name (:composer composition))
        composition-name (:name composition)]
    (str composer-name ": " composition-name)))

(def-sub
  :selected-composition-titles
  :<- [:selected-compositions]
  (fn [compositions _]
    (->> compositions
         (map (comp :name :composer))
;         (map composition-title)
         (distinct)
         (sort))))

(def-sub
  :all-nations
  :<- [:raw-data]
  (fn [data _]
    (->> data
         (map nation-id)
         (distinct)
         (sort))))
