(ns western-music.lib.ui
  (:require #?(:cljs [re-frame.core :refer [after debug dispatch]])
            [western-music.lib.composition :as composition]
            [western-music.spec :as spec]
            [western-music.lib.track]
            [western-music.util :as util]
            [clojure.spec :as s]))

#?(:clj
   (do
     (def fake-dispatch-results (atom []))
     
     (defn dispatch [arg]
       (swap! fake-dispatch-results conj arg))
     ))

(s/def :data/raw (s/coll-of ::spec/composition))

(s/def :data/ui 
  (s/keys :req [:ui/player
                :ui/nation
                :ui/composer]))

(s/def :ui/nation
  (s/keys :req [:ui.nation/mouse-on
                :ui.nation/selected]))

(s/def :ui.nation/selected (s/nilable :place/nation))

(s/def :ui.nation/mouse-on (s/nilable :place/nation))

(s/def :ui/composer (s/nilable :composer/name))

(s/def :ui/player
  (s/keys :req [:player/queue
                :player/track-list
                :player/paused
                :player/shuffle-memory]))

(s/def :player/paused boolean?)

(s/def :player/shuffle-memory (s/nilable ::spec/track-list))

(s/def :player/queue ::spec/track-list)

(s/def :player/track-list ::spec/track-list)

(s/def :player/playing (s/nilable ::spec/track))

(def all-data-spec (s/keys :req [:data/raw :data/ui]))

#?(:cljs
   (def verify-all-data
     (after (partial spec/verify all-data-spec)))
   )

(def ^:const blank
  #:ui{:player #:player{:queue []
                        :track-list []
                        :paused true
                        :playing nil
                        :shuffle-memory nil}
       :nation #:ui.nation{:mouse-on nil
                           :selected nil}
       :composer nil})

