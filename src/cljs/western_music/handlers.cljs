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

(defn enqueue-track 
  "Enqueues a track that hasn't already been added to the given collection"
  [queue track]
  (if (contains? (into #{} (map :track/id) queue) (:track/id track))
    queue
    (conj queue track)))

(defn play-track
  [{:keys [queue] :as player} track]
  (-> player
      (update :queue enqueue-track track)
      (assoc :playing track :paused false)))

(defn track-lookup [player track-id]
  (->> [:queue :track-list]
       (sequence (comp (mapcat player)
                       (filter (comp #{track-id} :track/id))))
       (first)))

(def-event
  :play-track
  (path :ui :player)
  (fn [player [_ track-id]]
    (->> track-id
         (track-lookup player)
         (play-track player))))

(def-event
  :enqueue-track
  (path :ui :player)
  (fn [player [_ track-id]]
    (let [track (track-lookup player track-id)]
      (update player :queue enqueue-track track))))

(def-event
  :dequeue-track
  (path :ui :player :queue)
  (fn [q [_ track-id]]
    (into [] (remove (comp #{track-id} :track/id)) q)))

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
