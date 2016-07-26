(ns western-music.spec
  (:require [#?(:cljs cljs.spec :clj clojure.spec) :as s]))

(s/def ::composition
  (s/keys :req [:composition/name ::composer]))

(s/def :composition/name string?)

(s/def ::composer
  (s/keys :req [:composer/name :composer/birth]))

(s/def :composer/name string?)

(s/def ::place-time
  (s/merge ::place ::time))

(s/def :composer/birth ::place-time)

(defmulti place-spec :place/type)
(s/def :place/type keyword?) ;; keep as a keyword for extensibility

(s/def :place/nation
  string?   ;; TODO want this to conform to a global list (maybe?) but specs are pure
  ;; data. Think on compile-time generation of such things)
  )

(s/def :place/city
  string? ;; TODO same business as above
  )

(defmethod place-spec :place/nation
  [_]
  (s/keys :req [:place/type
                :place/nation]))

(defmethod place-spec :place/city
  [_]
  (s/keys :req [:place/type
                :place/nation
                :place/city]))

(defmulti time-spec :time/type)
(s/def :time/type keyword?)

(s/def :time/year int?)

(defmethod time-spec :time/year
  [_]
  (s/keys :req [:time/type
                :time/year]))

(s/def ::place (s/multi-spec place-spec :place/type))
(s/def ::time (s/multi-spec time-spec :time/type))
