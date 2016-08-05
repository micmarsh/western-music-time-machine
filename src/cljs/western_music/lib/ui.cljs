(ns western-music.lib.ui
  (:require [re-frame.core :refer [after debug dispatch]]
            [western-music.lib.composition :as composition]
            [western-music.lib.track]
            [western-music.spec :as spec]
            [western-music.util :as util]
            [clojure.spec :as s]))

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
                :player/paused]))

(s/def :player/paused boolean?)

(s/def :player/queue ::spec/track-list)

(s/def :player/track-list ::spec/track-list)

(s/def :player/playing (s/nilable ::spec/track))

(defn check-and-throw
  "throw an exception if db doesn't match the spec."
  [spec data]
  (when-not (s/valid? spec data)
    (throw (ex-info (str "spec check failed: " (s/explain-str spec data))
                    (s/explain-data spec data)))))

(def verify-all-data
  (after (partial check-and-throw (s/keys :req [:data/raw :data/ui]))))

(def ^:const blank
  #:ui{:player #:player{:queue []
                        :track-list []
                        :paused true
                        :playing nil}
       :nation #:ui.nation{:mouse-on nil
                           :selected nil}
       :composer nil})

(defn ->initialize [initial-data]
  (fn [& _]
    (doseq [nation (map composition/nation-id initial-data)]
      (dispatch [:nation-ready nation]))
    #:data{:raw initial-data
           :ui blank}))

(defn track-list-by-composer
  [{compositions :data/raw :as all-data} composer]
  (->> compositions
       (filter (comp (partial util/string= composer) composition/composer-name))
       (map composition/track)
       (assoc-in all-data [:data/ui :ui/player :player/track-list])))

(def ^:const nation-focus-path
  [:data/ui :ui/nation :ui.nation/mouse-on])

(def ^:const nation-selected-path
  [:data/ui :ui/nation :ui.nation/selected])

(defn reset-selection [ui]
  (merge ui (select-keys (:data/ui blank) [:ui/composer :ui/nation])))

(defn set-composer [all-data composer]
  (assoc-in all-data [:data/ui :ui/composer] composer))

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

(defn player-forward [{q :player/queue paused? :player/paused :as p}]
  (cond-> p
    (not (player-at-end? p)) (player-set-playing (q (inc (player-index p))) paused?)))

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

(defn selected-nation [ui]
  (or (:ui.nation/selected (:ui/nation ui))
      (:ui.nation/mouse-on (:ui/nation ui))))

(def composer :ui/composer)

(def player :ui/player)

(def track-list (comp :player/track-list player))

(def player-queue :player/queue)

(def player-playing :player/playing)

(def player-paused? :player/paused)