(def ^:const initial-data
  #:data{:raw [] :ui blank})

(defn new-composition
  [raw-data composition]
  (conj (or raw-data []) composition))

(defn tracks-by-composer
  [composer-id compositions]
  (->> compositions
       (filter (comp (partial util/string= composer-id) composition/composer-name))
       (map composition/track)))

(defn tracks-by-nation
  [nation-id compositions]
  (->> compositions
       (filter (comp (partial util/string= nation-id) composition/nation-id))
       (map composition/track)))

(defn set-track-list-by-composer
  [{compositions :data/raw :as all-data} composer-id]
  (assoc-in all-data
            [:data/ui :ui/player :player/track-list]
            (tracks-by-composer composer-id compositions)))

(def ^:const nation-focus-path
  [:data/ui :ui/nation :ui.nation/mouse-on])

(def ^:const nation-selected-path
  [:data/ui :ui/nation :ui.nation/selected])

(defn reset-selection [ui]
  (merge ui (select-keys (:data/ui blank) [:ui/composer :ui/nation])))

(defn set-composer [all-data composer]
  (assoc-in all-data [:data/ui :ui/composer] composer))

(defn get-composer [all-data]
  (get-in all-data [:data/ui :ui/composer]))

(defn select-composer
  [all-data composer-id]
  (let [current (get-composer all-data)
        composer-id (when-not (util/string= composer-id current) composer-id)]
    (-> all-data
        (set-track-list-by-composer composer-id)
        (set-composer composer-id))))

(defn enqueue-track 
  "Enqueues a track that hasn't already been added to the given collection"
  [queue track]
  (if (contains? (into #{} (map :track/id) queue) (:track/id track))
    queue
    (conj queue track)))

(defn player-enqueue-track [player track]
  (update player :player/queue enqueue-track track))

(defn player-set-playing 
  ([player track] (player-set-playing player track false))
  ([player track paused]
   (dispatch [:new-track-playing track paused])
   (merge player #:player{:playing track :paused paused})))

(defn player-play-track
  [player track]
  (-> player
      (player-enqueue-track track)
      (player-set-playing track)))

(defn player-track-lookup [player track-id]
  (->> [:player/queue :player/track-list]
       (sequence (comp (mapcat player)
                       (filter (comp #{track-id} :track/id))))
       (first)))

(def ^:const player-path [:data/ui :ui/player])

(defn remove-track [coll track-id]
  (into [] (remove (comp #{track-id} :track/id)) coll)) ;(:player/queue player)

(defn player-play [player] 
  (let [q (:player/queue player)]
    (dispatch [:current-track-playing])
    (if (zero? (count q))
      player
      (cond-> player
        (nil? (:player/playing player)) (player-set-playing (first q))
        true (assoc :player/paused false)))))

(defn player-pause [player]
  (dispatch [:current-track-paused])
  (assoc player :player/paused true))

(defn track-index [coll track]
  (.indexOf (mapv :track/id coll) (:track/id track)))

(defn player-index [{q :player/queue track :player/playing}]
  (track-index q track))

(defn player-at-end? [{q :player/queue :as player}]
  (let [where (player-index player)
        max-index (dec (count q))]
    (= where max-index)))

(def player-at-beginning? (comp zero? player-index))

(defn player-back [{q :player/queue paused? :player/paused :as p}]
  (cond-> p
    (not (player-at-beginning? p)) (player-set-playing (q (dec (player-index p))) paused?)))

(def player-shuffling? (comp not nil? :player/shuffle-memory))

(defn player-shuffle-forward
  [{q :player/queue
    paused? :player/paused
    current :player/playing
    shuffle-mem :player/shuffle-memory
    :as p}]
  (let [mem-fn (comp boolean (into #{} (map :track/id) shuffle-mem) :track/id)
        next-track (util/rand-mem mem-fn q (* 5 (count shuffle-mem)))
        done? (= ::util/generator-exhausted next-track)]
    (cond-> p    
      done? (player-set-playing (first shuffle-mem) paused?)
      done? (assoc :player/shuffle-memory [])    
      (not done?) (player-set-playing next-track paused?)
      (not done?) (update :player/shuffle-memory enqueue-track next-track))))

(defn player-forward*
  [{q :player/queue paused? :player/paused :as p}]
  (cond-> p
    (not (player-at-end? p)) (player-set-playing (q (inc (player-index p))) paused?)))

(defn player-forward [player]
  (if (player-shuffling? player)
    (player-shuffle-forward player)
    (player-forward* player)))

(defn currently-playing? [player track-id]
  (-> player :player/playing :track/id (= track-id)))

(defn player-clear-queue [player]
  (dispatch [:all-tracks-cleared])
  (-> player
      (update :player/queue empty)
      (merge #:player{:playing nil :paused true})))

(defn player-dequeue-track [{queue :player/queue :as player} track-id]
  (let [q (remove-track queue track-id)
        empty (zero? (count q))]
    (cond-> player
      (and (currently-playing? player track-id) (player-at-end? player)) (player-back)
      (and (currently-playing? player track-id) (not (player-at-end? player))) (player-forward)
      empty (player-clear-queue)
      true (assoc :player/queue q))))

(defn player-track-ended
  [{ended :player/playing :as p}]
  (cond-> p
    (player-at-end? p) (player-pause)
    (not (player-at-end? p)) (player-forward)
    true (update :player/queue remove-track (:track/id ended))))

(defn player-enqueue-all
  [player tracks]
  (reduce player-enqueue-track player tracks))

(defn enqueue-composer
  [all-data composer-id]
  (let [tracks (tracks-by-composer composer-id (:data/raw all-data))]
    (update-in all-data player-path player-enqueue-all tracks)))

(defn enqueue-nation
  [all-data nation-id]
  (let [tracks (tracks-by-nation nation-id (:data/raw all-data))]
    (update-in all-data player-path player-enqueue-all tracks)))

(defn selected-nation [ui]
  (or (:ui.nation/selected (:ui/nation ui))
      (:ui.nation/mouse-on (:ui/nation ui))))

(def composer :ui/composer)

(def player :ui/player)

(def track-list (comp :player/track-list player))

(def player-queue :player/queue)

(def player-playing :player/playing)

(def player-paused? :player/paused)

(defn player-start-shuffling [player]
  (cond-> player
    (not (player-shuffling? player)) (assoc :player/shuffle-memory [])))

(defn player-stop-shuffling [player]
  (assoc player :player/shuffle-memory nil))