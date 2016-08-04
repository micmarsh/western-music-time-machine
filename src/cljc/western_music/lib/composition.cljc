(ns western-music.lib.composition
  (:refer-clojure :exclude [name])
  (:require [western-music.spec :as spec]
            [clojure.spec :as s]))

(defn nation-id [composition]
  (->> composition
       (::spec/composer)
       (:composer/birth)
       (:place/nation)))

(def name :composition/name)

(defn composer-name [composition]
  (->> composition
       (::spec/composer)
       (:composer/name)))

(defmulti track
  (fn [c] (into #{} (map :track/type) (:composition/tracks c))))

(defmethod track #{:track/no-player :track/youtube}
  [composition]
  (->> composition
       (:composition/tracks)
       (filter (comp #{:track/youtube} :track/type))
       (first)))

(defmethod track :default
  [composition]
  (-> composition
      (:composition/tracks)
      (first)))

(defn add-track [composition track]
  (update composition :composition/tracks (fnil conj []) track))
