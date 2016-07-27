(ns western-music.handlers
  (:require [western-music.data :refer [initial-data]]
            [re-frame.core :refer [def-event path after debug]]
            [western-music.lib.composition :as composition]
            [western-music.util :as util]
            [western-music.spec :as spec]
            [clojure.spec :as s]))

(defn check-and-throw
  "throw an exception if db doesn't match the spec."
  [spec data]
  (when-not (s/valid? spec data)
    (throw (ex-info (str "spec check failed: " (s/explain-str spec data))
                    {:problems (s/explain-data spec data) }))))

(def-event
  :initialize-data
  (after (comp (partial check-and-throw (s/coll-of ::spec/composition)) :raw))
  (constantly {:raw initial-data
               :ui {:player {:queue []
                             :track-list []
                             :paused true}
                    :nation {:mouse-on nil
                             :selected nil}
                    :composer nil}}))

(def gen-int-id (let [ids (atom 0)] #(swap! ids inc)))

(defn composition->track
  [composition]
  {:track/type :track/no-player
   :track/artist (composition/composer-name composition)
   :track/title (composition/name composition)
   :track/id (gen-int-id)})

(defn set-track-list
  [{compositions :raw :as all-data} composer]
  (->> compositions
       (filter (comp (partial util/string= composer) composition/composer-name))
       (map composition->track)
       (assoc-in all-data [:ui :player :track-list])))

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
  (after (comp (partial check-and-throw ::spec/track-list) :track-list :player :ui))
  (fn [all-data [_ composer]]
    (-> all-data
        (set-track-list composer)
        (assoc-in [:ui :composer] composer))))

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
