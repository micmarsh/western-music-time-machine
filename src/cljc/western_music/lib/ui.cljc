(ns western-music.lib.ui
  (:require [re-frame.core :refer [after debug dispatch]]
            [western-music.lib.composition :as composition]
            [western-music.spec :as spec]
            [western-music.lib.track]
            [western-music.util :as util]
            [clojure.spec :as s]
            [western-music.lib.ui.monad :as m]
            [#?(:cljs cljs.spec.impl.gen
                :clj clojure.spec.gen) :as gen]))

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
  (s/and (s/keys :req [:player/queue
                       :player/track-list
                       :player/paused
                       :player/playing
                       :player/shuffle-memory
                       :player/selected-tab])
         (fn [{playing :player/playing q :player/queue}]
           (if playing
             (contains? (into #{} (map :track/id) q) (:track/id playing))
             true))))

(s/def :player/paused boolean?)

(s/def :player/shuffle-memory (s/nilable ::spec/track-list))

(s/def :player/queue ::spec/track-list)

(s/def :player/track-list ::spec/track-list)

(s/def :player/playing (s/nilable ::spec/track))

(s/def :player/selected-tab #{:selection :queue})

(def all-data-spec (s/keys :req [:data/raw :data/ui]))

(def verify-all-data
 (after (partial spec/verify all-data-spec)))

(def ^:const blank
  #:ui{:player #:player{:queue []
                        :track-list []
                        :paused true
                        :playing nil
                        :shuffle-memory nil
                        :selected-tab :selection}       
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
   {:db (merge player #:player{:playing track :paused paused})
    :dispatch [:new-track-playing track paused]}))

(defn player-play-track
  [player track]
  (-> player
      (player-enqueue-track track)
      (m/return)
      (m/bind player-set-playing track)))

(defn player-track-lookup [player track-id]
  (->> [:player/queue :player/track-list]
       (sequence (comp (mapcat player)
                       (filter (comp #{track-id} :track/id))))
       (first)))

(def ^:const player-path [:data/ui :ui/player])

(defn remove-track [coll track-id]
  (into [] (remove (comp #{track-id} :track/id)) coll))

(defn player-play [player] reductions
  (let [q (:player/queue player)]
    (if (zero? (count q))
      (m/return player)
      (cond-> (m/return player)
        (nil? (:player/playing player)) (m/bind player-set-playing (first q))
        (some? (:player/playing player)) (m/bind (fn [p] {:db p :dispatch [:current-track-playing]}))
        true (m/fmap assoc :player/paused false)))))

(defn player-pause [player]  
  {:db (assoc player :player/paused true)
   :dispatch [:current-track-paused]})

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
  (cond-> (m/return p)
    true (m/fmap assoc :player/selected-tab :queue)
    (not (player-at-beginning? p)) (m/bind player-set-playing (q (dec (player-index p))) paused?)))

(defn player-forward
  [{q :player/queue paused? :player/paused :as p}]
  (cond-> (m/return p)
    true (m/fmap assoc :player/selected-tab :queue)
    (not (player-at-end? p)) (m/bind player-set-playing (q (inc (player-index p))) paused?)))

(defn currently-playing? [player track-id]
  (-> player :player/playing :track/id (= track-id)))

(defn player-clear-queue [player]
  {:dispatch [:all-tracks-cleared]
   :db (-> player
           (update :player/queue empty)
           (merge #:player{:playing nil :paused true}))})

(defn player-dequeue-track [{queue :player/queue :as player} track-id]
  (let [q (remove-track queue track-id)
        empty (zero? (count q))]
    (cond-> (m/return player)
      (and (currently-playing? player track-id) (player-at-end? player)) (m/bind player-back)
      (and (currently-playing? player track-id) (not (player-at-end? player))) (m/bind player-forward)
      empty (m/bind player-clear-queue)
      (not empty) (m/fmap assoc :player/queue q))))

(defn player-track-ended
  [{ended :player/playing :as p}]
  (cond-> (m/return p)
    (player-at-end? p) (m/bind player-pause)
    (not (player-at-end? p)) (m/bind player-forward)
    true (m/fmap update :player/queue remove-track (:track/id ended))))

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

(defn set-tab [ui tab]
  (assoc-in ui [:ui/player :player/selected-tab] tab))

(defn focus-nation [ui nation]
  (assoc-in ui [:ui/nation :ui.nation/mouse-on] nation))

(defn select-nation [ui nation]
  (assoc-in ui [:ui/nation :ui.nation/selected] nation))

(def selected-tab (comp :player/selected-tab :ui/player))

(def composer :ui/composer)

(def player :ui/player)

(def track-list (comp :player/track-list player))

(def player-queue :player/queue)

(def player-playing :player/playing)

(def player-paused? :player/paused)
