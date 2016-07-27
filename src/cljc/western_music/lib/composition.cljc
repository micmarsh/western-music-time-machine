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
