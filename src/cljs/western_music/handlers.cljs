(ns western-music.handlers
  (:require [western-music.data :refer [initial-data]]
            [re-frame.core :refer [def-event path debug]]
            [western-music.spec :as spec]))

(def-event
  :initialize-data
  (constantly {:raw initial-data
               :ui {:player {:queue []
                             :paused true}
                    :nation {:mouse-on nil
                             :selected nil}
                    :composer nil}}))

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
          (not (contains? (set queue) composiition)) (update :queue conj composiition)
          true (assoc :playing composiition
                      :paused false)))

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
    (conj q (str composer " - " composition))))

(def-event
  :dequeue-track
  (path :ui :player :queue)
  (fn [q [_ track]]
    (into [] (remove #{track}) q)))

(def-event
  :player-play
  (path :ui :player :paused)
  (constantly false))

(def-event
  :player-pause
  (path :ui :player :paused)
  (constantly true))

(def-event
  :player-back
  (path :ui :player)
  (fn [{:keys [queue playing] :as player} _]
    (let [where (.indexOf queue playing)]
      (if (zero? where)
        player
        (assoc player :playing (queue (dec where)))))))

(def-event
  :player-forward
  (path :ui :player)
  (fn [{:keys [queue playing] :as player} _]
    (let [where (.indexOf queue playing)
          max-index (dec (count queue))]
      (if (= where max-index)
        player
        (assoc player :playing (queue (inc where)))))))

;; TODO oh yeah, don't want to enqueue if already there, so that's
;; another thing that needs to go in here

;; TODO Time selection is the next UI element to incorporate
