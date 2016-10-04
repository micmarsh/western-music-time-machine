(ns western-music.lib.composition
  (:refer-clojure :exclude [name])
  (:require [western-music.spec :as spec]
            [western-music.util :as util]
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

(defn ->nation? [id]
  (comp (partial util/string= id) nation-id))

(defn max-id [compositions]
  (assert (sequential? compositions))
  (->> compositions
       (map :composition/id)
       (apply max)))

(defn max-track-id [compositions]
  (assert (sequential? compositions))
  (->> compositions
       (mapcat :composition/tracks)
       (map :track/id)
       (apply max)))

(defn minimal
  "Returns a minimal (non-spec passing) map of a composition"
  [artist composition-name]
  {:composition/name composition-name
   ::spec/composer {:composer/name artist}})
