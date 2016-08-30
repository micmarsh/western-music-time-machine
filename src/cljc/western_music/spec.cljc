(ns western-music.spec
  (:require [clojure.spec :as s]))

(s/def ::composition
  (s/keys :req [:composition/id 
                :composition/name
                :composition/tracks 
                ::composer]))

(s/def :composition/id int?)

(s/def :composition/name string?)

(s/def ::composer
  (s/keys :req [:composer/name :composer/birth]))

(s/def :composer/name string?)

(s/def ::place-time
  #?(:cljs (s/merge-spec-impl [::place ::time]
                              [::place ::time]
                              nil)
     :clj (s/merge ::place ::time)))

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

(defmulti track-spec :track/type)

(defmethod track-spec :track/no-player
  [_]
  (s/keys :req [:track/type
                :track/artist
                :track/title
                :track/id]))

(s/def :track/type keyword?)

(s/def :track/artist string?)

(s/def :track/title string?)

(s/def :track/id int?)

(s/def ::track (s/multi-spec track-spec :track/type))

(defn unique-track-ids? [tracks]
  (= (count tracks)
     (count (into #{} (map :track/id) tracks))))

(s/def ::track-list
  (s/and (s/coll-of ::track)
         unique-track-ids?))

(s/def :composition/tracks ::track-list)

(defn verify [spec item]
  (if (s/valid? spec item)
    item
    (throw (ex-info (str "spec check failed: " (s/explain-str spec item))
                    (s/explain-data spec item)))))

(comment
  (s/explain ::composition 
             {:composition/name "Teh 5th" 
              ::composer {:composer/name "Ludwig Van" 
                          :composer/birth  {:place/type :place/nation
                                            :place/nation "Austria"
                                            :time/type :time/year
                                            :time/year 1888}}})

  {:name "Blumine", :composer {:name "Gustav Mahler", :birth {:nation "Czech Republic", :city "Kaliště (Pelhřimov District)", :year 1860}}}

  (require '[clojure.set :refer [rename-keys]]
           '[western-music.util :refer [conditional-prewalk]]
           'western-music.data)

  (defn convert [old-data]
    (-> old-data 
        (rename-keys {:name :composition/name
                      :composer ::composer})
        (update ::composer rename-keys {:name :composer/name})
        (update-in [::composer :birth] (fn [place-time]
                                        {:place/type :place/city 
                                         :place/nation (:nation place-time)
                                         :place/city (:city place-time)
                                         :time/type :time/year
                                         :time/year (:year place-time)}))
        (update ::composer rename-keys {:birth :composer/birth})))

  (def all (map convert western-music.data/initial-data))

)
