(ns western-music.handlers
  (:require [western-music.data :refer [initial-data]]
            [re-frame.core :refer [def-event path debug]]))

(def-event
  :initialize-data
  (constantly {:raw initial-data
               :ui {}}))

(defn set-value-handler [_ [_ value]] value)

(def-event
  :focus-nation
  [(path :ui :nation :mouse-on)]
  set-value-handler)

(def-event
  :select-nation
  [(path :ui :nation :selected)]
  set-value-handler)

(def-event
  :select-blank
  (path :ui)
  (fn [ui _]
   (dissoc ui :composer :nation)))

(def-event
  :select-composer
  (path :ui :composer)
  set-value-handler)

;; TODO compositions (and composers) now need IDs, because this
;; string-based id'ing has its limits

(defn play-composition
  ;; TODO in addition to raw data, now should have data structs +
  ;; validation for "player" b/c is slightly complex.
  ;; Falling out of that is lib fns that abstract awasy a lot of the
  ;; details here
  [{:keys [queue] :as player} composiition]
  (cond-> player
          (not (contains? (set queue) composiition)) (update :queue (fnil conj []) composiition)
          true (assoc :playing composiition)))

(def-event
  :play-composition
  (path :ui :player)
  (fn [player [_ composer composition]]
    (play-composition
     player
     (str composer " - " composition))))

(def-event
  :enqueue-composition
  (path :ui :player :queue)
  (fn [q [_ composer composition]]
    (conj (or q []) (str composer " - " composition))))

(def-event
  :dequeue-track
  (path :ui :player :queue)
  (fn [q [_ track]]
    (into [] (remove #{track}) q)))

;; TODO oh yeah, don't want to enqueue if already there, so that's
;; another thing that needs to go in here

;; TODO Time selection is the next UI element to incorporate
