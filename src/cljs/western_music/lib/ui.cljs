(ns western-music.lib.ui
  (:require [re-frame.core :refer [after debug]]
            [western-music.lib.composition :as composition]
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
  (constantly 
   #:data{:raw initial-data
          :ui blank}))

(def composition->track
  "TODO major changes / moving around of this once \"re-ingest\"
   model is implemented"
  (let [ids (atom 0)]
    (fn [composition]
      #:track{:type :track/no-player
              :artist (composition/composer-name composition)
              :title (composition/name composition)
              :id (swap! ids inc)})))

(defn track-list-by-composer
  [{compositions :data/raw :as all-data} composer]
  (->> compositions
       (filter (comp (partial util/string= composer) composition/composer-name))
       (map composition->track)
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
  "TODO this function is likely to be important for actually loading/playing
  music files"
  ([player track] (player-set-playing player track false))
  ([player track paused]
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
    (if (zero? (count q))
      player
      (cond-> player
        (nil? (:player/playing player)) (player-set-playing (first q))
        true (assoc :player/paused false)))))

(defn player-pause [player] 
  (assoc player :player/paused true))

(defn track-index [coll track]
  (.indexOf (mapv :track/id coll) (:track/id track)))

(defn player-back [player] 
  (let [q (:player/queue player)
        where (track-index q (:player/playing player))]
    (cond-> player
      (pos? where) (player-set-playing (q (dec where)) (:player/paused player)))))

(defn player-forward [player]
    (let [q (:player/queue player)
          where (track-index q (:player/playing player))
          max-index (dec (count q))]
    (cond-> player
      (not= where max-index) (player-set-playing (q (inc where)) (:player/paused player)))))

(defn player-dequeue-track [player track-id]
  (let [q (remove-track (:player/queue player) track-id)
        empty (zero? (count q))]
    (cond-> player
      (-> player :player/playing :track/id (= track-id)) (player-forward)
      empty (assoc :player/playing nil)
      true (merge #:player{:queue q :paused empty}))))

(defn player-clear-queue [player]
  (reduce player-dequeue-track player
          (map :track/id (:player/queue player))))

(defn selected-nation [ui]
  (or (:ui.nation/selected (:ui/nation ui))
      (:ui.nation/mouse-on (:ui/nation ui))))

(def composer :ui/composer)

(def player :ui/player)

(def track-list (comp :player/track-list player))

(def player-queue :player/queue)

(def player-playing :player/playing)

(def player-paused? :player/paused)
